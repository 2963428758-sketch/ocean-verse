# Redis Stream 消息队列 — 其他模块接入指南

## 概述

项目使用 Redis Stream 替代传统消息队列（如 RabbitMQ），实现模块间的异步事件通信。

各业务模块只需在关键操作完成后，调用 `RedisUtil.xAdd()` 向指定 Stream 发布一条消息，后续的消费者会自动处理通知推送、入库持久化等逻辑。

## 接入步骤

### 1. 注入 RedisUtil

你的模块已经依赖了 `oceanverse-common`，所以可以直接注入：

```java
import com.oceanverse.common.utils.RedisUtil;

@Service
@RequiredArgsConstructor
public class YourServiceImpl {
    private final RedisUtil redisUtil;
    // ...
}
```

### 2. 发布消息

在业务方法的关键位置调用 `redisUtil.xAdd()`：

```java
import static com.oceanverse.common.constants.CommonConstants.*;

// 示例：社区模块中，用户点赞后发布事件
Map<String, String> fields = new HashMap<>();
fields.put(FIELD_TYPE, ACTION_LIKE_TOGGLED);
fields.put(FIELD_PAYLOAD, "{\"actorId\":1,\"actorName\":\"researcher\",\"targetUserId\":2,\"targetId\":5,\"targetType\":\"POST\"}");
fields.put(FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));

redisUtil.xAdd(STREAM_COMMUNITY, fields, 10000);
```

**消息格式说明（所有 Stream 统一）：**

| 字段名 | 说明 | 示例值 |
|---|---|---|
| `type` | 事件动作类型（使用下方常量） | `COMMENT_CREATED` |
| `payload` | JSON 字符串，包含业务数据 | `{"actorId":1,...}` |
| `timestamp` | 毫秒时间戳 | `1718534400000` |

> 建议使用 `CommonConstants` 中的常量 `FIELD_TYPE`、`FIELD_PAYLOAD`、`FIELD_TIMESTAMP`，不要硬编码字段名。

---

## 3. 各 Stream 的 payload 字段规范

### stream:community（社区事件 — 成员D）

**发布时机**：发帖、删帖、评论、点赞、收藏操作完成后。

| payload 字段 | 类型 | 说明 | 哪些 action 需要 |
|---|---|---|---|
| `actorId` | Long | 操作者用户 ID | 全部 |
| `actorName` | String | 操作者用户名 | 全部 |
| `targetUserId` | Long | 被通知的目标用户 ID（如帖子作者） | COMMENT_CREATED, LIKE_TOGGLED |
| `targetId` | Long | 关联业务 ID（帖子 ID、评论 ID） | 全部 |
| `targetType` | String | `POST` 或 `COMMENT` | LIKE_TOGGLED, FAVORITE_TOGGLED |
| `content` | String | 内容摘要（如评论前50字） | COMMENT_CREATED |

**action 常量**：

| 常量名 | 值 | 发布时机 | 是否需要通知用户 |
|---|---|---|---|
| `ACTION_POST_CREATED` | `POST_CREATED` | `createPost()` 成功 | 否 |
| `ACTION_POST_DELETED` | `POST_DELETED` | `deletePost()` 成功 | 否 |
| `ACTION_COMMENT_CREATED` | `COMMENT_CREATED` | `createComment()` 成功 | 是 → 通知帖子作者 |
| `ACTION_LIKE_TOGGLED` | `LIKE_TOGGLED` | `toggleLike()` 点赞成功时 | 是 → 通知帖子/评论作者 |
| `ACTION_FAVORITE_TOGGLED` | `FAVORITE_TOGGLED` | `toggleFavorite()` 收藏成功时 | 否 |

**代码示例（成员D 参考）**：

```java
// === 在 CommunityServiceImpl.createComment() 末尾添加 ===
CommunityPost post = postMapper.selectById(dto.getPostId());
if (post != null && !post.getUserId().equals(userId)) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FIELD_TYPE, ACTION_COMMENT_CREATED);
    fields.put(FIELD_PAYLOAD, String.format(
        "{\"actorId\":%d,\"actorName\":\"%s\",\"targetUserId\":%d,\"targetId\":%d,\"content\":\"%s\"}",
        userId, username, post.getUserId(), dto.getPostId(),
        dto.getContent().length() > 50 ? dto.getContent().substring(0, 50) : dto.getContent()
    ));
    fields.put(FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
    redisUtil.xAdd(STREAM_COMMUNITY, fields, 10000);
}

// === 在 CommunityServiceImpl.toggleLike() 点赞成功分支中添加 ===
// 注意：只在点赞成功时发，取消点赞不发
Map<String, String> fields = new HashMap<>();
fields.put(FIELD_TYPE, ACTION_LIKE_TOGGLED);
fields.put(FIELD_PAYLOAD, String.format(
    "{\"actorId\":%d,\"actorName\":\"%s\",\"targetUserId\":%d,\"targetId\":%d,\"targetType\":\"%s\"}",
    userId, username, postAuthorId, targetId, targetType
));
fields.put(FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
redisUtil.xAdd(STREAM_COMMUNITY, fields, 10000);
```

---

### stream:ai（AI 事件 — 成员B）

**发布时机**：图像识别完成、问答会话结束时。

| payload 字段 | 类型 | 说明 | 哪些 action 需要 |
|---|---|---|---|
| `userId` | Long | 操作用户 ID | 全部 |
| `recognitionCode` | String | 识别记录编码 | RECOGNITION_COMPLETE |
| `speciesName` | String | 识别结果物种名 | RECOGNITION_COMPLETE |
| `confidence` | Double | 置信度 (0.0~1.0) | RECOGNITION_COMPLETE |
| `questionType` | String | 问答类型 | CHAT_SESSION |

**action 常量**：

| 常量名 | 值 | 发布时机 | 是否需要通知用户 |
|---|---|---|---|
| `ACTION_RECOGNITION_COMPLETE` | `RECOGNITION_COMPLETE` | AI 识别返回结果时 | 是 → 通知用户识别完成 |
| `ACTION_CHAT_SESSION` | `CHAT_SESSION` | 问答会话结束时 | 否 |
| `ACTION_FEEDBACK` | `FEEDBACK` | 用户提交反馈时 | 否 |

**代码示例（成员B 参考）**：

```java
// === 在 AiServiceImpl.recognizeImage() 接入真实 AI 后，识别完成时添加 ===
Map<String, String> fields = new HashMap<>();
fields.put(FIELD_TYPE, ACTION_RECOGNITION_COMPLETE);
fields.put(FIELD_PAYLOAD, String.format(
    "{\"userId\":%d,\"recognitionCode\":\"%s\",\"speciesName\":\"%s\",\"confidence\":%.2f}",
    userId, record.getRecognitionCode(), predictedSpeciesName, confidenceScore
));
fields.put(FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
redisUtil.xAdd(STREAM_AI, fields, 10000);
```

---

### stream:system（系统事件 — 成员A/任意模块）

**发布时机**：用户注册、缓存需刷新、定时任务触发时。

| payload 字段 | 类型 | 说明 |
|---|---|---|
| `targetKey` | String | 缓存 key 或目标标识 |
| `data` | String | 附加数据（可选） |

**action 常量**：

| 常量名 | 值 | 说明 |
|---|---|---|
| `ACTION_CACHE_REFRESH` | `CACHE_REFRESH` | 通知刷新指定缓存 |
| `ACTION_STATS_UPDATE` | `STATS_UPDATE` | 通知更新统计数据 |
| `ACTION_LOG_ARCHIVE` | `LOG_ARCHIVE` | 日志归档 |
| `ACTION_USER_REGISTER` | `USER_REGISTER` | 新用户注册 |

**代码示例（成员A 参考）**：

```java
// === 在 UserServiceImpl.register() 末尾添加 ===
Map<String, String> fields = new HashMap<>();
fields.put(FIELD_TYPE, ACTION_USER_REGISTER);
fields.put(FIELD_PAYLOAD, String.format(
    "{\"targetKey\":\"user:%d\",\"data\":\"%s\"}",
    newUserId, dto.getUsername()
));
fields.put(FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
redisUtil.xAdd(STREAM_SYSTEM, fields, 10000);
```

---

## 4. 注意事项

1. **不需要新增 Maven 依赖**。`RedisUtil` 在 `oceanverse-common` 中，你的模块已经依赖它了。
2. **只在操作成功后发布消息**。放在事务内、`mapper.insert/update` 之后即可。
3. **消息发布失败不应影响主业务**。`RedisUtil.xAdd()` 的异常会被 `RedisStreamService` 捕获并记录日志，但如果直接调用 `redisUtil.xAdd()`，建议用 try-catch 包裹。
4. **payload 中的 JSON 不要太长**。Stream 消息适合轻量级事件通知，大段数据应该通过 ID 关联查询。
5. **Stream 最大长度建议传 10000**。超过会自动修剪旧消息，避免内存无限增长。

## 5. 常量速查

所有常量定义在 `com.oceanverse.common.constants.CommonConstants` 中：

```java
// Stream Key
STREAM_NOTIFICATION = "stream:notification"
STREAM_COMMUNITY    = "stream:community"
STREAM_AI           = "stream:ai"
STREAM_SYSTEM       = "stream:system"

// 消息公共字段名
FIELD_TYPE      = "type"
FIELD_PAYLOAD   = "payload"
FIELD_TIMESTAMP = "timestamp"

// 通知类型
NOTIFY_LIKE      = "LIKE"
NOTIFY_COMMENT   = "COMMENT"
NOTIFY_SYSTEM    = "SYSTEM"
NOTIFY_AI_RESULT = "AI_RESULT"

// 社区事件动作
ACTION_POST_CREATED      = "POST_CREATED"
ACTION_POST_DELETED      = "POST_DELETED"
ACTION_COMMENT_CREATED   = "COMMENT_CREATED"
ACTION_LIKE_TOGGLED      = "LIKE_TOGGLED"
ACTION_FAVORITE_TOGGLED  = "FAVORITE_TOGGLED"

// AI 事件动作
ACTION_RECOGNITION_COMPLETE = "RECOGNITION_COMPLETE"
ACTION_CHAT_SESSION         = "CHAT_SESSION"
ACTION_FEEDBACK             = "FEEDBACK"

// 系统事件动作
ACTION_CACHE_REFRESH   = "CACHE_REFRESH"
ACTION_STATS_UPDATE    = "STATS_UPDATE"
ACTION_LOG_ARCHIVE     = "LOG_ARCHIVE"
ACTION_USER_REGISTER   = "USER_REGISTER"
```
