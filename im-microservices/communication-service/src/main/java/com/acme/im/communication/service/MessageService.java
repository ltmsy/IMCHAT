package com.acme.im.communication.service;

import com.acme.im.communication.entity.Message;
import com.acme.im.communication.repository.MessageRepository;
import com.acme.im.common.websocket.dto.WebSocketDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        
        // 2. 生成消息序列号
        Long seq = sequenceService.getNextSequence(conversationId);
        
        // 3. 构建消息对象
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
        
        // 4. 保存消息到分表
        Message savedMessage = messageRepository.save(message);
        
        // 5. 记录幂等性信息
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
     * @param operatorId 操作者ID
     * @param newContent 新内容
     * @return 是否编辑成功
     */
    public boolean editMessage(Long conversationId, Long messageId, Long operatorId, String newContent) {
        
        // 检查消息是否存在
        Optional<Message> messageOpt = messageRepository.findById(conversationId, messageId);
        if (messageOpt.isEmpty()) {
            log.warn("编辑失败，消息不存在: conversationId={}, messageId={}", conversationId, messageId);
            return false;
        }
        
        Message message = messageOpt.get();
        
        // 检查编辑权限
        if (!message.getSenderId().equals(operatorId)) {
            log.warn("编辑失败，无权限: conversationId={}, messageId={}, senderId={}, operatorId={}", 
                    conversationId, messageId, message.getSenderId(), operatorId);
            return false;
        }
        
        // 检查是否可编辑（只有文本消息且未撤回）
        if (!message.isTextMessage() || message.isRecalled()) {
            log.warn("编辑失败，消息不可编辑: conversationId={}, messageId={}, msgType={}, recalled={}", 
                    conversationId, messageId, message.getMsgType(), message.isRecalled());
            return false;
        }
        
        // 执行编辑
        boolean success = messageRepository.editMessage(conversationId, messageId, newContent);
        
        if (success) {
            log.info("编辑消息成功: conversationId={}, messageId={}, operatorId={}", 
                    conversationId, messageId, operatorId);
        }
        
        return success;
    }

    /**
     * 置顶/取消置顶消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param pinned 是否置顶
     * @return 是否操作成功
     */
    public boolean pinMessage(Long conversationId, Long messageId, boolean pinned) {
        boolean success = messageRepository.pinMessage(conversationId, messageId, pinned);
        
        if (success) {
            log.info("{}消息成功: conversationId={}, messageId={}", 
                    pinned ? "置顶" : "取消置顶", conversationId, messageId);
        }
        
        return success;
    }

    /**
     * 删除消息（软删除）
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param operatorId 操作者ID
     * @return 是否删除成功
     */
    public boolean deleteMessage(Long conversationId, Long messageId, Long operatorId) {
        
        // 检查消息是否存在
        Optional<Message> messageOpt = messageRepository.findById(conversationId, messageId);
        if (messageOpt.isEmpty()) {
            log.warn("删除失败，消息不存在: conversationId={}, messageId={}", conversationId, messageId);
            return false;
        }
        
        Message message = messageOpt.get();
        
        // 检查删除权限（简化处理）
        if (!message.getSenderId().equals(operatorId)) {
            log.warn("删除失败，无权限: conversationId={}, messageId={}, senderId={}, operatorId={}", 
                    conversationId, messageId, message.getSenderId(), operatorId);
            return false;
        }
        
        // 执行删除
        boolean success = messageRepository.deleteMessage(conversationId, messageId);
        
        if (success) {
            log.info("删除消息成功: conversationId={}, messageId={}, operatorId={}", 
                    conversationId, messageId, operatorId);
        }
        
        return success;
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
} 