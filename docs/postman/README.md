# Postman 测 Agent 写稿（排查前端 / 后端）

## 导入

1. Postman → **Import** → 选 `ContentGenius-Agent.postman_collection.json`
2. 集合变量里改：
   - `baseUrl`：`http://localhost:8080`（Gateway）
   - `username` / `password`：你的账号
   - `projectId`：库里真实项目 id（可先调「我的项目列表」）

## 服务要先起来

- Gateway `8080`
- user-service、content-service、**agent-service**、Nacos、Redis、MySQL

---

## 推荐顺序（判断是谁的问题）

| 步骤 | 请求 | 预期 | 若失败说明 |
|------|------|------|------------|
| 1 | 登录获取 token | `code: 0`，`data.token` 有值 | 用户服务 / 网关 |
| 2 | **3 思考-同步首版** | 等 1–3 分钟，`code: 0`，`data.mode` = `"think"`，`data.content` 很长 | **后端编排或 LLM**；与前端无关 |
| 3 | **4 思考-同步改稿** | `code: 0`，正文变化；日志 `intent=STYLE` | **后端记忆 Redis**；与前端无关 |
| 4 | 浏览器同样操作 | 与 Postman 2、3 一致 | 若 Postman 成功、网页失败 → **前端** |

---

## 请求说明

### 公共 Header

```
Authorization: Bearer <token>
Content-Type: application/json
```

### POST `/api/agent/chat`（同步，思考模式必用）

**Body 示例（首版）：**

```json
{
  "projectId": 1,
  "topic": "夏日饮食健康指南",
  "platform": "xiaohongshu",
  "mode": "think",
  "isopen": false,
  "useRag": false,
  "memoryId": 1730000001
}
```

**成功响应结构：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "content": "……成稿正文……",
    "platform": "xiaohongshu",
    "versionId": 12,
    "versionNo": 3,
    "mode": "think",
    "memoryId": 1730000001
  }
}
```

**改稿 Body（第二步）：** 只改 `topic`，**`memoryId` 与首版相同**：

```json
{
  "projectId": 1,
  "topic": "语气更轻松一点，保留结构",
  "platform": "xiaohongshu",
  "mode": "think",
  "isopen": false,
  "useRag": false,
  "memoryId": 1730000001
}
```

### POST `/api/agent/chat/stream`（流式）

- Header 多加：`Accept: text/event-stream`
- 快速模式：`"mode": "fast"`
- 思考模式对比：`"mode": "think"`（应用新版 agent 后也应走四步）

Postman 流式响应为多行 `data: {...}`，最后一条 `versionId` 非空表示结束。

---

## 常见错误对照

| message | 含义 |
|---------|------|
| `今日免费额度已用完` | 会员等级 / 额度（user 表 memberLevel） |
| `多轮改稿需要 memoryId` | 改稿未带 memoryId |
| `未找到上一轮稿件` | 首版未写入 Redis，或 memoryId 与首版不一致 |
| `思考模式仅支持同步…` | 旧版 agent；或误用旧包 |
| 一直转圈后 504 | Gateway/代理超时；思考模式要 1–3 分钟 |

---

## curl 快速复制（Windows PowerShell）

先登录（把密码改成你的）：

```powershell
$body = '{"username":"admin01","password":"你的密码"}'
$r = Invoke-RestMethod -Uri "http://localhost:8080/api/users/login" -Method POST -ContentType "application/json" -Body $body
$token = $r.data.token
```

思考模式首版：

```powershell
$headers = @{ Authorization = "Bearer $token" }
$chat = @{
  projectId = 1
  topic = "夏日饮食健康指南"
  platform = "xiaohongshu"
  mode = "think"
  isopen = $false
  useRag = $false
  memoryId = 1730000001
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/agent/chat" -Method POST -Headers $headers -ContentType "application/json" -Body $chat
```

改稿（同一 memoryId）：

```powershell
$chat2 = @{
  projectId = 1
  topic = "语气更轻松一点"
  platform = "xiaohongshu"
  mode = "think"
  isopen = $false
  useRag = $false
  memoryId = 1730000001
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/agent/chat" -Method POST -Headers $headers -ContentType "application/json" -Body $chat2
```

---

## 结论怎么下

- **Postman 3、4 都成功** → 后端正常，查前端是否仍走 `/chat/stream`、是否没带 `memoryId`、是否没重启 Vite。
- **Postman 3 失败** → 后端 / LLM / 额度 / 存稿，看 agent-service 日志。
- **Postman 3 成功、4 失败** → 几乎一定是 **Redis 记忆** 或 **memoryId 不一致**。
