// IM系统消息数据库 - MongoDB建库脚本
// 基于分布式单体架构设计
// 维护方：im-core-service
// 版本：v1.0.0
// 数据库名称：im_messages

// 连接到MongoDB实例
const dbName = 'im_messages';
const db = db.getSiblingDB(dbName);

print('开始初始化IM消息数据库...');

// ============================================
// 1. 消息存储集合设计（分片存储）
// ============================================

// 创建消息集合（按会话ID分片，支持32个分片）
for (let i = 0; i < 32; i++) {
    const collectionName = `message_${i.toString().padStart(2, '0')}`;
    
    // 创建消息集合
    db.createCollection(collectionName, {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["messageId", "conversationId", "seq", "fromUserId", "messageType", "content", "createdAt"],
                properties: {
                    messageId: {
                        bsonType: "string",
                        description: "消息唯一ID，必填"
                    },
                    conversationId: {
                        bsonType: "string",
                        description: "会话ID，必填"
                    },
                    seq: {
                        bsonType: "long",
                        description: "消息序号，必填"
                    },
                    fromUserId: {
                        bsonType: "long",
                        description: "发送者用户ID，必填"
                    },
                    messageType: {
                        bsonType: "int",
                        minimum: 1,
                        maximum: 20,
                        description: "消息类型：1-文本 2-图片 3-语音 4-视频 5-文件 6-位置 7-名片 8-表情 9-引用 10-撤回 11-系统通知"
                    },
                    content: {
                        bsonType: "object",
                        description: "消息内容，必填"
                    },
                    status: {
                        bsonType: "int",
                        minimum: 0,
                        maximum: 5,
                        description: "消息状态：0-发送中 1-已发送 2-已送达 3-已读 4-撤回 5-删除"
                    },
                    createdAt: {
                        bsonType: "date",
                        description: "创建时间，必填"
                    },
                    updatedAt: {
                        bsonType: "date",
                        description: "更新时间"
                    },
                    clientMsgId: {
                        bsonType: "string",
                        description: "客户端消息ID，用于去重"
                    },
                    replyToMsgId: {
                        bsonType: "string",
                        description: "回复的消息ID"
                    },
                    mediaRef: {
                        bsonType: "object",
                        description: "媒体引用信息"
                    },
                    mentions: {
                        bsonType: "array",
                        description: "@用户列表"
                    },
                    extra: {
                        bsonType: "object",
                        description: "扩展字段"
                    }
                }
            }
        }
    });

    // 创建消息集合的索引
    // 主要查询索引：按会话ID和序号查询
    db[collectionName].createIndex(
        { "conversationId": 1, "seq": 1 },
        { 
            name: "idx_conversation_seq",
            unique: true,
            background: true 
        }
    );

    // 时间范围查询索引
    db[collectionName].createIndex(
        { "conversationId": 1, "createdAt": -1 },
        { 
            name: "idx_conversation_time",
            background: true 
        }
    );

    // 消息ID唯一索引
    db[collectionName].createIndex(
        { "messageId": 1 },
        { 
            name: "idx_message_id",
            unique: true,
            background: true 
        }
    );

    // 客户端消息ID索引（用于去重）
    db[collectionName].createIndex(
        { "clientMsgId": 1 },
        { 
            name: "idx_client_msg_id",
            sparse: true,
            background: true 
        }
    );

    // 发送者查询索引
    db[collectionName].createIndex(
        { "conversationId": 1, "fromUserId": 1, "createdAt": -1 },
        { 
            name: "idx_conversation_sender_time",
            background: true 
        }
    );

    // 消息类型查询索引
    db[collectionName].createIndex(
        { "conversationId": 1, "messageType": 1, "createdAt": -1 },
        { 
            name: "idx_conversation_type_time",
            background: true 
        }
    );

    // 状态查询索引
    db[collectionName].createIndex(
        { "conversationId": 1, "status": 1, "createdAt": -1 },
        { 
            name: "idx_conversation_status_time",
            background: true 
        }
    );

    print(`创建消息集合: ${collectionName} 完成`);
}

// ============================================
// 2. 消息归档集合（冷数据存储）
// ============================================

// 按年月创建归档集合模板
function createArchiveCollection(yearMonth) {
    const collectionName = `message_archive_${yearMonth}`;
    
    db.createCollection(collectionName, {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["messageId", "conversationId", "seq", "fromUserId", "messageType", "content", "createdAt", "archivedAt"],
                properties: {
                    messageId: { bsonType: "string" },
                    conversationId: { bsonType: "string" },
                    seq: { bsonType: "long" },
                    fromUserId: { bsonType: "long" },
                    messageType: { bsonType: "int", minimum: 1, maximum: 20 },
                    content: { bsonType: "object" },
                    status: { bsonType: "int", minimum: 0, maximum: 5 },
                    createdAt: { bsonType: "date" },
                    archivedAt: { bsonType: "date", description: "归档时间" },
                    clientMsgId: { bsonType: "string" },
                    replyToMsgId: { bsonType: "string" },
                    mediaRef: { bsonType: "object" },
                    mentions: { bsonType: "array" },
                    extra: { bsonType: "object" }
                }
            }
        }
    });

    // 归档集合索引
    db[collectionName].createIndex(
        { "conversationId": 1, "seq": 1 },
        { name: "idx_archive_conversation_seq", background: true }
    );

    db[collectionName].createIndex(
        { "conversationId": 1, "createdAt": -1 },
        { name: "idx_archive_conversation_time", background: true }
    );

    db[collectionName].createIndex(
        { "messageId": 1 },
        { name: "idx_archive_message_id", unique: true, background: true }
    );

    // 设置TTL索引，180天后自动删除
    db[collectionName].createIndex(
        { "archivedAt": 1 },
        { 
            name: "idx_archive_ttl",
            expireAfterSeconds: 15552000, // 180天 = 180 * 24 * 60 * 60
            background: true 
        }
    );

    print(`创建归档集合: ${collectionName} 完成`);
}

// 创建当前年月和未来几个月的归档集合
const now = new Date();
for (let i = 0; i < 6; i++) {
    const date = new Date(now.getFullYear(), now.getMonth() + i, 1);
    const yearMonth = date.getFullYear().toString() + (date.getMonth() + 1).toString().padStart(2, '0');
    createArchiveCollection(yearMonth);
}

// ============================================
// 3. 用户时间线集合（按用户ID分片）
// ============================================

for (let i = 0; i < 32; i++) {
    const collectionName = `timeline_${i.toString().padStart(2, '0')}`;
    
    db.createCollection(collectionName, {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["userId", "conversationId", "messageId", "seq", "messageType", "fromUserId", "createdAt"],
                properties: {
                    userId: {
                        bsonType: "long",
                        description: "用户ID，必填"
                    },
                    conversationId: {
                        bsonType: "string",
                        description: "会话ID，必填"
                    },
                    messageId: {
                        bsonType: "string",
                        description: "消息ID，必填"
                    },
                    seq: {
                        bsonType: "long",
                        description: "消息序号，必填"
                    },
                    messageType: {
                        bsonType: "int",
                        minimum: 1,
                        maximum: 20,
                        description: "消息类型，必填"
                    },
                    fromUserId: {
                        bsonType: "long",
                        description: "发送者用户ID，必填"
                    },
                    isRead: {
                        bsonType: "bool",
                        description: "是否已读"
                    },
                    readAt: {
                        bsonType: "date",
                        description: "已读时间"
                    },
                    createdAt: {
                        bsonType: "date",
                        description: "创建时间，必填"
                    },
                    extra: {
                        bsonType: "object",
                        description: "扩展字段"
                    }
                }
            }
        }
    });

    // 用户时间线索引
    db[collectionName].createIndex(
        { "userId": 1, "createdAt": -1 },
        { 
            name: "idx_user_timeline",
            background: true 
        }
    );

    // 用户会话时间线索引
    db[collectionName].createIndex(
        { "userId": 1, "conversationId": 1, "seq": 1 },
        { 
            name: "idx_user_conversation_seq",
            background: true 
        }
    );

    // 消息ID索引
    db[collectionName].createIndex(
        { "messageId": 1 },
        { 
            name: "idx_timeline_message_id",
            background: true 
        }
    );

    // 未读消息索引
    db[collectionName].createIndex(
        { "userId": 1, "conversationId": 1, "isRead": 1, "createdAt": -1 },
        { 
            name: "idx_user_conversation_unread",
            background: true 
        }
    );

    print(`创建时间线集合: ${collectionName} 完成`);
}

// ============================================
// 4. 会话序号集合（序号分配）
// ============================================

db.createCollection('conversation_sequence', {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["conversationId", "currentSeq", "updatedAt"],
            properties: {
                conversationId: {
                    bsonType: "string",
                    description: "会话ID，必填"
                },
                currentSeq: {
                    bsonType: "long",
                    description: "当前序号，必填"
                },
                lastMessageAt: {
                    bsonType: "date",
                    description: "最后消息时间"
                },
                messageCount: {
                    bsonType: "long",
                    description: "消息总数"
                },
                createdAt: {
                    bsonType: "date",
                    description: "创建时间"
                },
                updatedAt: {
                    bsonType: "date",
                    description: "更新时间，必填"
                }
            }
        }
    }
});

// 会话序号索引
db.conversation_sequence.createIndex(
    { "conversationId": 1 },
    { 
        name: "idx_conversation_id",
        unique: true,
        background: true 
    }
);

db.conversation_sequence.createIndex(
    { "lastMessageAt": -1 },
    { 
        name: "idx_last_message_time",
        background: true 
    }
);

// ============================================
// 5. 幂等性控制集合
// ============================================

db.createCollection('message_idempotent', {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["clientMsgId", "messageId", "conversationId", "seq", "createdAt"],
            properties: {
                clientMsgId: {
                    bsonType: "string",
                    description: "客户端消息ID，必填"
                },
                messageId: {
                    bsonType: "string",
                    description: "服务端消息ID，必填"
                },
                conversationId: {
                    bsonType: "string",
                    description: "会话ID，必填"
                },
                seq: {
                    bsonType: "long",
                    description: "消息序号，必填"
                },
                fromUserId: {
                    bsonType: "long",
                    description: "发送者用户ID"
                },
                status: {
                    bsonType: "int",
                    description: "消息状态"
                },
                createdAt: {
                    bsonType: "date",
                    description: "创建时间，必填"
                }
            }
        }
    }
});

// 幂等性索引
db.message_idempotent.createIndex(
    { "clientMsgId": 1 },
    { 
        name: "idx_client_msg_id",
        unique: true,
        background: true 
    }
);

db.message_idempotent.createIndex(
    { "messageId": 1 },
    { 
        name: "idx_message_id",
        background: true 
    }
);

db.message_idempotent.createIndex(
    { "conversationId": 1 },
    { 
        name: "idx_conversation_id",
        background: true 
    }
);

// 设置TTL索引，7天后自动清理
db.message_idempotent.createIndex(
    { "createdAt": 1 },
    { 
        name: "idx_ttl",
        expireAfterSeconds: 604800, // 7天 = 7 * 24 * 60 * 60
        background: true 
    }
);

// ============================================
// 6. 消息统计集合
// ============================================

db.createCollection('message_statistics', {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["conversationId", "date", "messageCount"],
            properties: {
                conversationId: {
                    bsonType: "string",
                    description: "会话ID，必填"
                },
                date: {
                    bsonType: "string",
                    description: "日期(YYYY-MM-DD)，必填"
                },
                messageCount: {
                    bsonType: "long",
                    description: "消息数量，必填"
                },
                userCount: {
                    bsonType: "int",
                    description: "活跃用户数"
                },
                mediaCount: {
                    bsonType: "int",
                    description: "媒体消息数"
                },
                typeStats: {
                    bsonType: "object",
                    description: "消息类型统计"
                },
                hourlyStats: {
                    bsonType: "array",
                    description: "小时级统计"
                },
                updatedAt: {
                    bsonType: "date",
                    description: "更新时间"
                }
            }
        }
    }
});

// 统计集合索引
db.message_statistics.createIndex(
    { "conversationId": 1, "date": -1 },
    { 
        name: "idx_conversation_date",
        unique: true,
        background: true 
    }
);

db.message_statistics.createIndex(
    { "date": -1 },
    { 
        name: "idx_date",
        background: true 
    }
);

// ============================================
// 7. 搜索索引集合（全文搜索）
// ============================================

db.createCollection('message_search_index', {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["messageId", "conversationId", "content", "keywords", "createdAt"],
            properties: {
                messageId: {
                    bsonType: "string",
                    description: "消息ID，必填"
                },
                conversationId: {
                    bsonType: "string",
                    description: "会话ID，必填"
                },
                fromUserId: {
                    bsonType: "long",
                    description: "发送者用户ID"
                },
                content: {
                    bsonType: "string",
                    description: "搜索内容，必填"
                },
                keywords: {
                    bsonType: "array",
                    description: "关键词数组，必填"
                },
                messageType: {
                    bsonType: "int",
                    description: "消息类型"
                },
                createdAt: {
                    bsonType: "date",
                    description: "创建时间，必填"
                }
            }
        }
    }
});

// 搜索索引
db.message_search_index.createIndex(
    { "content": "text", "keywords": "text" },
    { 
        name: "idx_fulltext_search",
        background: true,
        default_language: "none",
        language_override: "language"
    }
);

db.message_search_index.createIndex(
    { "conversationId": 1, "createdAt": -1 },
    { 
        name: "idx_conversation_time",
        background: true 
    }
);

db.message_search_index.createIndex(
    { "messageId": 1 },
    { 
        name: "idx_message_id",
        unique: true,
        background: true 
    }
);

// ============================================
// 8. 工具函数
// ============================================

// 根据conversationId计算分片
function getMessageShardIndex(conversationId) {
    // 使用简单哈希算法计算分片索引
    let hash = 0;
    for (let i = 0; i < conversationId.length; i++) {
        hash = ((hash << 5) - hash + conversationId.charCodeAt(i)) & 0xffffffff;
    }
    return Math.abs(hash) % 32;
}

// 根据userId计算时间线分片
function getTimelineShardIndex(userId) {
    return userId % 32;
}

// 获取消息集合名称
function getMessageCollectionName(conversationId) {
    const shardIndex = getMessageShardIndex(conversationId);
    return `message_${shardIndex.toString().padStart(2, '0')}`;
}

// 获取时间线集合名称
function getTimelineCollectionName(userId) {
    const shardIndex = getTimelineShardIndex(userId);
    return `timeline_${shardIndex.toString().padStart(2, '0')}`;
}

// 将工具函数存储为数据库函数
db.system.js.insertOne({
    _id: "getMessageShardIndex",
    value: function(conversationId) {
        let hash = 0;
        for (let i = 0; i < conversationId.length; i++) {
            hash = ((hash << 5) - hash + conversationId.charCodeAt(i)) & 0xffffffff;
        }
        return Math.abs(hash) % 32;
    }
});

db.system.js.insertOne({
    _id: "getTimelineShardIndex", 
    value: function(userId) {
        return userId % 32;
    }
});

db.system.js.insertOne({
    _id: "getMessageCollectionName",
    value: function(conversationId) {
        const shardIndex = getMessageShardIndex(conversationId);
        return `message_${shardIndex.toString().padStart(2, '0')}`;
    }
});

db.system.js.insertOne({
    _id: "getTimelineCollectionName",
    value: function(userId) {
        const shardIndex = getTimelineShardIndex(userId);
        return `timeline_${shardIndex.toString().padStart(2, '0')}`;
    }
});

// ============================================
// 9. 初始化数据
// ============================================

// 插入一些测试会话的序号初始化数据
const testConversations = [
    'conv_test_001',
    'conv_test_002', 
    'conv_test_003'
];

testConversations.forEach(conversationId => {
    db.conversation_sequence.insertOne({
        conversationId: conversationId,
        currentSeq: NumberLong(0),
        lastMessageAt: new Date(),
        messageCount: NumberLong(0),
        createdAt: new Date(),
        updatedAt: new Date()
    });
});

// ============================================
// 10. 数据库配置优化
// ============================================

// 设置数据库级别的配置
db.adminCommand({
    setParameter: 1,
    // 启用部分索引
    partialIndexes: true,
    // 优化写入性能
    journalCommitInterval: 100,
    // 设置oplog大小
    oplogSizeMB: 2048
});

// ============================================
// 11. 监控和维护脚本
// ============================================

// 创建数据库状态监控函数
db.system.js.insertOne({
    _id: "getDatabaseStats",
    value: function() {
        const stats = {
            database: db.getName(),
            collections: {},
            totalSize: 0,
            totalIndexSize: 0
        };
        
        db.runCommand("listCollections").cursor.firstBatch.forEach(function(collection) {
            const collStats = db.runCommand({collStats: collection.name});
            stats.collections[collection.name] = {
                count: collStats.count,
                size: collStats.size,
                avgObjSize: collStats.avgObjSize,
                indexSize: collStats.totalIndexSize
            };
            stats.totalSize += collStats.size;
            stats.totalIndexSize += collStats.totalIndexSize;
        });
        
        return stats;
    }
});

// 创建清理过期数据函数
db.system.js.insertOne({
    _id: "cleanupExpiredData",
    value: function(daysOld) {
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - daysOld);
        
        let totalDeleted = 0;
        
        // 清理过期的幂等性记录
        const idempotentResult = db.message_idempotent.deleteMany({
            createdAt: { $lt: cutoffDate }
        });
        totalDeleted += idempotentResult.deletedCount;
        
        print(`清理完成，共删除 ${totalDeleted} 条过期记录`);
        return totalDeleted;
    }
});

// ============================================
// 12. 分片配置建议（如果使用MongoDB Sharded Cluster）
// ============================================

print(`
=== MongoDB分片配置建议 ===
如果使用MongoDB分片集群，建议进行以下配置：

1. 消息集合分片键建议：
   - 分片键: { "conversationId": "hashed" }
   - 原因: 确保会话消息分布均匀，避免热点

2. 时间线集合分片键建议：
   - 分片键: { "userId": "hashed" }
   - 原因: 确保用户数据分布均匀

3. 分片命令示例：
   sh.enableSharding("im_messages")
   
   // 对消息集合进行分片
   for (let i = 0; i < 32; i++) {
       const collectionName = "im_messages.message_" + i.toString().padStart(2, '0');
       sh.shardCollection(collectionName, { "conversationId": "hashed" });
   }
   
   // 对时间线集合进行分片  
   for (let i = 0; i < 32; i++) {
       const collectionName = "im_messages.timeline_" + i.toString().padStart(2, '0');
       sh.shardCollection(collectionName, { "userId": "hashed" });
   }

4. 其他集合分片：
   sh.shardCollection("im_messages.conversation_sequence", { "conversationId": "hashed" });
   sh.shardCollection("im_messages.message_idempotent", { "clientMsgId": "hashed" });
`);

// ============================================
// 13. 性能优化建议
// ============================================

print(`
=== 性能优化建议 ===

1. 读写分离：
   - 消息写入：Primary节点
   - 历史查询：Secondary节点（设置读偏好为secondary）
   - 统计查询：Secondary节点

2. 连接池配置：
   - 最大连接数：每个应用节点100-200个连接
   - 连接超时：30秒
   - Socket超时：0（禁用）

3. 写关注（Write Concern）：
   - 消息写入：{ w: 1, j: true } （确保写入日志）
   - 统计数据：{ w: 1, j: false } （允许异步写入）

4. 读关注（Read Concern）：
   - 消息查询：majority（确保一致性）
   - 统计查询：local（允许最终一致性）

5. 索引优化：
   - 定期分析慢查询日志
   - 使用explain()分析查询计划
   - 避免全表扫描

6. 内存优化：
   - WiredTiger缓存：物理内存的50%
   - 索引缓存：尽量保持热点索引在内存中
`);

// 脚本执行完成
print('=== MongoDB消息数据库初始化完成！===');
print(`数据库名称: ${dbName}`);
print('集合统计:');
db.runCommand("listCollections").cursor.firstBatch.forEach(function(collection) {
    print(`- ${collection.name}`);
});

print('');
print('工具函数已创建:');
print('- getMessageShardIndex(conversationId)');
print('- getTimelineShardIndex(userId)'); 
print('- getMessageCollectionName(conversationId)');
print('- getTimelineCollectionName(userId)');
print('- getDatabaseStats()');
print('- cleanupExpiredData(daysOld)');

print('');
print('数据库初始化完成，可以开始使用！');
