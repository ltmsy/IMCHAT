// IM系统消息数据库优化版 - MongoDB建库脚本
// 基于分布式单体架构设计，包含高级性能优化
// 维护方：im-core-service
// 版本：v2.0.0
// 数据库名称：im_messages

// 连接到MongoDB实例
const dbName = 'im_messages';
const db = db.getSiblingDB(dbName);

print('开始初始化IM消息数据库优化版...');

// ============================================
// 1. 消息存储集合设计（双重分片策略）
// ============================================

// 创建消息集合（按时间+会话ID双重分片）
for (let year = 2024; year <= 2026; year++) {
    for (let month = 1; month <= 12; month++) {
        const yearMonth = `${year}${month.toString().padStart(2, '0')}`;
        const collectionName = `message_${yearMonth}`;
        
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
                        editHistory: {
                            bsonType: "array",
                            description: "编辑历史记录"
                        },
                        pinInfo: {
                            bsonType: "object",
                            description: "置顶信息"
                        },
                        extra: {
                            bsonType: "object",
                            description: "扩展字段"
                        }
                    }
                }
            }
        });

        // 创建消息集合的索引（优化版）
        // 主要查询索引：按会话ID和序号查询
        db[collectionName].createIndex(
            { "conversationId": 1, "seq": 1 },
            { 
                name: "idx_conversation_seq",
                unique: true,
                background: true 
            }
        );

        // 按时间查询索引
        db[collectionName].createIndex(
            { "createdAt": -1 },
            { 
                name: "idx_created_at",
                background: true 
            }
        );

        // 按发送者查询索引
        db[collectionName].createIndex(
            { "fromUserId": 1, "createdAt": -1 },
            { 
                name: "idx_from_user_time",
                background: true 
            }
        );

        // 按消息类型查询索引
        db[collectionName].createIndex(
            { "messageType": 1, "createdAt": -1 },
            { 
                name: "idx_type_time",
                background: true 
            }
        );

        // 按客户端消息ID查询索引（幂等性）
        db[collectionName].createIndex(
            { "clientMsgId": 1 },
            { 
                name: "idx_client_msg_id",
                background: true 
            }
        );

        // 复合索引：会话+时间+类型
        db[collectionName].createIndex(
            { "conversationId": 1, "createdAt": -1, "messageType": 1 },
            { 
                name: "idx_conv_time_type",
                background: true 
            }
        );

        // 文本搜索索引
        db[collectionName].createIndex(
            { "content.text": "text" },
            { 
                name: "idx_content_text",
                background: true,
                default_language: "chinese"
            }
        );

        print(`创建消息集合: ${collectionName}`);
    }
}

// 创建归档消息集合（按年份分片）
for (let year = 2020; year <= 2023; year++) {
    const collectionName = `message_archive_${year}`;
    
    db.createCollection(collectionName, {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["messageId", "conversationId", "seq", "fromUserId", "messageType", "content", "createdAt"],
                properties: {
                    messageId: { bsonType: "string" },
                    conversationId: { bsonType: "string" },
                    seq: { bsonType: "long" },
                    fromUserId: { bsonType: "long" },
                    messageType: { bsonType: "int" },
                    content: { bsonType: "object" },
                    status: { bsonType: "int" },
                    createdAt: { bsonType: "date" },
                    archivedAt: { bsonType: "date" }
                }
            }
        }
    });

    // 归档集合的索引（简化版）
    db[collectionName].createIndex(
        { "conversationId": 1, "seq": 1 },
        { name: "idx_conv_seq", background: true }
    );

    db[collectionName].createIndex(
        { "createdAt": -1 },
        { name: "idx_created_at", background: true }
    );

    print(`创建归档消息集合: ${collectionName}`);
}

// ============================================
// 2. 用户时间线集合（优化版）
// ============================================

// 创建用户时间线集合（按用户ID哈希分片）
for (let i = 0; i < 32; i++) {
    const collectionName = `timeline_${i.toString().padStart(2, '0')}`;
    
    db.createCollection(collectionName, {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["userId", "conversationId", "messageId", "seq", "timestamp"],
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
                        description: "消息类型"
                    },
                    fromUserId: {
                        bsonType: "long",
                        description: "发送者用户ID"
                    },
                    content: {
                        bsonType: "object",
                        description: "消息内容摘要"
                    },
                    timestamp: {
                        bsonType: "date",
                        description: "时间戳，必填"
                    },
                    isRead: {
                        bsonType: "bool",
                        description: "是否已读"
                    },
                    isPinned: {
                        bsonType: "bool",
                        description: "是否置顶"
                    },
                    extra: {
                        bsonType: "object",
                        description: "扩展字段"
                    }
                }
            }
        }
    });

    // 时间线集合的索引
    db[collectionName].createIndex(
        { "userId": 1, "timestamp": -1 },
        { 
            name: "idx_user_time",
            background: true 
        }
    );

    db[collectionName].createIndex(
        { "userId": 1, "conversationId": 1, "timestamp": -1 },
        { 
            name: "idx_user_conv_time",
            background: true 
        }
    );

    db[collectionName].createIndex(
        { "conversationId": 1, "timestamp": -1 },
        { 
            name: "idx_conv_time",
            background: true 
        }
    );

    print(`创建时间线集合: ${collectionName}`);
}

// ============================================
// 3. 在线状态持久化集合（新增）
// ============================================

// 在线状态历史集合（按时间分片）
for (let year = 2024; year <= 2026; year++) {
    for (let month = 1; month <= 12; month++) {
        const yearMonth = `${year}${month.toString().padStart(2, '0')}`;
        const collectionName = `presence_history_${yearMonth}`;
        
        db.createCollection(collectionName, {
            validator: {
                $jsonSchema: {
                    bsonType: "object",
                    required: ["userId", "deviceId", "nodeId", "presenceType", "newStatus", "timestamp"],
                    properties: {
                        userId: {
                            bsonType: "long",
                            description: "用户ID，必填"
                        },
                        deviceId: {
                            bsonType: "string",
                            description: "设备ID，必填"
                        },
                        nodeId: {
                            bsonType: "string",
                            description: "节点ID，必填"
                        },
                        presenceType: {
                            bsonType: "int",
                            minimum: 1,
                            maximum: 3,
                            description: "状态类型：1-上线 2-下线 3-状态变更"
                        },
                        oldStatus: {
                            bsonType: "string",
                            description: "原状态"
                        },
                        newStatus: {
                            bsonType: "string",
                            description: "新状态，必填"
                        },
                        ipAddress: {
                            bsonType: "string",
                            description: "IP地址"
                        },
                        userAgent: {
                            bsonType: "string",
                            description: "用户代理"
                        },
                        location: {
                            bsonType: "string",
                            description: "地理位置"
                        },
                        timestamp: {
                            bsonType: "date",
                            description: "时间戳，必填"
                        },
                        metadata: {
                            bsonType: "object",
                            description: "元数据"
                        }
                    }
                }
            }
        });

        // 在线状态历史索引
        db[collectionName].createIndex(
            { "userId": 1, "timestamp": -1 },
            { name: "idx_user_time", background: true }
        );

        db[collectionName].createIndex(
            { "deviceId": 1, "timestamp": -1 },
            { name: "idx_device_time", background: true }
        );

        db[collectionName].createIndex(
            { "nodeId": 1, "timestamp": -1 },
            { name: "idx_node_time", background: true }
        );

        db[collectionName].createIndex(
            { "presenceType": 1, "timestamp": -1 },
            { name: "idx_type_time", background: true }
        );

        print(`创建在线状态历史集合: ${collectionName}`);
    }
}

// 在线状态快照集合（读写分离）
db.createCollection("presence_snapshot", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["userId", "deviceId", "nodeId", "status", "lastSeenAt"],
            properties: {
                userId: {
                    bsonType: "long",
                    description: "用户ID，必填"
                },
                deviceId: {
                    bsonType: "string",
                    description: "设备ID，必填"
                },
                nodeId: {
                    bsonType: "string",
                    description: "节点ID，必填"
                },
                status: {
                    bsonType: "string",
                    description: "当前状态，必填"
                },
                lastSeenAt: {
                    bsonType: "date",
                    description: "最后活跃时间，必填"
                },
                ipAddress: {
                    bsonType: "string",
                    description: "IP地址"
                },
                userAgent: {
                    bsonType: "string",
                    description: "用户代理"
                },
                location: {
                    bsonType: "string",
                    description: "地理位置"
                },
                metadata: {
                    bsonType: "object",
                    description: "元数据"
                }
            }
        }
    }
});

// 在线状态快照索引
db.presence_snapshot.createIndex(
    { "userId": 1, "deviceId": 1 },
    { 
        name: "idx_user_device",
        unique: true,
        background: true 
    }
);

db.presence_snapshot.createIndex(
    { "userId": 1, "status": 1, "lastSeenAt": -1 },
    { name: "idx_user_status_time", background: true }
);

db.presence_snapshot.createIndex(
    { "nodeId": 1, "status": 1 },
    { name: "idx_node_status", background: true }
);

// ============================================
// 4. 性能监控集合（新增）
// ============================================

// 客户端性能监控集合（按时间分片）
for (let year = 2024; year <= 2026; year++) {
    for (let month = 1; month <= 12; month++) {
        const yearMonth = `${year}${month.toString().padStart(2, '0')}`;
        const collectionName = `client_performance_${yearMonth}`;
        
        db.createCollection(collectionName, {
            validator: {
                $jsonSchema: {
                    bsonType: "object",
                    required: ["userId", "deviceId", "metricName", "metricValue", "timestamp"],
                    properties: {
                        userId: {
                            bsonType: "long",
                            description: "用户ID，必填"
                        },
                        deviceId: {
                            bsonType: "string",
                            description: "设备ID，必填"
                        },
                        metricName: {
                            bsonType: "string",
                            description: "指标名称，必填"
                        },
                        metricValue: {
                            bsonType: "double",
                            description: "指标值，必填"
                        },
                        metricUnit: {
                            bsonType: "string",
                            description: "指标单位"
                        },
                        clientVersion: {
                            bsonType: "string",
                            description: "客户端版本"
                        },
                        osVersion: {
                            bsonType: "string",
                            description: "操作系统版本"
                        },
                        networkType: {
                            bsonType: "string",
                            description: "网络类型"
                        },
                        timestamp: {
                            bsonType: "date",
                            description: "时间戳，必填"
                        },
                        metadata: {
                            bsonType: "object",
                            description: "元数据"
                        }
                    }
                }
            }
        });

        // 性能监控索引
        db[collectionName].createIndex(
            { "userId": 1, "deviceId": 1, "timestamp": -1 },
            { name: "idx_user_device_time", background: true }
        );

        db[collectionName].createIndex(
            { "metricName": 1, "timestamp": -1 },
            { name: "idx_metric_time", background: true }
        );

        db[collectionName].createIndex(
            { "timestamp": -1 },
            { name: "idx_timestamp", background: true }
        );

        print(`创建客户端性能监控集合: ${collectionName}`);
    }
}

// 本地索引配置集合
db.createCollection("local_index_config", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["userId", "deviceId", "indexType"],
            properties: {
                userId: {
                    bsonType: "long",
                    description: "用户ID，必填"
                },
                deviceId: {
                    bsonType: "string",
                    description: "设备ID，必填"
                },
                indexType: {
                    bsonType: "string",
                    description: "索引类型：message/user/group/file，必填"
                },
                indexStrategy: {
                    bsonType: "int",
                    minimum: 1,
                    maximum: 3,
                    description: "索引策略：1-云端 2-本地 3-混合"
                },
                localCapacity: {
                    bsonType: "long",
                    description: "本地索引容量（字节）"
                },
                currentSize: {
                    bsonType: "long",
                    description: "当前索引大小（字节）"
                },
                cleanupPolicy: {
                    bsonType: "string",
                    description: "清理策略：7d/30d/90d"
                },
                offlineSearchRange: {
                    bsonType: "string",
                    description: "离线搜索范围"
                },
                isEnabled: {
                    bsonType: "bool",
                    description: "是否启用"
                },
                lastSyncAt: {
                    bsonType: "date",
                    description: "最后同步时间"
                },
                metadata: {
                    bsonType: "object",
                    description: "元数据"
                }
            }
        }
    }
});

// 本地索引配置索引
db.local_index_config.createIndex(
    { "userId": 1, "deviceId": 1, "indexType": 1 },
    { 
        name: "idx_user_device_type",
        unique: true,
        background: true 
    }
);

db.local_index_config.createIndex(
    { "indexType": 1, "isEnabled": 1 },
    { name: "idx_type_enabled", background: true }
);

// ============================================
// 5. 数据安全增强集合（新增）
// ============================================

// 数据分类配置集合
db.createCollection("data_classification", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["tableName", "columnName", "dataType", "sensitivityLevel"],
            properties: {
                tableName: {
                    bsonType: "string",
                    description: "表名，必填"
                },
                columnName: {
                    bsonType: "string",
                    description: "字段名，必填"
                },
                dataType: {
                    bsonType: "string",
                    description: "数据类型，必填"
                },
                sensitivityLevel: {
                    bsonType: "int",
                    minimum: 1,
                    maximum: 4,
                    description: "敏感级别：1-公开 2-内部 3-机密 4-绝密，必填"
                },
                classificationReason: {
                    bsonType: "string",
                    description: "分类原因"
                },
                retentionPolicy: {
                    bsonType: "string",
                    description: "保留策略"
                },
                encryptionRequired: {
                    bsonType: "bool",
                    description: "是否需要加密"
                },
                maskingRequired: {
                    bsonType: "bool",
                    description: "是否需要脱敏"
                },
                metadata: {
                    bsonType: "object",
                    description: "元数据"
                }
            }
        }
    }
});

// 数据分类索引
db.data_classification.createIndex(
    { "tableName": 1, "columnName": 1 },
    { 
        name: "idx_table_column",
        unique: true,
        background: true 
    }
);

db.data_classification.createIndex(
    { "sensitivityLevel": 1 },
    { name: "idx_sensitivity_level", background: true }
);

// 数据脱敏配置集合
db.createCollection("data_masking_config", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["tableName", "columnName", "maskingType"],
            properties: {
                tableName: {
                    bsonType: "string",
                    description: "表名，必填"
                },
                columnName: {
                    bsonType: "string",
                    description: "字段名，必填"
                },
                maskingType: {
                    bsonType: "string",
                    description: "脱敏类型：hash/partial/random/null，必填"
                },
                maskingRule: {
                    bsonType: "string",
                    description: "脱敏规则（JSON格式）"
                },
                maskingParams: {
                    bsonType: "object",
                    description: "脱敏参数"
                },
                isEnabled: {
                    bsonType: "bool",
                    description: "是否启用"
                },
                metadata: {
                    bsonType: "object",
                    description: "元数据"
                }
            }
        }
    }
});

// 数据脱敏配置索引
db.data_masking_config.createIndex(
    { "tableName": 1, "columnName": 1 },
    { 
        name: "idx_table_column",
        unique: true,
        background: true 
    }
);

db.data_masking_config.createIndex(
    { "maskingType": 1, "isEnabled": 1 },
    { name: "idx_type_enabled", background: true }
);

// ============================================
// 6. 数据访问审计集合（增强版）
// ============================================

// 数据访问审计集合（按时间分片）
for (let year = 2024; year <= 2026; year++) {
    for (let month = 1; month <= 12; month++) {
        const yearMonth = `${year}${month.toString().padStart(2, '0')}`;
        const collectionName = `data_access_audit_${yearMonth}`;
        
        db.createCollection(collectionName, {
            validator: {
                $jsonSchema: {
                    bsonType: "object",
                    required: ["auditId", "accessType", "timestamp"],
                    properties: {
                        auditId: {
                            bsonType: "string",
                            description: "审计唯一ID，必填"
                        },
                        userId: {
                            bsonType: "long",
                            description: "访问用户ID"
                        },
                        accessType: {
                            bsonType: "string",
                            description: "访问类型：read/write/export/delete，必填"
                        },
                        tableName: {
                            bsonType: "string",
                            description: "访问表名"
                        },
                        columnName: {
                            bsonType: "string",
                            description: "访问字段名"
                        },
                        recordId: {
                            bsonType: "string",
                            description: "记录ID"
                        },
                        accessResult: {
                            bsonType: "int",
                            minimum: 0,
                            maximum: 2,
                            description: "访问结果：0-拒绝 1-允许 2-部分允许"
                        },
                        accessReason: {
                            bsonType: "string",
                            description: "访问原因"
                        },
                        ipAddress: {
                            bsonType: "string",
                            description: "IP地址"
                        },
                        userAgent: {
                            bsonType: "string",
                            description: "用户代理"
                        },
                        deviceId: {
                            bsonType: "string",
                            description: "设备ID"
                        },
                        traceId: {
                            bsonType: "string",
                            description: "链路追踪ID"
                        },
                        timestamp: {
                            bsonType: "date",
                            description: "时间戳，必填"
                        },
                        metadata: {
                            bsonType: "object",
                            description: "元数据"
                        }
                    }
                }
            }
        });

        // 数据访问审计索引
        db[collectionName].createIndex(
            { "auditId": 1 },
            { 
                name: "idx_audit_id",
                unique: true,
                background: true 
            }
        );

        db[collectionName].createIndex(
            { "userId": 1, "accessType": 1, "timestamp": -1 },
            { name: "idx_user_access_time", background: true }
        );

        db[collectionName].createIndex(
            { "tableName": 1, "columnName": 1 },
            { name: "idx_table_column", background: true }
        );

        db[collectionName].createIndex(
            { "traceId": 1 },
            { name: "idx_trace_id", background: true }
        );

        db[collectionName].createIndex(
            { "timestamp": -1 },
            { name: "idx_timestamp", background: true }
        );

        print(`创建数据访问审计集合: ${collectionName}`);
    }
}

// ============================================
// 7. 初始化数据
// ============================================

// 插入数据分类配置
db.data_classification.insertMany([
    {
        tableName: "user",
        columnName: "password_hash",
        dataType: "hash",
        sensitivityLevel: 4,
        classificationReason: "用户密码哈希，绝密信息",
        retentionPolicy: "永久保留",
        encryptionRequired: true,
        maskingRequired: true
    },
    {
        tableName: "user",
        columnName: "phone",
        dataType: "string",
        sensitivityLevel: 3,
        classificationReason: "手机号码，机密信息",
        retentionPolicy: "7年",
        encryptionRequired: false,
        maskingRequired: true
    },
    {
        tableName: "user",
        columnName: "email",
        dataType: "string",
        sensitivityLevel: 3,
        classificationReason: "邮箱地址，机密信息",
        retentionPolicy: "7年",
        encryptionRequired: false,
        maskingRequired: true
    },
    {
        tableName: "conversation_member",
        columnName: "last_read_seq",
        dataType: "number",
        sensitivityLevel: 2,
        classificationReason: "阅读状态，内部信息",
        retentionPolicy: "1年",
        encryptionRequired: false,
        maskingRequired: false
    },
    {
        tableName: "media_file",
        columnName: "file_path",
        dataType: "string",
        sensitivityLevel: 2,
        classificationReason: "文件路径，内部信息",
        retentionPolicy: "3年",
        encryptionRequired: false,
        maskingRequired: false
    }
]);

// 插入数据脱敏配置
db.data_masking_config.insertMany([
    {
        tableName: "user",
        columnName: "phone",
        maskingType: "partial",
        maskingRule: '{"pattern": "\\d{3}\\*{4}\\d{4}", "visible": [0,1,2,7,8,9,10,11]}',
        maskingParams: { "prefix": 3, "suffix": 4 },
        isEnabled: true
    },
    {
        tableName: "user",
        columnName: "email",
        maskingType: "partial",
        maskingRule: '{"pattern": "\\w{1,3}\\*{1,10}@\\w{1,3}\\*{1,10}\\.\\w{2,4}", "visible": [0,1,2,3,4,5,6,7,8,9,10,11]}',
        maskingParams: { "prefix": 3, "suffix": 10 },
        isEnabled: true
    }
]);

// 插入本地索引默认配置
db.local_index_config.insertMany([
    {
        userId: 0,
        deviceId: "default",
        indexType: "message",
        indexStrategy: 3,
        localCapacity: 1073741824,
        currentSize: 0,
        cleanupPolicy: "30d",
        offlineSearchRange: "30d",
        isEnabled: true
    },
    {
        userId: 0,
        deviceId: "default",
        indexType: "user",
        indexStrategy: 1,
        localCapacity: 268435456,
        currentSize: 0,
        cleanupPolicy: "90d",
        offlineSearchRange: "7d",
        isEnabled: true
    },
    {
        userId: 0,
        deviceId: "default",
        indexType: "group",
        indexStrategy: 1,
        localCapacity: 268435456,
        currentSize: 0,
        cleanupPolicy: "90d",
        offlineSearchRange: "7d",
        isEnabled: true
    },
    {
        userId: 0,
        deviceId: "default",
        indexType: "file",
        indexStrategy: 2,
        localCapacity: 536870912,
        currentSize: 0,
        cleanupPolicy: "30d",
        offlineSearchRange: "30d",
        isEnabled: true
    }
]);

// ============================================
// 8. 创建数据清理任务
// ============================================

// 创建数据清理函数
function cleanupExpiredData() {
    const now = new Date();
    
    // 清理30天前的性能监控数据
    const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
    for (let year = 2024; year <= 2026; year++) {
        for (let month = 1; month <= 12; month++) {
            const yearMonth = `${year}${month.toString().padStart(2, '0')}`;
            const collectionName = `client_performance_${yearMonth}`;
            
            if (db.getCollectionNames().includes(collectionName)) {
                const result = db[collectionName].deleteMany({
                    timestamp: { $lt: thirtyDaysAgo }
                });
                print(`清理${collectionName}: 删除${result.deletedCount}条记录`);
            }
        }
    }
    
    // 清理90天前的在线状态历史
    const ninetyDaysAgo = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
    for (let year = 2024; year <= 2026; year++) {
        for (let month = 1; month <= 12; month++) {
            const yearMonth = `${year}${month.toString().padStart(2, '0')}`;
            const collectionName = `presence_history_${yearMonth}`;
            
            if (db.getCollectionNames().includes(collectionName)) {
                const result = db[collectionName].deleteMany({
                    timestamp: { $lt: ninetyDaysAgo }
                });
                print(`清理${collectionName}: 删除${result.deletedCount}条记录`);
            }
        }
    }
    
    // 清理365天前的审计日志
    const oneYearAgo = new Date(now.getTime() - 365 * 24 * 60 * 60 * 1000);
    for (let year = 2024; year <= 2026; year++) {
        for (let month = 1; month <= 12; month++) {
            const yearMonth = `${year}${month.toString().padStart(2, '0')}`;
            const collectionName = `data_access_audit_${yearMonth}`;
            
            if (db.getCollectionNames().includes(collectionName)) {
                const result = db[collectionName].deleteMany({
                    timestamp: { $lt: oneYearAgo }
                });
                print(`清理${collectionName}: 删除${result.deletedCount}条记录`);
            }
        }
    }
}

// 创建分区管理函数
function managePartitions() {
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;
    
    // 检查是否需要创建新的月份分区
    const yearMonth = `${currentYear}${currentMonth.toString().padStart(2, '0')}`;
    
    // 检查消息集合
    const messageCollectionName = `message_${yearMonth}`;
    if (!db.getCollectionNames().includes(messageCollectionName)) {
        print(`需要创建新的消息分区: ${messageCollectionName}`);
        // 这里可以调用创建集合的逻辑
    }
    
    // 检查性能监控集合
    const performanceCollectionName = `client_performance_${yearMonth}`;
    if (!db.getCollectionNames().includes(performanceCollectionName)) {
        print(`需要创建新的性能监控分区: ${performanceCollectionName}`);
    }
    
    // 检查在线状态历史集合
    const presenceCollectionName = `presence_history_${yearMonth}`;
    if (!db.getCollectionNames().includes(presenceCollectionName)) {
        print(`需要创建新的在线状态历史分区: ${presenceCollectionName}`);
    }
    
    // 检查审计日志集合
    const auditCollectionName = `data_access_audit_${yearMonth}`;
    if (!db.getCollectionName().includes(auditCollectionName)) {
        print(`需要创建新的审计日志分区: ${auditCollectionName}`);
    }
}

// ============================================
// 9. 性能优化配置
// ============================================

// 设置MongoDB性能参数
db.adminCommand({
    setParameter: 1,
    maxTransactionLockRequestTimeoutMillis: 5000
});

// 创建TTL索引用于自动清理
// 在线状态快照：7天过期
db.presence_snapshot.createIndex(
    { "lastSeenAt": 1 },
    { 
        name: "idx_ttl_last_seen",
        expireAfterSeconds: 7 * 24 * 60 * 60,
        background: true 
    }
);

// 本地索引配置：90天过期（如果未更新）
db.local_index_config.createIndex(
    { "updatedAt": 1 },
    { 
        name: "idx_ttl_updated",
        expireAfterSeconds: 90 * 24 * 60 * 60,
        background: true 
    }
);

// ============================================
// 10. 分片策略配置建议
// ============================================

/*
分片策略配置建议：

1. 消息集合分片策略：
   - 按时间分片：每年12个月，每月一个集合
   - 优势：时间范围查询性能好，便于归档
   - 劣势：可能存在热点月份

2. 时间线集合分片策略：
   - 按用户ID哈希分片：32个分片
   - 优势：用户数据分布均匀，查询性能稳定
   - 劣势：跨用户查询需要聚合

3. 性能监控集合分片策略：
   - 按时间分片：每年12个月
   - 优势：便于按时间范围分析，自动清理过期数据
   - 劣势：实时查询可能跨多个集合

4. 在线状态集合分片策略：
   - 历史数据按时间分片：每年12个月
   - 快照数据不分片：单集合存储
   - 优势：历史查询性能好，快照查询简单
   - 劣势：快照集合可能成为热点

5. 分片键选择建议：
   - 时间序列数据：使用时间字段作为分片键
   - 用户相关数据：使用用户ID作为分片键
   - 会话相关数据：使用会话ID作为分片键
*/

// ============================================
// 11. 读写分离配置建议
// ============================================

/*
读写分离配置建议：

1. 主库（写操作）：
   - 消息写入
   - 在线状态更新
   - 性能数据写入
   - 审计日志写入

2. 从库（读操作）：
   - 消息查询
   - 时间线查询
   - 统计报表查询
   - 搜索功能

3. 配置示例：
   - 主库：192.168.1.10:27017
   - 从库1：192.168.1.11:27017
   - 从库2：192.168.1.12:27017

4. 应用层配置：
   - 写操作：使用主库连接
   - 读操作：使用从库连接池
   - 事务操作：强制使用主库
*/

// ============================================
// 12. 监控和维护建议
// ============================================

/*
监控和维护建议：

1. 集合大小监控：
   - 定期检查各集合的大小
   - 监控分片集合的均衡性
   - 设置集合大小告警阈值

2. 索引性能监控：
   - 监控索引使用情况
   - 识别慢查询和缺失索引
   - 定期重建和优化索引

3. 数据清理策略：
   - 自动清理过期数据
   - 定期归档历史数据
   - 监控存储空间使用率

4. 性能优化：
   - 使用适当的读写关注级别
   - 批量操作优化
   - 连接池管理
*/

print('MongoDB消息数据库优化版初始化完成！');
print('包含双重分片策略、在线状态持久化、性能监控、数据安全等高级功能。');
print('建议定期运行 cleanupExpiredData() 和 managePartitions() 函数进行维护。'); 