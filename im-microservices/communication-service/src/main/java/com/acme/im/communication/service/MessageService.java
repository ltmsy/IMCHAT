package com.acme.im.communication.service;

import com.acme.im.communication.entity.Message;
import com.acme.im.communication.repository.MessageRepository;
import com.acme.im.communication.event.MessageEditEvent;
import com.acme.im.communication.event.NewMessageEvent;
import com.acme.im.communication.event.MessageDeleteEvent;
import com.acme.im.communication.event.MessagePinEvent;

import com.acme.im.common.plugin.ExtensionPointManager;
import com.acme.im.communication.plugin.MessageRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * 消息处理服务
 * 整合消息的创建、存储、查询、更新等核心功能
 * 
 * 核心职责：
 * 1. 消息创建和存储（支持分表）
 * 2. 消息序列号管理
 * 3. 消息幂等性处理
 * 4. 消息查询和历史记录
 * 5. 消息状态更新（撤回、编辑、置顶等）
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageSequenceService sequenceService;
    private final MessageIdempotencyService idempotencyService;
    private final ExtensionPointManager extensionPointManager;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建并保存消息
     * 
     * @param conversationId 会话ID
     * @param senderId 发送者ID
     * @param msgType 消息类型
     * @param content 消息内容
     * @param clientMsgId 客户端消息ID
     * @param contentExtra 扩展内容
     * @return 创建的消息
     */
    @Transactional
    public Message createMessage(Long conversationId, Long senderId, Integer msgType, 
                               String content, String clientMsgId, String contentExtra) {
        
        // 1. 幂等性检查
        Long existingMsgId = idempotencyService.checkMessageExists(conversationId, clientMsgId);
        if (existingMsgId != null) {
            log.debug("消息已存在，返回现有消息: conversationId={}, clientMsgId={}, msgId={}", 
                     conversationId, clientMsgId, existingMsgId);
            return messageRepository.findById(conversationId, existingMsgId).orElse(null);
        }
        
        // 2. 消息处理扩展点 - 执行所有处理器
        Map<String, Object> processingContext = createProcessingContext(conversationId, senderId, msgType, content);
        try {
            extensionPointManager.executeExtensionPoint("message.process", 
                processingContext, context -> {
                    // 这里可以添加具体的消息处理逻辑
                    log.debug("消息处理扩展点执行: conversationId={}, senderId={}", conversationId, senderId);
                    return null;
                });
            log.debug("消息处理完成: conversationId={}, senderId={}", conversationId, senderId);
        } catch (Exception e) {
            log.warn("消息处理失败: conversationId={}, senderId={}, error={}", 
                    conversationId, senderId, e.getMessage());
            // 根据业务需求决定是否继续处理
        }
        
        // 3. 生成消息序列号
        Long seq = sequenceService.getNextSequence(conversationId);
        
        // 4. 构建消息对象
        Message message = Message.builder()
                .conversationId(conversationId)
                .seq(seq)
                .clientMsgId(clientMsgId)
                .senderId(senderId)
                .msgType(msgType)
                .content(content)
                .contentExtra(contentExtra)
                .status(1) // 正常状态
                .isPinned(0)
                .isEdited(0)
                .isRecalled(0)
                .editCount(0)
                .serverTimestamp(LocalDateTime.now())
                .build();
        
        // 5. 保存消息到分表
        Message savedMessage = messageRepository.save(message);
        
        // 6. 记录幂等性信息
        idempotencyService.recordMessageIdempotency(conversationId, clientMsgId, 
                                                   savedMessage.getId(), senderId);
        
        log.info("创建消息成功: messageId={}, conversationId={}, seq={}, senderId={}", 
                savedMessage.getId(), conversationId, seq, senderId);
        
        return savedMessage;
    }

    /**
     * 创建回复消息
     * 
     * @param conversationId 会话ID
     * @param senderId 发送者ID
     * @param content 消息内容
     * @param clientMsgId 客户端消息ID
     * @param replyToId 回复的消息ID
     * @return 创建的消息
     */
    public Message createReplyMessage(Long conversationId, Long senderId, String content, 
                                    String clientMsgId, Long replyToId) {
        
        // 验证被回复的消息是否存在
        Optional<Message> replyToMessage = messageRepository.findById(conversationId, replyToId);
        if (replyToMessage.isEmpty()) {
            throw new IllegalArgumentException("被回复的消息不存在: " + replyToId);
        }
        
        // 构建回复消息
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .msgType(Message.MessageType.TEXT.getCode())
                .content(content)
                .clientMsgId(clientMsgId)
                .replyToId(replyToId)
                .build();
        
        return createMessageInternal(message);
    }

    /**
     * 创建转发消息
     * 
     * @param conversationId 目标会话ID
     * @param senderId 转发者ID
     * @param clientMsgId 客户端消息ID
     * @param forwardFromId 原消息ID
     * @return 创建的消息
     */
    public Message createForwardMessage(Long conversationId, Long senderId, String clientMsgId, 
                                      Long forwardFromId) {
        
        // 获取原消息（可能在不同的分表中，需要提供原会话ID）
        // 这里简化处理，实际应用中需要传入原会话ID
        // Message originalMessage = messageRepository.findById(originalConversationId, forwardFromId)
        //     .orElseThrow(() -> new IllegalArgumentException("原消息不存在: " + forwardFromId));
        
        // 构建转发消息
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .msgType(Message.MessageType.TEXT.getCode()) // 简化处理
                .content("转发消息") // 实际应该复制原消息内容
                .clientMsgId(clientMsgId)
                .forwardFromId(forwardFromId)
                .build();
        
        return createMessageInternal(message);
    }

    /**
     * 查询会话的最新消息
     * 
     * @param conversationId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> getLatestMessages(Long conversationId, int limit) {
        return messageRepository.findLatestByConversationId(conversationId, limit);
    }

    /**
     * 分页查询消息历史
     * 
     * @param conversationId 会话ID
     * @param beforeSeq 在此序号之前的消息
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> getMessageHistory(Long conversationId, Long beforeSeq, int limit) {
        return messageRepository.findHistoryByConversationId(conversationId, beforeSeq, limit);
    }

    /**
     * 根据消息ID查找消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @return 消息对象
     */
    public Optional<Message> getMessage(Long conversationId, Long messageId) {
        return messageRepository.findById(conversationId, messageId);
    }

    /**
     * 撤回消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param operatorId 操作者ID
     * @param reason 撤回原因
     * @return 是否撤回成功
     */
    public boolean recallMessage(Long conversationId, Long messageId, Long operatorId, String reason) {
        
        // 检查消息是否存在
        Optional<Message> messageOpt = messageRepository.findById(conversationId, messageId);
        if (messageOpt.isEmpty()) {
            log.warn("撤回失败，消息不存在: conversationId={}, messageId={}", conversationId, messageId);
            return false;
        }
        
        Message message = messageOpt.get();
        
        // 检查撤回权限（简化处理，实际应该检查是否是发送者或管理员）
        if (!message.getSenderId().equals(operatorId)) {
            log.warn("撤回失败，无权限: conversationId={}, messageId={}, senderId={}, operatorId={}", 
                    conversationId, messageId, message.getSenderId(), operatorId);
            return false;
        }
        
        // 检查是否已撤回
        if (message.isRecalled()) {
            log.warn("撤回失败，消息已撤回: conversationId={}, messageId={}", conversationId, messageId);
            return false;
        }
        
        // 执行撤回
        boolean success = messageRepository.recallMessage(conversationId, messageId, reason);
        
        if (success) {
            log.info("撤回消息成功: conversationId={}, messageId={}, operatorId={}", 
                    conversationId, messageId, operatorId);
        }
        
        return success;
    }

    /**
     * 编辑消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param newContent 新内容
     * @param editorId 编辑者ID
     * @param editReason 编辑原因
     * @return 编辑后的消息
     */
    @Transactional
    public Message editMessage(Long conversationId, Long messageId, String newContent, 
                             Long editorId, String editReason) {
        
        // 1. 查找原消息
        Message originalMessage = messageRepository.findById(conversationId, messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));
        
        // 2. 检查消息是否可编辑
        if (!originalMessage.isEditable()) {
            throw new IllegalStateException("消息不可编辑");
        }
        
        // 3. 检查编辑权限
        if (!originalMessage.getSenderId().equals(editorId)) {
            throw new SecurityException("无权限编辑此消息");
        }
        
        // 4. 创建编辑消息
        Message editMessage = Message.builder()
                .conversationId(conversationId)
                .seq(sequenceService.getNextSequence(conversationId))
                .clientMsgId("edit_" + System.currentTimeMillis() + "_" + editorId)
                .senderId(editorId)
                .msgType(Message.MessageType.EDIT.getCode())
                .content(newContent)
                .originalMessageId(messageId)
                .operationType(Message.OperationType.EDIT.getCode())
                .originalContent(originalMessage.getContent())
                .editReason(editReason)
                .status(1)
                .serverTimestamp(LocalDateTime.now())
                .build();
        
        // 5. 保存编辑消息
        Message savedEditMessage = messageRepository.save(editMessage);
        
        // 6. 更新原消息状态
        originalMessage.setIsEdited(1);
        originalMessage.setEditCount(originalMessage.getEditCount() + 1);
        originalMessage.setLastEditAt(LocalDateTime.now());
        messageRepository.update(originalMessage);
        
        // 7. 推送编辑通知
        // messageRoutingService.pushMessageEdit(originalMessage, savedEditMessage); // Removed direct dependency
        eventPublisher.publishEvent(new MessageEditEvent(originalMessage, savedEditMessage));
        
        log.info("消息编辑完成: conversationId={}, messageId={}, editorId={}", 
                conversationId, messageId, editorId);
        
        return savedEditMessage;
    }
    
    /**
     * 引用消息
     * 
     * @param conversationId 会话ID
     * @param senderId 发送者ID
     * @param content 引用内容
     * @param quotedMessageId 被引用的消息ID
     * @param quotedConversationId 被引用消息的会话ID
     * @return 引用消息
     */
    @Transactional
    public Message quoteMessage(Long conversationId, Long senderId, String content, 
                              Long quotedMessageId, Long quotedConversationId) {
        
        // 1. 查找被引用的消息
        Message quotedMessage = messageRepository.findById(quotedConversationId, quotedMessageId)
                .orElseThrow(() -> new IllegalArgumentException("被引用的消息不存在"));
        
        // 2. 创建引用消息
        Message quoteMessage = Message.builder()
                .conversationId(conversationId)
                .seq(sequenceService.getNextSequence(conversationId))
                .clientMsgId("quote_" + System.currentTimeMillis() + "_" + senderId)
                .senderId(senderId)
                .msgType(Message.MessageType.QUOTE.getCode())
                .content(content)
                .originalMessageId(quotedMessageId)
                .operationType(Message.OperationType.QUOTE.getCode())
                .quotedMessageId(quotedMessageId)
                .quotedContent(quotedMessage.getContent())
                .quotedSenderId(quotedMessage.getSenderId())
                .quotedContentType(quotedMessage.getMsgType())
                .status(1)
                .serverTimestamp(LocalDateTime.now())
                .build();
        
        // 3. 保存引用消息
        Message savedQuoteMessage = messageRepository.save(quoteMessage);
        
        // 4. 推送引用消息通知
        // messageRoutingService.pushNewMessage(savedQuoteMessage); // Removed direct dependency
        eventPublisher.publishEvent(new NewMessageEvent(savedQuoteMessage));
        
        log.info("引用消息创建完成: conversationId={}, quotedMessageId={}, senderId={}", 
                conversationId, quotedMessageId, senderId);
        
        return savedQuoteMessage;
    }
    
    /**
     * 转发消息
     * 
     * @param targetConversationId 目标会话ID
     * @param senderId 发送者ID
     * @param originalMessageId 原消息ID
     * @param originalConversationId 原会话ID
     * @param forwardReason 转发原因
     * @return 转发消息
     */
    @Transactional
    public Message forwardMessage(Long targetConversationId, Long senderId, 
                                Long originalMessageId, Long originalConversationId, 
                                String forwardReason) {
        
        // 1. 查找原消息
        Message originalMessage = messageRepository.findById(originalConversationId, originalMessageId)
                .orElseThrow(() -> new IllegalArgumentException("原消息不存在"));
        
        // 2. 创建转发消息
        Message forwardMessage = Message.builder()
                .conversationId(targetConversationId)
                .seq(sequenceService.getNextSequence(targetConversationId))
                .clientMsgId("forward_" + System.currentTimeMillis() + "_" + senderId)
                .senderId(senderId)
                .msgType(Message.MessageType.FORWARD.getCode())
                .content(originalMessage.getContent())
                .contentExtra(originalMessage.getContentExtra())
                .originalMessageId(originalMessageId)
                .operationType(Message.OperationType.FORWARD.getCode())
                .originalConversationId(originalConversationId)
                .originalSenderId(originalMessage.getSenderId())
                .forwardReason(forwardReason)
                .status(1)
                .serverTimestamp(LocalDateTime.now())
                .build();
        
        // 3. 保存转发消息
        Message savedForwardMessage = messageRepository.save(forwardMessage);
        
        // 4. 推送转发消息通知
        // messageRoutingService.pushNewMessage(savedForwardMessage); // Removed direct dependency
        eventPublisher.publishEvent(new NewMessageEvent(savedForwardMessage));
        
        log.info("消息转发完成: targetConversationId={}, originalMessageId={}, senderId={}", 
                targetConversationId, originalMessageId, senderId);
        
        return savedForwardMessage;
    }
    
    /**
     * 删除消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param operatorId 操作者ID
     * @param deleteScope 删除范围：0-仅我，1-所有人
     * @param deleteReason 删除原因
     * @return 删除后的消息
     */
    @Transactional
    public Message deleteMessage(Long conversationId, Long messageId, Long operatorId, 
                               Integer deleteScope, String deleteReason) {
        
        // 1. 查找消息
        Message message = messageRepository.findById(conversationId, messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));
        
        // 2. 检查删除权限
        if (deleteScope == 1) { // 全局删除
            // 只有消息发送者或管理员可以全局删除
            if (!message.getSenderId().equals(operatorId)) {
                throw new SecurityException("无权限全局删除此消息");
            }
        }
        
        // 3. 更新消息状态
        message.setIsDeleted(1);
        message.setDeleteScope(deleteScope);
        message.setDeletedBy(operatorId);
        message.setDeletedAt(LocalDateTime.now());
        message.setDeleteReason(deleteReason);
        
        // 4. 保存更新
        messageRepository.update(message);
        
        // 5. 推送删除通知
        // messageRoutingService.pushMessageDelete(message, deleteReason, deleteScope); // Removed direct dependency
        eventPublisher.publishEvent(new MessageDeleteEvent(message, deleteReason, deleteScope));
        
        log.info("消息删除完成: conversationId={}, messageId={}, operatorId={}, scope={}", 
                message.getConversationId(), message.getId(), operatorId, deleteScope);
        
        return message;
    }
    
    /**
     * 置顶消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param operatorId 操作者ID
     * @param pinScope 置顶范围：0-仅我，1-所有人
     * @return 置顶后的消息
     */
    @Transactional
    public Message pinMessage(Long conversationId, Long messageId, Long operatorId, Integer pinScope) {
        
        // 1. 查找消息
        Message message = messageRepository.findById(conversationId, messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));
        
        // 2. 检查置顶权限
        if (pinScope == 1) { // 全局置顶
            // 只有消息发送者或群主可以全局置顶
            if (!message.getSenderId().equals(operatorId)) {
                throw new SecurityException("无权限全局置顶此消息");
            }
        }
        
        // 3. 更新消息状态
        message.setIsPinned(1);
        message.setPinScope(pinScope);
        message.setPinnedBy(operatorId);
        message.setPinnedAt(LocalDateTime.now());
        
        // 4. 保存更新
        messageRepository.update(message);
        
        // 5. 推送置顶通知
        // messageRoutingService.pushMessagePin(message, pinScope); // Removed direct dependency
        eventPublisher.publishEvent(new MessagePinEvent(message, pinScope));
        
        log.info("消息置顶完成: conversationId={}, messageId={}, operatorId={}, scope={}", 
                conversationId, messageId, operatorId, pinScope);
        
        return message;
    }
    
    /**
     * 取消置顶消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param operatorId 操作者ID
     * @return 取消置顶后的消息
     */
    @Transactional
    public Message unpinMessage(Long conversationId, Long messageId, Long operatorId) {
        
        // 1. 查找消息
        Message message = messageRepository.findById(conversationId, messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));
        
        // 2. 检查取消置顶权限
        if (!message.getPinnedBy().equals(operatorId)) {
            throw new SecurityException("无权限取消置顶此消息");
        }
        
        // 3. 更新消息状态
        message.setIsPinned(0);
        message.setPinScope(null);
        message.setPinnedBy(null);
        message.setPinnedAt(null);
        
        // 4. 保存更新
        messageRepository.update(message);
        
        log.info("消息取消置顶完成: conversationId={}, messageId={}, operatorId={}", 
                conversationId, messageId, operatorId);
        
        return message;
    }

    /**
     * 获取会话中的置顶消息
     * 
     * @param conversationId 会话ID
     * @return 置顶消息列表
     */
    public List<Message> getPinnedMessages(Long conversationId) {
        return messageRepository.findPinnedMessages(conversationId);
    }

    /**
     * 统计会话消息数量
     * 
     * @param conversationId 会话ID
     * @return 消息数量
     */
    public long countMessages(Long conversationId) {
        return messageRepository.countByConversationId(conversationId);
    }

    // ================================
    // 私有方法
    // ================================

    /**
     * 内部消息创建方法
     */
    private Message createMessageInternal(Message message) {
        // 生成序列号
        Long seq = sequenceService.getNextSequence(message.getConversationId());
        message.setSeq(seq);
        
        // 设置默认值
        if (message.getStatus() == null) {
            message.setStatus(1);
        }
        if (message.getIsPinned() == null) {
            message.setIsPinned(0);
        }
        if (message.getIsEdited() == null) {
            message.setIsEdited(0);
        }
        if (message.getIsRecalled() == null) {
            message.setIsRecalled(0);
        }
        if (message.getEditCount() == null) {
            message.setEditCount(0);
        }
        if (message.getServerTimestamp() == null) {
            message.setServerTimestamp(LocalDateTime.now());
        }
        
        // 保存消息
        Message savedMessage = messageRepository.save(message);
        
        // 记录幂等性信息
        idempotencyService.recordMessageIdempotency(
            message.getConversationId(), 
            message.getClientMsgId(), 
            savedMessage.getId(), 
            message.getSenderId()
        );
        
        return savedMessage;
    }

    /**
     * 创建验证上下文
     * 
     * @param conversationId 会话ID
     * @param senderId 发送者ID
     * @param msgType 消息类型
     * @param content 消息内容
     * @return 验证上下文
     */
    private Map<String, Object> createValidationContext(Long conversationId, Long senderId, Integer msgType, String content) {
        Map<String, Object> context = new HashMap<>();
        context.put("conversationId", conversationId);
        context.put("senderId", senderId);
        context.put("messageType", msgType);
        context.put("content", content);
        context.put("timestamp", System.currentTimeMillis());
        context.put("source", "MessageService.createMessage");
        return context;
    }

    /**
     * 创建处理上下文
     * 
     * @param conversationId 会话ID
     * @param senderId 发送者ID
     * @param msgType 消息类型
     * @param content 消息内容
     * @return 处理上下文
     */
    private Map<String, Object> createProcessingContext(Long conversationId, Long senderId, Integer msgType, String content) {
        Map<String, Object> context = new HashMap<>();
        context.put("conversationId", conversationId);
        context.put("senderId", senderId);
        context.put("messageType", msgType);
        context.put("content", content);
        context.put("timestamp", System.currentTimeMillis());
        context.put("source", "MessageService.createMessage");
        return context;
    }
} 