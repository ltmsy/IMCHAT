
# IM 系统数据模型交付
- 本文包含两部分可执行脚本：
  1) MySQL 完整建表 SQL（InnoDB, utf8mb4, 每字段注释、约束、示例初始数据）
  2) MongoDB 建库脚本（分片集合、索引、归档、TTL、示例文档）
- 约定
  - MySQL 版本：8.0+
  - MongoDB 版本：6.0+（已启用分片）
  - 所有对象名与字段名符合文档中对齐的业务语义
  - 可根据你的命名/域名/分片规模做微调

---

## 一、MySQL 建表 SQL（含字段注释与示例数据）

说明
- 使用 utf8mb4 与 utf8mb4_general_ci；生产建议 utf8mb4_0900_ai_ci
- 所有时间字段为 DATETIME(3) 存毫秒；默认 CURRENT_TIMESTAMP(3)
- 主键一律 BIGINT 无符号自增
- JSON 字段用于扩展；必要时加 CHECK 约束
- 按需添加分区或二级索引

可执行脚本

```sql
-- 建库
CREATE DATABASE IF NOT EXISTS im_business
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
USE im_business;

-- 统一表选项
SET NAMES utf8mb4;

-- 1) 用户表
DROP TABLE IF EXISTS user_account;
CREATE TABLE user_account (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  uid VARCHAR(64) NULL DEFAULT NULL COMMENT '外部可见UID/工号/学号（可选唯一）',
  nickname VARCHAR(64) NOT NULL COMMENT '昵称',
  avatar VARCHAR(512) NULL DEFAULT NULL COMMENT '头像URL',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：1正常 0停用 2注销',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  ext JSON NULL COMMENT '扩展字段：如标签、来源渠道等',
  PRIMARY KEY (id),
  UNIQUE KEY uk_uid (uid),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账户主表';

-- 2) 设备表（可选）
DROP TABLE IF EXISTS device;
CREATE TABLE device (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
  device_id VARCHAR(128) NOT NULL COMMENT '设备唯一标识（如硬件指纹/安装ID）',
  device_type TINYINT NOT NULL COMMENT '设备类型：1 iOS 2 Android 3 Web 4 Windows 5 macOS',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  last_seen_at DATETIME(3) NULL DEFAULT NULL COMMENT '最近活跃时间',
  ext JSON NULL COMMENT '扩展字段：系统版本、App版本等',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_device (user_id, device_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录设备与会话控制';

-- 3) 会话主表（单聊/群聊）
DROP TABLE IF EXISTS conversation;
CREATE TABLE conversation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  conversation_id VARCHAR(64) NOT NULL COMMENT '会话唯一ID（业务可读，如 c_xxx）',
  type TINYINT NOT NULL COMMENT '会话类型：1单聊 2群聊',
  last_msg_seq BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最新消息序号快照（以Mongo为准，异步回填）',
  last_msg_at DATETIME(3) NULL DEFAULT NULL COMMENT '最新消息时间快照（异步回填）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  ext JSON NULL COMMENT '扩展配置：置顶策略、显示风格等',
  PRIMARY KEY (id),
  UNIQUE KEY uk_conversation_id (conversation_id),
  KEY idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话主表';

-- 4) 群信息（仅群聊存在）
DROP TABLE IF EXISTS `group`;
CREATE TABLE `group` (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  conversation_id VARCHAR(64) NOT NULL COMMENT '关联会话ID（type=2）',
  name VARCHAR(128) NOT NULL COMMENT '群名称',
  avatar VARCHAR(512) NULL DEFAULT NULL COMMENT '群头像',
  notice TEXT NULL COMMENT '群公告',
  verify_join TINYINT NOT NULL DEFAULT 0 COMMENT '入群验证策略：0自由 1需要审核 2关闭加群',
  max_member INT NOT NULL DEFAULT 500 COMMENT '最大群成员数',
  created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  ext JSON NULL COMMENT '扩展字段：群级别、应用场景标签等',
  PRIMARY KEY (id),
  UNIQUE KEY uk_conv (conversation_id),
  KEY idx_creator (created_by),
  CONSTRAINT fk_group_conv FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_group_creator FOREIGN KEY (created_by) REFERENCES user_account(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群信息';

-- 5) 会话成员（含角色、既读、禁言）
DROP TABLE IF EXISTS conversation_member;
CREATE TABLE conversation_member (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '成员用户ID',
  role TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0成员 1管理员 2群主；单聊双方为0',
  mute_until DATETIME(3) NULL DEFAULT NULL COMMENT '禁言截止时间（为空表示未禁言）',
  join_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入时间',
  last_read_seq BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '既读序号（与Mongo消息seq对齐）',
  last_ack_at DATETIME(3) NULL DEFAULT NULL COMMENT '最近ACK/拉取时间',
  ext JSON NULL COMMENT '扩展：群名片、入群来源等',
  PRIMARY KEY (id),
  UNIQUE KEY uk_conv_user (conversation_id, user_id),
  KEY idx_user (user_id),
  KEY idx_conv_role (conversation_id, role),
  KEY idx_mute_until (mute_until),
  CONSTRAINT fk_cm_conv FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话成员关系与既读';

-- 6) 权限策略
DROP TABLE IF EXISTS permission_policy;
CREATE TABLE permission_policy (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  scope_type ENUM('conversation','group','global') NOT NULL COMMENT '作用域类型',
  scope_id VARCHAR(64) NULL DEFAULT NULL COMMENT '作用域ID（global时为空）',
  role TINYINT NOT NULL COMMENT '角色：与成员角色语义一致',
  action VARCHAR(64) NOT NULL COMMENT '动作：send_message, recall, at_all, invite_member 等',
  effect ENUM('allow','deny') NOT NULL COMMENT '裁决：允许/拒绝',
  constraints_json JSON NULL COMMENT '约束：频率/大小/类型白名单等',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_scope (scope_type, scope_id),
  KEY idx_role_action (role, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限策略定义';

-- 7) 禁言/黑名单
DROP TABLE IF EXISTS ban_blacklist;
CREATE TABLE ban_blacklist (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  scope_type ENUM('conversation','group','user') NOT NULL COMMENT '作用域类型',
  scope_id VARCHAR(64) NOT NULL COMMENT '作用域ID（user时可放发起人的用户ID）',
  target_user_id BIGINT UNSIGNED NOT NULL COMMENT '被限制的用户ID',
  ban_type ENUM('mute','block') NOT NULL COMMENT '类型：禁言/拉黑',
  reason VARCHAR(255) NULL DEFAULT NULL COMMENT '原因',
  operator BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '操作人用户ID',
  start_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
  end_at DATETIME(3) NULL DEFAULT NULL COMMENT '结束时间（空表示长期）',
  ext JSON NULL COMMENT '扩展：证据链接、来源等',
  PRIMARY KEY (id),
  KEY idx_scope_target (scope_type, scope_id, target_user_id),
  KEY idx_end_at (end_at),
  CONSTRAINT fk_bb_operator FOREIGN KEY (operator) REFERENCES user_account(id)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='禁言与黑名单';

-- 8) 配额计划
DROP TABLE IF EXISTS quota_plan;
CREATE TABLE quota_plan (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  plan_code VARCHAR(64) NOT NULL COMMENT '配额计划代码（唯一）',
  limits_json JSON NOT NULL COMMENT '配额限制：消息/图片/视频/文件/频率等',
  level INT NOT NULL DEFAULT 0 COMMENT '计划级别/排序',
  ext JSON NULL COMMENT '扩展字段',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_code (plan_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配额计划定义';

-- 9) 配额绑定
DROP TABLE IF EXISTS user_quota_binding;
CREATE TABLE user_quota_binding (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  subject_type ENUM('user','group') NOT NULL COMMENT '主体类型：用户或群',
  subject_id VARCHAR(64) NOT NULL COMMENT '主体ID（user用用户ID，group用conversation_id）',
  plan_code VARCHAR(64) NOT NULL COMMENT '绑定配额计划代码',
  start_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
  end_at DATETIME(3) NULL DEFAULT NULL COMMENT '结束时间（空为长期）',
  ext JSON NULL COMMENT '扩展：来源订单、备注等',
  PRIMARY KEY (id),
  UNIQUE KEY uk_subject_plan (subject_type, subject_id, plan_code),
  KEY idx_end_at (end_at),
  CONSTRAINT fk_uqb_plan FOREIGN KEY (plan_code) REFERENCES quota_plan(plan_code)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体绑定配额计划';

-- 10) 媒体策略
DROP TABLE IF EXISTS media_policy;
CREATE TABLE media_policy (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  scope_type ENUM('conversation','group','global') NOT NULL COMMENT '作用域类型',
  scope_id VARCHAR(64) NULL DEFAULT NULL COMMENT '作用域ID（global为空）',
  storage_class VARCHAR(32) NOT NULL DEFAULT 'standard' COMMENT '存储类别（standard, warm, cold）',
  max_size BIGINT UNSIGNED NOT NULL DEFAULT 104857600 COMMENT '最大大小（字节），默认100MB',
  virus_scan TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用病毒扫描：1是 0否',
  content_moderation TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用内容审核：1是 0否',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  ext JSON NULL COMMENT '扩展字段',
  PRIMARY KEY (id),
  KEY idx_scope (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒体存储与安全策略';

-- 11) 会话置顶/星标
DROP TABLE IF EXISTS session_pin_star;
CREATE TABLE session_pin_star (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1是 0否',
  is_starred TINYINT NOT NULL DEFAULT 0 COMMENT '是否星标：1是 0否',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_conv (user_id, conversation_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_sps_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_sps_conv FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话置顶与星标';

-- 12) 会话设置（免打扰、别名、折叠）
DROP TABLE IF EXISTS session_setting;
CREATE TABLE session_setting (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  mute_until DATETIME(3) NULL DEFAULT NULL COMMENT '免打扰截止时间',
  notification_level TINYINT NOT NULL DEFAULT 2 COMMENT '通知级别：0关闭 1仅@ 2全部',
  fold TINYINT NOT NULL DEFAULT 0 COMMENT '列表折叠：1是 0否',
  alias VARCHAR(64) NULL DEFAULT NULL COMMENT '该会话内为对方/群设置的别名',
  ext JSON NULL COMMENT '扩展：快捷筛选标签等',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_conv (user_id, conversation_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_ss_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_ss_conv FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户对会话的个性化设置';

-- 13) 审计日志
DROP TABLE IF EXISTS audit_log;
CREATE TABLE audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  actor_id BIGINT UNSIGNED NOT NULL COMMENT '操作者用户ID',
  action VARCHAR(64) NOT NULL COMMENT '动作名称',
  target_type VARCHAR(64) NOT NULL COMMENT '目标类型（conversation/group/user 等）',
  target_id VARCHAR(64) NOT NULL COMMENT '目标ID（可为会话ID/用户ID等）',
  detail JSON NULL COMMENT '详情JSON',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_actor_time (actor_id, created_at),
  KEY idx_target_time (target_type, target_id, created_at),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES user_account(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感/重要操作审计';

-- 示例初始数据
INSERT INTO user_account (uid, nickname, avatar, status) VALUES
('u_alice', 'Alice', NULL, 1),
('u_bob', 'Bob', NULL, 1),
('u_carla', 'Carla', NULL, 1);

-- 创建一个单聊会话 Alice-Bob
INSERT INTO conversation (conversation_id, type, ext)
VALUES ('c_alice_bob', 1, JSON_OBJECT('biz', 'p2p'));

-- 成员关系
INSERT INTO conversation_member (conversation_id, user_id, role)
SELECT 'c_alice_bob', id, 0 FROM user_account WHERE uid IN ('u_alice','u_bob');

-- 创建一个群聊
INSERT INTO conversation (conversation_id, type, ext)
VALUES ('c_team_alpha', 2, JSON_OBJECT('biz', 'group'));

INSERT INTO `group` (conversation_id, name, created_by, max_member)
VALUES ('c_team_alpha', 'Team Alpha', (SELECT id FROM user_account WHERE uid='u_alice'), 1000);

-- 群成员
INSERT INTO conversation_member (conversation_id, user_id, role)
SELECT 'c_team_alpha', id, CASE uid WHEN 'u_alice' THEN 2 ELSE 0 END
FROM user_account WHERE uid IN ('u_alice','u_bob','u_carla');

-- 权限策略示例：群主允许@all，一般成员禁止
INSERT INTO permission_policy (scope_type, scope_id, role, action, effect, constraints_json)
VALUES
('conversation', 'c_team_alpha', 2, 'at_all', 'allow', JSON_OBJECT()),
('conversation', 'c_team_alpha', 0, 'at_all', 'deny', JSON_OBJECT());

-- 配额计划与绑定
INSERT INTO quota_plan (plan_code, limits_json, level)
VALUES ('standard', JSON_OBJECT('msg_per_min', 60, 'image_max_mb', 20), 0);

INSERT INTO user_quota_binding (subject_type, subject_id, plan_code)
VALUES ('user', (SELECT CAST(id AS CHAR) FROM user_account WHERE uid='u_alice'), 'standard');

-- 会话设置样例
INSERT INTO session_setting (user_id, conversation_id, notification_level, fold)
VALUES ((SELECT id FROM user_account WHERE uid='u_bob'), 'c_team_alpha', 1, 0);
```

---

## 二、MongoDB 建库脚本（分片、索引、归档、TTL、示例文档）

说明
- 假设已部署分片集群并启用 sh.enableSharding
- 主集合 message 与 timeline 使用哈希分片：message 按 conversationId，timeline 按 userId
- 归档集合按月命名，示例以当前月与下一月说明
- TTL 用于归档集合自动过期；生产可按计划任务迁移后设置 TTL

可执行脚本（在 mongosh 中运行）

```javascript
// 基础设置
const dbName = "im_core";
const shards = 32; // 逻辑分片粒度由哈希分片自动决定，此值仅做命名参考
use admin;

// 启用数据库分片
sh.enableSharding(dbName);

// 切换数据库
use im_core;

/**
 * 1) 消息主集合：message
 * 分片键：conversationId 哈希
 */
if (!db.getCollectionNames().includes("message")) {
  db.createCollection("message", {
    validator: {
      $jsonSchema: {
        bsonType: "object",
        required: ["conversationId", "seq", "fromUserId", "type", "createdAt"],
        properties: {
          conversationId: { bsonType: "string", description: "会话ID" },
          seq: { bsonType: "long", description: "会话内递增序号" },
          clientMsgId: { bsonType: "string", description: "客户端幂等ID" },
          fromUserId: { bsonType: "long", description: "发送者用户ID" },
          type: { bsonType: "int", description: "消息类型：1文本 2图片 3视频 4文件 5系统" },
          content: { bsonType: ["object", "string", "null"], description: "小型payload或字符串" },
          blobRef: { bsonType: ["string", "null"], description: "对象存储引用" },
          meta: { bsonType: ["object", "null"], description: "引用/提及/撤回/编辑等元信息" },
          status: { bsonType: ["int", "null"], description: "状态：0初始 1可见 2撤回" },
          createdAt: { bsonType: "date", description: "服务端创建时间" },
          deletedFor: { bsonType: ["array", "null"], description: "针对部分用户隐藏" }
        }
      }
    }
  });
}
sh.shardCollection(`${dbName}.message`, { conversationId: "hashed" });

// 索引
db.message.createIndex({ conversationId: 1, seq: 1 }, { unique: true, name: "uk_conv_seq" });
db.message.createIndex({ conversationId: 1, createdAt: -1 }, { name: "idx_conv_time" });
db.message.createIndex({ clientMsgId: 1 }, { name: "idx_client_id", sparse: true });

/**
 * 2) 用户时间线：timeline
 * 分片键：userId 哈希
 */
if (!db.getCollectionNames().includes("timeline")) {
  db.createCollection("timeline", {
    validator: {
      $jsonSchema: {
        bsonType: "object",
        required: ["userId", "conversationId", "seq", "createdAt"],
        properties: {
          userId: { bsonType: "long", description: "用户ID" },
          conversationId: { bsonType: "string", description: "会话ID" },
          seq: { bsonType: "long", description: "消息序号引用" },
          msgRefId: { bsonType: ["objectId", "null"], description: "指向 message._id" },
          createdAt: { bsonType: "date", description: "写入时间" },
          flags: { bsonType: ["object", "null"], description: "置顶/未读/提醒等" }
        }
      }
    }
  });
}
sh.shardCollection(`${dbName}.timeline`, { userId: "hashed" });

// 索引
db.timeline.createIndex({ userId: 1, conversationId: 1, seq: 1 }, { name: "idx_user_conv_seq" });
db.timeline.createIndex({ userId: 1, createdAt: -1 }, { name: "idx_user_time" });

/**
 * 3) 幂等辅助（可选）：msg_idempotency
 */
if (!db.getCollectionNames().includes("msg_idempotency")) {
  db.createCollection("msg_idempotency");
}
db.msg_idempotency.createIndex({ clientMsgId: 1 }, { unique: true, name: "uk_clientMsgId" });

/**
 * 4) 归档集合（示例：按月）
 * 实际生产使用离线任务迁移 message -> message_archive_YYYYMM
 */
function ensureArchiveColl(yyyymm) {
  const collName = `message_archive_${yyyymm}`;
  if (!db.getCollectionNames().includes(collName)) {
    db.createCollection(collName);
    // TTL 索引：默认180天，按需调整；字段 archivedAt 需写入时赋值
    db[collName].createIndex(
      { archivedAt: 1 },
      { name: "ttl_archivedAt_180d", expireAfterSeconds: 180 * 24 * 3600 }
    );
    // 查询索引
    db[collName].createIndex({ conversationId: 1, seq: 1 }, { name: "idx_conv_seq" });
    db[collName].createIndex({ conversationId: 1, createdAt: -1 }, { name: "idx_conv_time" });
  }
  return collName;
}

const now = new Date();
const y = now.getUTCFullYear();
const m = (now.getUTCMonth() + 1).toString().padStart(2, '0');
const nextMonth = (now.getUTCMonth() + 2);
const y2 = y + Math.floor((nextMonth - 1) / 12);
const m2 = ((nextMonth - 1) % 12 + 1).toString().padStart(2, '0');

ensureArchiveColl(`${y}${m}`);
ensureArchiveColl(`${y2}${m2}`);

/**
 * 5) 示例插入文档
 */
// 清理示例
db.message.deleteMany({ conversationId: { $in: ["c_alice_bob", "c_team_alpha"] } });
db.timeline.deleteMany({ userId: { $in: [1,2,3] } }); // 仅示例（假设对应MySQL用户ID）

// 插入消息示例（P2P）
db.message.insertMany([
  {
    conversationId: "c_alice_bob",
    seq: NumberLong(1),
    clientMsgId: "client_abc_1",
    fromUserId: NumberLong(1),
    type: 1,
    content: { text: "Hi Bob!" },
    meta: { mentions: [] },
    status: 1,
    createdAt: new Date()
  },
  {
    conversationId: "c_alice_bob",
    seq: NumberLong(2),
    clientMsgId: "client_abc_2",
    fromUserId: NumberLong(2),
    type: 1,
    content: { text: "Hi Alice!" },
    meta: { replyTo: { seq: NumberLong(1), brief: "Hi Bob!" } },
    status: 1,
    createdAt: new Date()
  }
]);

// 时间线示例（Alice=1, Bob=2）
db.timeline.insertMany([
  { userId: NumberLong(1), conversationId: "c_alice_bob", seq: NumberLong(1), createdAt: new Date(), flags: { unread: false } },
  { userId: NumberLong(2), conversationId: "c_alice_bob", seq: NumberLong(1), createdAt: new Date(), flags: { unread: true } },
  { userId: NumberLong(1), conversationId: "c_alice_bob", seq: NumberLong(2), createdAt: new Date(), flags: { unread: true } },
  { userId: NumberLong(2), conversationId: "c_alice_bob", seq: NumberLong(2), createdAt: new Date(), flags: { unread: false } }
]);

// 大群消息示例
db.message.insertOne({
  conversationId: "c_team_alpha",
  seq: NumberLong(1),
  clientMsgId: "team_alpha_msg_1",
  fromUserId: NumberLong(1),
  type: 5,
  content: { code: "tip_only", params: { hint: "有新消息，进入会话加载" } },
  meta: { fanout: "light" },
  status: 1,
  createdAt: new Date()
});

// 查询示例：按会话分页拉取
// db.message.find({ conversationId: "c_alice_bob", seq: { $gt: NumberLong(0) } })
//   .sort({ seq: 1 })
//   .limit(50);
```

使用与运维建议
- message 集合写入走批量/有序写；客户端幂等用 clientMsgId 查重
- 序号分配建议在 Redis 原子自增，写成功后与 seq 建立唯一索引保证一致性
- 归档迁移由离线任务执行：按 createdAt 时间窗口迁移到归档集合，并设置 archivedAt
- 大群扇出按“提示+拉取”，timeline 可写轻量提示，正文在进入会话时按 seq 拉取

