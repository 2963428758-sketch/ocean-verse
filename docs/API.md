# OceanVerse API 文档

> 智慧海洋探索平台 · 后端接口规范 v1.0
>
> - **Base URL**: `http://localhost:8080`
> - **接口前缀**: 所有业务接口均以 `/api` 开头
> - **Swagger UI**: `http://localhost:8080/swagger-ui.html`
> - **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

## 目录

- [通用约定](#通用约定)
  - [统一响应结构](#统一响应结构)
  - [分页结构](#分页结构)
  - [认证方式](#认证方式)
  - [状态码](#状态码)
- [1. 用户认证模块 Auth](#1-用户认证模块-auth)
- [2. 海洋物种模块 Species](#2-海洋物种模块-species)
- [3. 观测记录模块 Observation](#3-观测记录模块-observation)
- [4. 数据可视化模块 Visual](#4-数据可视化模块-visual)
- [5. AI 智能服务模块 AI](#5-ai-智能服务模块-ai)
- [6. 社区互动模块 Community](#6-社区互动模块-community)

---

## 通用约定

### 统一响应结构

所有接口（除 AI 流式问答外）均返回统一的 `Result<T>` 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 状态码，`200` 表示成功 |
| `message` | string | 提示信息 |
| `data` | T | 业务数据，失败时为 `null` |

### 分页结构

分页接口使用 MyBatis-Plus 的 `Page<T>`，`data` 字段结构如下：

```json
{
  "records": [ ],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `records` | array | 当前页数据列表 |
| `total` | long | 总记录数 |
| `size` | long | 每页条数 |
| `current` | long | 当前页码 |
| `pages` | long | 总页数 |

### 认证方式

除登录、注册、公开查询接口外，所有需要身份鉴权的接口均要求在请求头携带 JWT：

```
Authorization: Bearer <token>
```

> 部分接口直接以 `@RequestHeader("Authorization")` 接收完整 token 字符串，建议统一使用 `Bearer` 前缀格式。

### 状态码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 / 业务校验失败 |
| 401 | 未认证或 token 失效 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

---

## 1. 用户认证模块 Auth

模块：`user-auth-manager`（成员 A） · 前缀：`/api/auth`

### 1.1 用户登录

`POST /api/auth/login`

**请求体** (`LoginDTO`)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**请求示例**

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应 data**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "username": "admin",
  "role": "ADMIN"
}
```

---

### 1.2 用户注册

`POST /api/auth/register`

**请求体** (`RegisterDTO`)

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| username | string | 是 | 长度 3–50 | 用户名 |
| password | string | 是 | 长度 6–50 | 密码 |
| email | string | 是 | 合法邮箱格式 | 邮箱 |
| phone | string | 否 | — | 手机号 |

**请求示例**

```json
{
  "username": "newuser",
  "password": "123456",
  "email": "newuser@ocean.com",
  "phone": "13800000000"
}
```

**响应**：`data` 为 `null`，成功即表示注册完成。

---

### 1.3 退出登录

`POST /api/auth/logout`

**请求头**

| 名称 | 必填 | 说明 |
|------|------|------|
| Authorization | 是 | JWT token |

**响应**：`data` 为 `null`。

---

### 1.4 获取当前用户信息

`GET /api/auth/info`

**请求头**

| 名称 | 必填 | 说明 |
|------|------|------|
| Authorization | 是 | JWT token |

**响应 data**

```json
{
  "userId": 1,
  "username": "admin",
  "realName": "管理员",
  "email": "admin@ocean.com",
  "phone": "13800000000",
  "avatarUrl": "https://...",
  "role": "ADMIN",
  "points": 1200
}
```

---

## 2. 海洋物种模块 Species

模块：`species-manager`（成员 B） · 前缀：`/api/species`

### 数据模型 `Species`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 主键 |
| speciesCode | string | 物种编码 |
| scientificName | string | 学名 |
| commonName | string | 通用名（英） |
| chineseName | string | 中文名 |
| protectionLevel | string | 保护等级：`1`-一级 / `2`-二级 / `3`-三级 |
| iucnStatus | string | IUCN 濒危等级（如 LC、NT、VU、EN、CR 等） |
| kingdom / phylum / className / orderName / family / genus / species | string | 分类学阶层（界门纲目科属种） |
| description | string | 简介 |
| morphology | string | 形态特征 |
| ecology | string | 生态习性 |
| videoUrl | string | 视频地址 |
| isEndemic | int | 是否特有种（0/1） |
| isInvasive | int | 是否入侵种（0/1） |
| dataQuality | string | 数据质量评级 |
| source | string | 数据来源 |
| createTime / updateTime | datetime | 时间戳 |

### 2.1 分页查询物种列表

`GET /api/species/list`

**查询参数** (`SpeciesQueryDTO`)

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| keyword | string | — | 关键词（学名/中英文名模糊匹配） |
| family | string | — | 科名 |
| iucnStatus | string | — | IUCN 等级 |
| protectionLevel | string | — | 保护等级 |
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

**请求示例**

```
GET /api/species/list?keyword=海豚&family=海豚科&page=1&size=10
```

**响应 data**：`Page<Species>`

---

### 2.2 获取物种详情

`GET /api/species/{id}`

**路径参数**

| 名称 | 类型 | 说明 |
|------|------|------|
| id | long | 物种 ID |

**响应 data**：`Species`

---

### 2.3 新增物种

`POST /api/species`

**请求体**：`Species`（不含 `id`、`createTime`、`updateTime`）

**响应**：`data` 为 `null`。

> 需管理员/研究员权限。

---

### 2.4 更新物种

`PUT /api/species/{id}`

**路径参数**：`id`（物种 ID）

**请求体**：`Species`（`id` 以路径参数为准，请求体内的 `id` 会被覆盖）

**响应**：`data` 为 `null`。

---

### 2.5 删除物种

`DELETE /api/species/{id}`

**路径参数**：`id`（物种 ID）

**响应**：`data` 为 `null`。逻辑删除。

---

### 2.6 物种统计

`GET /api/species/statistics`

**响应 data**：物种总量、按保护等级 / IUCN 等级的统计聚合（具体结构由 Service 实现）。

---

## 3. 观测记录模块 Observation

模块：`eco-observation-manager` · 前缀：`/api/observation`

### 数据模型 `Observation`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 主键 |
| observationCode | string | 观测编号 |
| observationType | string | 观测类型 |
| observationDate | date | 观测日期 |
| observationTime | time | 观测时间 |
| durationMinutes | int | 持续时长（分钟） |
| locationId | long | 地点 ID |
| ecosystemId | long | 生态系统 ID |
| latitude / longitude | decimal | 经纬度 |
| depth | decimal | 深度（m） |
| waterTemperature | decimal | 水温（℃） |
| salinity | decimal | 盐度 |
| ph | decimal | pH 值 |
| weatherCondition | string | 天气状况 |
| seaCondition | string | 海况 |
| observerId | long | 观测者 ID |
| observerName | string | 观测者姓名 |
| organization | string | 所属机构 |
| equipmentUsed | string | 使用设备 |
| notes | string | 备注 |

### 3.1 分页查询观测列表

`GET /api/observation/list`

**查询参数**

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

**响应 data**：`Page<Observation>`

---

### 3.2 获取观测详情

`GET /api/observation/{id}`

**响应 data**：`Observation`

---

### 3.3 新增观测

`POST /api/observation`

**请求体**：`Observation`（不含 `id`）

**响应**：`data` 为 `null`。

---

### 3.4 更新观测

`PUT /api/observation/{id}`

**请求体**：`Observation`（`id` 以路径参数为准）

**响应**：`data` 为 `null`。

---

### 3.5 删除观测

`DELETE /api/observation/{id}`

**响应**：`data` 为 `null`。逻辑删除。

---

## 4. 数据可视化模块 Visual

模块：`data-visual-analytics`（成员 C） · 前缀：`/api/visual`

> 该模块部分接口当前为占位实现（返回 "待实现"），下方为约定的数据结构。

### 4.1 仪表盘总览

`GET /api/visual/dashboard`

**响应 data**

```json
{
  "totalSpecies": 0,
  "totalObservations": 0,
  "totalUsers": 0,
  "totalRecognitions": 0
}
```

---

### 4.2 物种分布

`GET /api/visual/species/distribution`

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| speciesId | long | 否 | 指定物种 ID，不传则返回全部 |

**响应 data**：经纬度分布点列表（用于 Leaflet 地图渲染）。

---

### 4.3 观测趋势

`GET /api/visual/trend/observation`

**查询参数**

| 参数 | 类型 | 默认 | 可选值 | 说明 |
|------|------|------|--------|------|
| period | string | monthly | `daily` / `weekly` / `monthly` / `yearly` | 统计周期 |

**响应 data**：按时间维度聚合的观测数量序列。

---

### 4.4 按科统计物种

`GET /api/visual/statistics/family`

**响应 data**：各科（family）的物种数量分布。

---

### 4.5 按 IUCN 等级统计

`GET /api/visual/statistics/iucn`

**响应 data**：各 IUCN 保护等级（LC/NT/VU/EN/CR/EW/EX）的物种数量分布。

---

## 5. AI 智能服务模块 AI

模块：`smart-ai-service`（成员 B/E） · 前缀：`/api/ai`

### 5.1 图像识别（上传照片识别海洋生物）

`POST /api/ai/recognize`

**Content-Type**：`multipart/form-data`

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 待识别的图片 |
| latitude | double | 否 | 拍摄纬度 |
| longitude | double | 否 | 拍摄经度 |

> 单文件上限 20MB，单请求上限 50MB（见 `application.yml`）。

**请求示例（curl）**

```bash
curl -X POST http://localhost:8080/api/ai/recognize \
  -H "Authorization: Bearer <token>" \
  -F "file=@dolphin.jpg" \
  -F "latitude=22.3" \
  -F "longitude=114.1"
```

**响应 data**：识别结果，含预测物种、置信度、Top-N 候选等（结构对应 `ImageRecognition`）。

---

### 5.2 智能问答（SSE 流式返回）

`POST /api/ai/chat`

⚠️ 本接口**不**返回标准 `Result` 结构，而是以 **Server-Sent Events** 流式推送文本。

**响应头**

```
Content-Type: text/event-stream; charset=UTF-8
```

**请求体** (`ChatDTO`)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| question | string | 是 | 用户提问内容 |
| sessionId | string | 否 | 会话 ID，用于多轮对话上下文 |
| questionType | string | 否 | 问题分类（如物种、生态、保护等） |

**请求示例**

```json
{
  "question": "中华白海豚主要分布在哪些海域？",
  "sessionId": "abc123",
  "questionType": "species"
}
```

**流式响应**（逐 token 推送）

```
data:中华
data:白海豚
data:主要
...
```

---

### 5.3 获取识别历史

`GET /api/ai/recognition/history`

**查询参数**

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

**响应 data**：分页的 `ImageRecognition` 记录列表。

---

### 5.4 获取问答历史

`GET /api/ai/chat/history`

**查询参数**

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

**响应 data**：分页的 `QaHistory` 记录列表。

---

### 5.5 问答反馈

`POST /api/ai/chat/feedback/{id}`

对某条问答记录提交反馈（点赞/点踩），用于优化 AI 效果。

**路径参数**

| 名称 | 类型 | 说明 |
|------|------|------|
| id | long | 问答记录 ID |

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| feedback | int | 是 | 反馈类型，如 `1`-有用 / `0`-无用 |

**响应**：`data` 为 `null`。

---

## 6. 社区互动模块 Community

模块：`community-manager`（成员 D） · 前缀：`/api/community`

### 6.1 发布动态

`POST /api/community/post`

**请求头**：`Authorization`

**请求体** (`PostCreateDTO`)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | string | 是 | 动态正文 |
| postType | string | 否 | 类型：`NORMAL`-普通 / `OBSERVATION`-观测 / `RECOGNITION`-识别 |
| relatedSpeciesId | long | 否 | 关联物种 ID |
| relatedObservationId | long | 否 | 关联观测 ID |
| imageUrls | string | 否 | 图片 URL 列表（JSON 数组字符串） |

**请求示例**

```json
{
  "content": "今天在海边拍到了一只海豚！",
  "postType": "RECOGNITION",
  "relatedSpeciesId": 12,
  "imageUrls": "[\"https://cdn.ocean.com/1.jpg\",\"https://cdn.ocean.com/2.jpg\"]"
}
```

**响应**：`data` 为 `null`。

---

### 6.2 动态列表

`GET /api/community/post/list`

**查询参数** (`PostQueryDTO`)

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| postType | string | — | 动态类型筛选 |
| userId | long | — | 指定用户 |
| speciesId | long | — | 关联物种 |
| orderBy | string | — | 排序方式：`LATEST`-最新 / `HOT`-热门 |
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

**响应 data**：分页的动态列表（含作者信息、点赞/评论数等）。

---

### 6.3 动态详情

`GET /api/community/post/{id}`

**路径参数**：`id`（动态 ID）

**响应 data**：动态详情，含作者、图片、关联物种/观测、统计数等。

---

### 6.4 删除动态

`DELETE /api/community/post/{id}`

**请求头**：`Authorization`（仅作者或管理员可删）

**响应**：`data` 为 `null`。

---

### 6.5 发表评论

`POST /api/community/comment`

**请求头**：`Authorization`

**请求体** (`CommentCreateDTO`)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | long | 是 | 动态 ID |
| parentId | long | 否 | 父评论 ID，为空表示顶级评论 |
| content | string | 是 | 评论内容 |

**响应**：`data` 为 `null`。

---

### 6.6 评论列表

`GET /api/community/comment/list/{postId}`

**路径参数**：`postId`（动态 ID）

**查询参数**

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

**响应 data**：分页评论列表（含楼中楼结构）。

---

### 6.7 点赞 / 取消点赞

`POST /api/community/like/{targetType}/{targetId}`

对动态或评论进行点赞（再次调用则取消，为切换式）。

**请求头**：`Authorization`

**路径参数**

| 名称 | 类型 | 说明 |
|------|------|------|
| targetType | string | 目标类型：`POST`-动态 / `COMMENT`-评论 |
| targetId | long | 目标 ID |

**响应 data**

```json
{
  "liked": true,
  "likeCount": 36
}
```

---

### 6.8 收藏 / 取消收藏

`POST /api/community/favorite/{targetType}/{targetId}`

**请求头**：`Authorization`

**路径参数**

| 名称 | 类型 | 说明 |
|------|------|------|
| targetType | string | 目标类型：`POST`-动态 / `SPECIES`-物种 |
| targetId | long | 目标 ID |

**响应 data**

```json
{
  "favorited": true,
  "favoriteCount": 12
}
```

---

### 6.9 我的收藏列表

`GET /api/community/favorite/list`

**请求头**：`Authorization`

**查询参数**

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| targetType | string | — | 必填，目标类型 |
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

**响应 data**：分页的收藏列表。

---

### 6.10 排行榜

`GET /api/community/leaderboard`

**查询参数**

| 参数 | 类型 | 默认 | 可选值 | 说明 |
|------|------|------|--------|------|
| type | string | points | `points`-积分榜 / `posts`-活跃榜 / `recognitions`-识别榜 | 榜单类型 |

**响应 data**：用户排名列表。

---

## 附录：测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 超级管理员 |
| researcher | 123456 | 研究员 |
| observer | 123456 | 观测员 |

## 附录：接口与负责人映射

| 模块 | 前缀 | 负责人 |
|------|------|--------|
| 用户认证 | `/api/auth` | 成员 A |
| 海洋物种 | `/api/species` | 成员 B |
| 观测记录 | `/api/observation` | — |
| 数据可视化 | `/api/visual` | 成员 C |
| AI 服务 | `/api/ai` | 成员 B / E |
| 社区互动 | `/api/community` | 成员 D |
