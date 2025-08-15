# IM系统 WebSocket 客户端对接说明文档

## 目录
- [1. 概述](#1-概述)
- [2. WebSocket连接配置](#2-websocket连接配置)
- [3. 消息类型定义](#3-消息类型定义)
- [4. 聊天消息处理](#4-聊天消息处理)
- [5. 系统通知处理](#5-系统通知处理)
- [6. 后台推送处理](#6-后台推送处理)
- [7. NATS事件推送处理](#7-nats事件推送处理)
- [8. 消息发送接口](#8-消息发送接口)
- [9. 错误处理](#9-错误处理)
- [10. 最佳实践](#10-最佳实践)

## 1. 概述

IM系统采用WebSocket + STOMP协议进行实时通信，支持以下功能：
- 实时聊天消息推送
- 系统通知和公告
- 后台管理推送
- NATS事件实时同步
- 多端消息同步
- 消息状态管理

### 技术架构
- **协议**: WebSocket + STOMP
- **消息格式**: Protocol Buffers (protobuf)
- **事件系统**: NATS + WebSocket实时推送
- **认证方式**: Token认证
- **连接管理**: 自动重连、心跳检测

## 2. WebSocket连接配置

### 2.1 连接端点
```javascript
// 开发环境
const wsUrl = 'ws://localhost:8080/ws';

// 生产环境
const wsUrl = 'wss://your-domain.com/ws';
```

### 2.2 连接建立
```javascript
// 使用SockJS + STOMP
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 连接参数
const connectHeaders = {
    'Authorization': 'Bearer ' + token,
    'userId': userId,
    'deviceId': deviceId,
    'clientType': 'web'
};

// 建立连接
stompClient.connect(connectHeaders, 
    function(frame) {
        console.log('连接成功:', frame);
        subscribeToTopics();
    },
    function(error) {
        console.error('连接失败:', error);
    }
);
```

### 2.3 订阅主题
```javascript
function subscribeToTopics() {
    // 订阅个人消息队列
    stompClient.subscribe('/user/queue/private-messages', handlePrivateMessage);
    
    // 订阅群组消息主题
    stompClient.subscribe('/topic/conversation/{conversationId}', handleGroupMessage);
    
    // 订阅系统通知
    stompClient.subscribe('/topic/system/notifications', handleSystemNotification);
    
    // 订阅用户状态变更
    stompClient.subscribe('/user/queue/user-status', handleUserStatusChange);
    
    // 订阅消息状态变更
    stompClient.subscribe('/user/queue/message-status', handleMessageStatusChange);
}
```

## 3. 消息类型定义

### 3.1 基础消息结构
```protobuf
message WebSocketMessage {
    string messageId = 1;           // 消息唯一ID
    MessageType type = 2;           // 消息类型
    MessageStatus status = 3;       // 消息状态
    string senderId = 4;            // 发送者ID
    string receiverId = 5;          // 接收者ID
    string conversationId = 6;      // 会话ID
    string clientMessageId = 7;     // 客户端消息ID
    string version = 8;             // 协议版本
    string deviceId = 9;            // 设备ID
    string source = 10;             // 消息来源
    int64 timestamp = 11;           // 时间戳
    int64 sequence = 12;            // 序列号
    
    oneof payload {
        ChatMessage chat = 20;           // 聊天消息
        NotificationMessage notification = 21;  // 通知消息
        SystemMessage system = 22;       // 系统消息
        EventMessage event = 23;         // 事件消息
    }
}
```

### 3.2 消息类型枚举
```javascript
const MessageType = {
    // 聊天消息类型 (0-19)
    CHAT_TEXT: 0,           // 文本消息
    CHAT_IMAGE: 1,          // 图片消息
    CHAT_FILE: 2,           // 文件消息
    CHAT_VOICE: 3,          // 语音消息
    CHAT_VIDEO: 4,          // 视频消息
    CHAT_LOCATION: 5,       // 位置消息
    CHAT_CARD: 6,           // 名片消息
    
    // 聊天特殊类型 (10-19)
    CHAT_EDIT: 10,          // 编辑消息
    CHAT_QUOTE: 11,         // 引用消息
    CHAT_FORWARD: 12,       // 转发消息
    CHAT_RECALL: 13,        // 撤回消息
    
    // 通知消息类型 (20-29)
    NOTIFICATION_ANNOUNCEMENT: 20,  // 公告通知
    NOTIFICATION_ALERT: 21,         // 提醒通知
    NOTIFICATION_PROMOTION: 22,     // 推广通知
    NOTIFICATION_MAINTENANCE: 23,   // 维护通知
    
    // 系统消息类型 (30-39)
    SYSTEM_CONNECT: 30,     // 连接建立
    SYSTEM_DISCONNECT: 31,  // 连接断开
    SYSTEM_HEARTBEAT: 32,   // 心跳消息
    SYSTEM_AUTH: 33,        // 认证消息
    SYSTEM_STATUS: 34,      // 状态更新
    
    // 事件消息类型 (40-49)
    EVENT_USER_STATUS_CHANGE: 40,   // 用户状态变更
    EVENT_GROUP_UPDATE: 41,         // 群组更新
    EVENT_MESSAGE_STATUS_CHANGE: 42, // 消息状态变更
    EVENT_SYSTEM_CONFIG_CHANGE: 43  // 系统配置变更
};
```

### 3.3 消息状态枚举
```javascript
const MessageStatus = {
    MESSAGE_NORMAL: 0,      // 正常消息
    MESSAGE_SENDING: 1,     // 发送中
    MESSAGE_SENT: 2,        // 已发送
    MESSAGE_DELIVERED: 3,   // 已投递
    MESSAGE_READ: 4,        // 已读
    MESSAGE_FAILED: 5,      // 发送失败
    MESSAGE_RECALLED: 6,    // 已撤回
    MESSAGE_EDITED: 7,      // 已编辑
    MESSAGE_DELETED: 8      // 已删除
};
```

## 4. 聊天消息处理

### 4.1 接收聊天消息
```javascript
function handleChatMessage(message) {
    const { type, senderId, conversationId, chat, timestamp } = message;
    
    switch (type) {
        case MessageType.CHAT_TEXT:
            handleTextMessage(message);
            break;
        case MessageType.CHAT_IMAGE:
            handleImageMessage(message);
            break;
        case MessageType.CHAT_FILE:
            handleFileMessage(message);
            break;
        case MessageType.CHAT_EDIT:
            handleEditMessage(message);
            break;
        case MessageType.CHAT_QUOTE:
            handleQuoteMessage(message);
            break;
        case MessageType.CHAT_FORWARD:
            handleForwardMessage(message);
            break;
        case MessageType.CHAT_RECALL:
            handleRecallMessage(message);
            break;
        default:
            console.warn('未知消息类型:', type);
    }
}

function handleTextMessage(message) {
    const { senderId, conversationId, chat, timestamp } = message;
    const content = JSON.parse(chat.content);
    
    // 显示文本消息
    displayTextMessage({
        senderId,
        conversationId,
        content: content.content,
        timestamp,
        messageId: message.messageId
    });
}
```

### 4.2 消息内容格式
```javascript
// 文本消息
{
    "content": "Hello World",
    "contentExtra": null
}

// 图片消息
{
    "content": "图片描述",
    "contentExtra": {
        "url": "https://example.com/image.jpg",
        "width": 800,
        "height": 600,
        "size": 1024000,
        "format": "JPEG"
    }
}

// 文件消息
{
    "content": "文件名.pdf",
    "contentExtra": {
        "url": "https://example.com/file.pdf",
        "size": 2048000,
        "type": "application/pdf",
        "extension": "pdf"
    }
}

// 引用消息
{
    "content": "回复内容",
    "quotedMessageId": "12345",
    "quotedContent": "被引用的消息内容",
    "quotedSenderId": "67890"
}

// 转发消息
{
    "content": "转发的内容",
    "originalMessageId": "12345",
    "forwardReason": "转发原因"
}
```

## 5. 系统通知处理

### 5.1 系统通知类型
```javascript
function handleSystemNotification(message) {
    const { type, notification, timestamp } = message;
    
    switch (type) {
        case MessageType.NOTIFICATION_ANNOUNCEMENT:
            handleAnnouncement(message);
            break;
        case MessageType.NOTIFICATION_ALERT:
            handleAlert(message);
            break;
        case MessageType.NOTIFICATION_MAINTENANCE:
            handleMaintenance(message);
            break;
        default:
            console.warn('未知通知类型:', type);
    }
}

function handleAnnouncement(message) {
    const { notification, timestamp } = message;
    const content = JSON.parse(notification.content);
    
    // 显示公告通知
    showNotification({
        type: 'announcement',
        title: content.title,
        content: content.content,
        priority: content.priority,
        timestamp
    });
}
```

### 5.2 通知内容格式
```javascript
// 公告通知
{
    "title": "系统公告",
    "content": "系统将于今晚进行维护升级",
    "priority": "high",
    "category": "maintenance",
    "expiresAt": "2024-01-20T23:59:59Z"
}

// 提醒通知
{
    "title": "新消息提醒",
    "content": "您有3条未读消息",
    "priority": "medium",
    "action": "view_messages",
    "data": {
        "conversationId": "12345"
    }
}

// 维护通知
{
    "title": "系统维护",
    "content": "系统将于22:00-24:00进行维护",
    "priority": "high",
    "maintenanceType": "scheduled",
    "estimatedDuration": "2小时"
}
```

## 6. 后台推送处理

### 6.1 管理推送类型
```javascript
function handleAdminPush(message) {
    const { type, event, timestamp } = message;
    
    switch (type) {
        case MessageType.EVENT_SYSTEM_CONFIG_CHANGE:
            handleConfigChange(message);
            break;
        case MessageType.EVENT_GROUP_UPDATE:
            handleGroupUpdate(message);
            break;
        case MessageType.EVENT_USER_STATUS_CHANGE:
            handleUserStatusChange(message);
            break;
        default:
            console.warn('未知管理推送类型:', type);
    }
}

function handleConfigChange(message) {
    const { event, timestamp } = message;
    const content = JSON.parse(event.content);
    
    // 处理配置变更
    if (content.configType === 'chat_settings') {
        updateChatSettings(content.newConfig);
    } else if (content.configType === 'user_permissions') {
        updateUserPermissions(content.newConfig);
    }
}
```

### 6.2 管理推送内容格式
```javascript
// 系统配置变更
{
    "configType": "chat_settings",
    "oldConfig": {
        "maxMessageLength": 1000,
        "fileUploadLimit": 10
    },
    "newConfig": {
        "maxMessageLength": 2000,
        "fileUploadLimit": 20
    },
    "changedBy": "admin_user",
    "reason": "提升用户体验"
}

// 群组更新
{
    "groupId": "12345",
    "updateType": "member_added",
    "memberId": "67890",
    "memberRole": "member",
    "updatedBy": "admin_user",
    "timestamp": "2024-01-20T10:00:00Z"
}

// 用户权限变更
{
    "userId": "12345",
    "permissionType": "chat_permission",
    "oldPermission": "read_only",
    "newPermission": "full_access",
    "changedBy": "admin_user",
    "reason": "用户升级"
}
```

## 7. NATS事件推送处理

### 7.1 事件类型分类
```javascript
// 用户相关事件
const USER_EVENTS = [
    'business.user.login',
    'business.user.logout',
    'business.user.status.changed',
    'business.user.profile.updated'
];

// 会话相关事件
const CONVERSATION_EVENTS = [
    'business.conversation.created',
    'business.conversation.updated',
    'business.conversation.member.added',
    'business.conversation.member.removed'
];

// 群组相关事件
const GROUP_EVENTS = [
    'business.group.created',
    'business.group.updated',
    'business.group.member.joined',
    'business.group.member.left'
];

// 消息相关事件
const MESSAGE_EVENTS = [
    'communication.message.sent',
    'communication.message.recalled',
    'communication.message.edited',
    'communication.message.deleted'
];
```

### 7.2 事件处理函数
```javascript
function handleNatsEvent(message) {
    const { type, event, timestamp } = message;
    const eventData = JSON.parse(event.content);
    
    switch (eventData.eventType) {
        case 'business.user.login':
            handleUserLogin(eventData);
            break;
        case 'business.user.logout':
            handleUserLogout(eventData);
            break;
        case 'business.conversation.created':
            handleConversationCreated(eventData);
            break;
        case 'communication.message.recalled':
            handleMessageRecalled(eventData);
            break;
        default:
            console.warn('未知NATS事件类型:', eventData.eventType);
    }
}

function handleUserLogin(eventData) {
    const { userId, deviceId, loginTime, ipAddress } = eventData;
    
    // 更新用户在线状态
    updateUserOnlineStatus(userId, true);
    
    // 显示登录通知
    showUserStatusNotification({
        userId,
        status: 'online',
        message: '用户已上线',
        timestamp: loginTime
    });
}

function handleMessageRecalled(eventData) {
    const { messageId, conversationId, operatorId, reason } = eventData;
    
    // 更新消息显示状态
    updateMessageDisplay(messageId, {
        status: 'recalled',
        recalledBy: operatorId,
        recallReason: reason
    });
    
    // 显示撤回通知
    showMessageRecallNotification({
        messageId,
        reason,
        timestamp: Date.now()
    });
}
```

### 7.3 事件数据格式
```javascript
// 用户登录事件
{
    "eventType": "business.user.login",
    "userId": "12345",
    "deviceId": "device_001",
    "loginTime": "2024-01-20T10:00:00Z",
    "ipAddress": "192.168.1.100",
    "clientType": "web",
    "userAgent": "Mozilla/5.0..."
}

// 消息撤回事件
{
    "eventType": "communication.message.recalled",
    "messageId": "msg_001",
    "conversationId": "conv_001",
    "operatorId": "12345",
    "reason": "用户撤回",
    "timestamp": "2024-01-20T10:00:00Z"
}

// 群组创建事件
{
    "eventType": "business.group.created",
    "groupId": "group_001",
    "groupName": "技术交流群",
    "creatorId": "12345",
    "memberCount": 1,
    "createTime": "2024-01-20T10:00:00Z"
}
```

## 8. 消息发送接口

### 8.1 发送聊天消息
```javascript
function sendChatMessage(conversationId, content, msgType = MessageType.CHAT_TEXT) {
    const message = {
        destination: '/app/chat',
        body: {
            conversationId: conversationId,
            content: content,
            msgType: msgType,
            clientMsgId: generateClientMsgId(),
            timestamp: Date.now()
        }
    };
    
    stompClient.send('/app/chat', {}, JSON.stringify(message.body));
}

function sendPrivateMessage(receiverId, content) {
    const message = {
        destination: '/app/private-message',
        body: {
            receiverId: receiverId,
            content: content,
            clientMsgId: generateClientMsgId(),
            timestamp: Date.now()
        }
    };
    
    stompClient.send('/app/private-message', {}, JSON.stringify(message.body));
}
```

### 8.2 发送特殊操作消息
```javascript
// 编辑消息
function editMessage(messageId, newContent) {
    const message = {
        destination: '/app/edit-message',
        body: {
            messageId: messageId,
            newContent: newContent,
            editReason: '用户编辑',
            timestamp: Date.now()
        }
    };
    
    stompClient.send('/app/edit-message', {}, JSON.stringify(message.body));
}

// 撤回消息
function recallMessage(messageId, reason = '用户撤回') {
    const message = {
        destination: '/app/delete-message',
        body: {
            messageId: messageId,
            deleteScope: 0, // 0-仅我，1-所有人
            deleteReason: reason,
            timestamp: Date.now()
        }
    };
    
    stompClient.send('/app/delete-message', {}, JSON.stringify(message.body));
}

// 置顶消息
function pinMessage(messageId, pinScope = 0) {
    const message = {
        destination: '/app/pin-message',
        body: {
            messageId: messageId,
            pinScope: pinScope, // 0-仅我，1-所有人
            timestamp: Date.now()
        }
    };
    
    stompClient.send('/app/pin-message', {}, JSON.stringify(message.body));
}
```

### 8.3 系统消息发送
```javascript
// 发送心跳
function sendHeartbeat() {
    const message = {
        destination: '/app/heartbeat',
        body: {
            timestamp: Date.now(),
            deviceId: getDeviceId()
        }
    };
    
    stompClient.send('/app/heartbeat', {}, JSON.stringify(message.body));
}

// 发送认证消息
function sendAuthMessage(token) {
    const message = {
        destination: '/app/auth',
        body: {
            token: token,
            timestamp: Date.now()
        }
    };
    
    stompClient.send('/app/auth', {}, JSON.stringify(message.body));
}
```

## 9. 错误处理

### 9.1 连接错误处理
```javascript
function handleConnectionError(error) {
    console.error('WebSocket连接错误:', error);
    
    // 根据错误类型进行处理
    if (error.code === 'ECONNREFUSED') {
        showErrorNotification('连接被拒绝，请检查网络');
        scheduleReconnect(5000); // 5秒后重连
    } else if (error.code === 'ETIMEDOUT') {
        showErrorNotification('连接超时，正在重试');
        scheduleReconnect(3000); // 3秒后重连
    } else {
        showErrorNotification('连接失败，请稍后重试');
        scheduleReconnect(10000); // 10秒后重连
    }
}

function scheduleReconnect(delay) {
    setTimeout(() => {
        console.log('尝试重新连接...');
        connectWebSocket();
    }, delay);
}
```

### 9.2 消息错误处理
```javascript
function handleMessageError(error) {
    console.error('消息处理错误:', error);
    
    // 显示错误通知
    showErrorNotification({
        title: '消息处理失败',
        content: error.message || '未知错误',
        type: 'error'
    });
    
    // 记录错误日志
    logError('message_error', {
        error: error.message,
        stack: error.stack,
        timestamp: Date.now()
    });
}
```

### 9.3 重连机制
```javascript
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;
const baseReconnectDelay = 1000;

function connectWebSocket() {
    if (reconnectAttempts >= maxReconnectAttempts) {
        showErrorNotification('重连次数已达上限，请刷新页面');
        return;
    }
    
    const delay = baseReconnectDelay * Math.pow(2, reconnectAttempts);
    reconnectAttempts++;
    
    setTimeout(() => {
        console.log(`第${reconnectAttempts}次重连尝试...`);
        initWebSocketConnection();
    }, delay);
}

function onConnectionSuccess() {
    reconnectAttempts = 0; // 重置重连计数
    console.log('WebSocket连接成功');
}
```

## 10. 最佳实践

### 10.1 连接管理
```javascript
class WebSocketManager {
    constructor() {
        this.stompClient = null;
        this.isConnected = false;
        this.reconnectTimer = null;
        this.heartbeatTimer = null;
    }
    
    connect() {
        // 建立连接
        this.stompClient = Stomp.over(new SockJS('/ws'));
        
        this.stompClient.connect(
            this.getConnectHeaders(),
            this.onConnect.bind(this),
            this.onError.bind(this)
        );
    }
    
    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
        }
        this.clearTimers();
    }
    
    clearTimers() {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
        }
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
        }
    }
    
    startHeartbeat() {
        this.heartbeatTimer = setInterval(() => {
            this.sendHeartbeat();
        }, 30000); // 30秒发送一次心跳
    }
}
```

### 10.2 消息队列管理
```javascript
class MessageQueue {
    constructor() {
        this.queue = [];
        this.isProcessing = false;
    }
    
    addMessage(message) {
        this.queue.push(message);
        if (!this.isProcessing) {
            this.processQueue();
        }
    }
    
    async processQueue() {
        if (this.isProcessing || this.queue.length === 0) {
            return;
        }
        
        this.isProcessing = true;
        
        while (this.queue.length > 0) {
            const message = this.queue.shift();
            try {
                await this.processMessage(message);
            } catch (error) {
                console.error('处理消息失败:', error);
                // 失败的消息重新入队
                this.queue.unshift(message);
                break;
            }
        }
        
        this.isProcessing = false;
    }
    
    async processMessage(message) {
        // 根据消息类型进行处理
        switch (message.type) {
            case MessageType.CHAT_TEXT:
                await this.handleChatMessage(message);
                break;
            case MessageType.NOTIFICATION_ANNOUNCEMENT:
                await this.handleNotification(message);
                break;
            default:
                console.warn('未知消息类型:', message.type);
        }
    }
}
```

### 10.3 性能优化
```javascript
// 消息去重
const processedMessages = new Set();

function handleMessage(message) {
    if (processedMessages.has(message.messageId)) {
        return; // 消息已处理，跳过
    }
    
    processedMessages.add(message.messageId);
    
    // 限制缓存大小
    if (processedMessages.size > 1000) {
        const firstKey = processedMessages.keys().next().value;
        processedMessages.delete(firstKey);
    }
    
    // 处理消息
    processMessage(message);
}

// 批量处理
let messageBatch = [];
const batchSize = 10;
const batchTimeout = 100; // 100ms

function addToBatch(message) {
    messageBatch.push(message);
    
    if (messageBatch.length >= batchSize) {
        processBatch();
    } else if (messageBatch.length === 1) {
        // 第一个消息启动定时器
        setTimeout(processBatch, batchTimeout);
    }
}

function processBatch() {
    if (messageBatch.length === 0) return;
    
    const batch = messageBatch.splice(0);
    // 批量处理消息
    batch.forEach(processMessage);
}
```

### 10.4 安全考虑
```javascript
// Token验证
function validateToken(token) {
    if (!token) {
        throw new Error('Token不能为空');
    }
    
    // 检查Token格式
    if (!/^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]*$/.test(token)) {
        throw new Error('Token格式无效');
    }
    
    return true;
}

// 消息内容过滤
function sanitizeMessage(content) {
    // 移除HTML标签
    content = content.replace(/<[^>]*>/g, '');
    
    // 限制消息长度
    if (content.length > 2000) {
        content = content.substring(0, 2000);
    }
    
    return content;
}

// 频率限制
class RateLimiter {
    constructor(maxRequests, timeWindow) {
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
        this.requests = [];
    }
    
    canMakeRequest() {
        const now = Date.now();
        this.requests = this.requests.filter(time => now - time < this.timeWindow);
        
        if (this.requests.length >= this.maxRequests) {
            return false;
        }
        
        this.requests.push(now);
        return true;
    }
}

const messageRateLimiter = new RateLimiter(10, 1000); // 每秒最多10条消息
```

---

## 附录

### A. 消息状态流转图
```
发送中 -> 已发送 -> 已投递 -> 已读
   |         |         |
   v         v         v
发送失败  投递失败   已撤回
```

### B. 错误代码对照表
| 错误代码 | 含义 | 处理建议 |
|---------|------|----------|
| ECONNREFUSED | 连接被拒绝 | 检查网络和服务状态 |
| ETIMEDOUT | 连接超时 | 增加超时时间或重试 |
| EPERM | 权限不足 | 检查用户权限 |
| EINVAL | 参数无效 | 检查消息格式 |

### C. 调试工具
```javascript
// 启用STOMP调试
Stomp.setDebug(console.log);

// 消息日志
function logMessage(direction, message) {
    console.log(`[${direction}]`, {
        timestamp: new Date().toISOString(),
        messageId: message.messageId,
        type: message.type,
        senderId: message.senderId,
        conversationId: message.conversationId
    });
}
```

---

**文档版本**: 2.0.0  
**最后更新**: 2024-01-20  
**维护团队**: IM开发团队
