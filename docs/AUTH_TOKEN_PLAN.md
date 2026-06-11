# 认证鉴权深化计划（双 Token / 无感刷新）

> 与 `DEVELOPMENT_PLAN.md` 第一周的关系：**5/26～5/28 不是登录**；**5/29～5/30 才是 MVP 级 JWT**（单 Token + 网关校验）。  
> 本文档描述如何在此基础上做 **Access + Refresh 双 Token、Redis 存储、前端拦截器无感刷新**——适合作为 **M1 完成后** 或 **前端联调前** 的加深项。

---

## 一、第一周到底在做什么？

| 日期 | 主题 | 是否「登录」 |
|------|------|----------------|
| 5/26 | Nacos、MySQL、Redis、编译 | 否，基建 |
| 5/27 | Gateway + Nacos 路由 | 否，网关 |
| 5/28 | 建库、实体、Mapper | 否，数据层 |
| **5/29** | 注册/登录 API、BCrypt、**签发 Access JWT** | **是（基础版）** |
| **5/30** | **Gateway JWT 过滤器**、白名单 | **是（网关鉴权）** |
| 5/31 | Sentinel、member_level | 否，治理/字段预留 |

**结论**：整周不是登录；登录鉴权主要集中在 **周四、周五 2 天**，且当前计划是 **够用、可演示的 MVP**，不是双 Token 完整方案。

---

## 二、MVP（按计划做）vs 深化（本文档）

| 能力 | MVP（5/29～5/30） | 深化（AUTH 专项） |
|------|-------------------|-------------------|
| 密码 | BCrypt 存库 | 同左 |
| 令牌 | **一个 Access Token**（如 2h 过期） | **Access 短 + Refresh 长** |
| 存储 | 前端 `localStorage` 或 `sessionStorage` 存 Access | Access 内存/SessionStorage；Refresh httpOnly Cookie 或安全存储 |
| 刷新 | 过期重新登录 | **`POST /api/users/refresh`** 无感换新 Access |
| 登出 | 前端删 Token 即可 | Redis **吊销 Refresh** + 可选 Access 黑名单 |
| 网关 | 校验 Access JWT | 同左；Refresh 接口走白名单 |
| 前端 | 手动带 `Authorization` | **Axios 拦截器** + 401 自动 refresh 队列 |

---

## 三、双 Token 架构（推荐）

```text
登录成功
  → 返回 accessToken（JSON，15min～2h）
  → 返回 refreshToken（httpOnly Cookie 或 JSON 由前端存安全位置，7～30 天）

业务请求
  → Header: Authorization: Bearer <accessToken>
  → Gateway / 服务校验 Access

Access 将过期 / 接口返回 401（code=TOKEN_EXPIRED）
  → 前端拦截器调用 /api/users/refresh（带 refreshToken）
  → user-service 校验 Refresh（Redis 是否存在、是否吊销）
  → 签发新 Access（可选旋转 Refresh）
  → 重放失败队列中的请求

登出
  → 删除 Redis 中 refresh:{userId}:{deviceId}
  → 清除 Cookie / 本地存储
```

---

## 四、后端要增的内容（user-service + common + gateway）

### 4.1 表 / Redis

**表 `refresh_token`（可选，简单 MVP 可只用 Redis）**

| 字段 | 说明 |
|------|------|
| id | 主键 |
| user_id | 用户 |
| token_hash | Refresh 哈希存库（不明文） |
| device_id | 设备标识（Web 可 UUID） |
| expire_at | 过期时间 |
| revoked | 是否吊销 |

**Redis 键（推荐）**

| Key | 说明 |
|-----|------|
| `refresh:{userId}:{deviceId}` | refreshToken 哈希或 jti，TTL = 7 天 |
| `access:blacklist:{jti}` | 登出后 Access 黑名单（可选，TTL = Access 剩余时间） |

### 4.2 API 清单

| 接口 | 说明 | 鉴权 |
|------|------|------|
| `POST /api/users/register` | 注册 | 放行 |
| `POST /api/users/login` | 登录，返回双 Token | 放行 |
| `POST /api/users/refresh` | 用 Refresh 换 Access | 放行（校验 Refresh 本身） |
| `POST /api/users/logout` | 吊销 Refresh | 需 Access 或 Refresh |
| `GET /api/users/me` | 当前用户信息 | 需 Access |

### 4.3 common 模块

- `JwtUtil`：`createAccessToken` / `createRefreshToken` / `parse` / 提取 `jti`、`userId`
- `TokenPair` DTO：`accessToken`、`expiresIn`、`tokenType`
- 常量：`TOKEN_EXPIRED`、`REFRESH_INVALID` 业务码（给前端判断）

### 4.4 Gateway 白名单（Nacos）

```yaml
# 示例，与 Jwt 过滤器配合
contentgenius:
  security:
    ignore-urls:
      - /api/users/register
      - /api/users/login
      - /api/users/refresh
```

### 4.5 安全要点（面试可讲）

- Refresh **只走 HTTPS**；Web 优先 **httpOnly + Secure Cookie**，防 XSS 偷 Refresh  
- **Refresh 旋转**：每次 refresh 发新 Refresh，旧 Refresh 从 Redis 删除，防重放  
- Access **短过期**；敏感操作可二次校验  
- 不在 JWT 里放密码、权限过大字段；`userId` + `username` 即可  

---

## 五、前端（6/27 前端周或提前）

| 项 | 做法 |
|----|------|
| 请求拦截器 | 每个请求自动加 `Authorization` |
| 响应拦截器 | `401` 且业务码 `TOKEN_EXPIRED` → 调 refresh，**单例锁** 避免并发刷 10 次 |
| 失败队列 | refresh 成功后重试原请求 |
| 存储 | Access：`sessionStorage`；Refresh：由后端 Set-Cookie（推荐）或封装在安全模块 |
| 路由守卫 | 无 Token 跳转登录页（前端路由） |

---

## 六、建议插入时间表（不拖 7/1 MVP）

在 **M1 完成（5/31）** 之后、**前端联调（6/27）** 之前选 2～3 天做深化即可：

| 阶段 | 建议时间 | 任务 | 验收 |
|------|----------|------|------|
| **Auth-MVP** | 5/29～5/30（原计划） | 单 Token + Gateway 过滤器 | Postman 注册登录拿 token |
| **Auth+** | 6/5～6/6 或 6/20 空档 | Refresh 接口 + Redis + 旋转 | Postman：refresh 拿到新 access |
| **Auth 前端** | 6/27 与聊天页一起 | Axios 拦截器无感刷新 | 过期后自动续期，用户无感 |

若时间紧：**7/1 仍用单 Token**；双 Token 写进简历「二期完成」。

---

## 七、和 Spring Security 的关系

- **5/29～5/30**：Security 管 **BCrypt、路径放行、（可选）Resource Server 解析 JWT**  
- **深化**：仍是 Security + 自研 JWT，**不是**必须上 OAuth2 Authorization Server  
- Refresh 接口是 **普通 Controller + Service**，校验逻辑在 Service 里查 Redis  

---

## 八、你问过的「会不会做到」

- 当前 `DEVELOPMENT_PLAN` **默认不包含** 双 Token / 前端拦截器。  
- 按本文档做，属于 **有意识的加深**，简历可从「会用 JWT」升到「双 Token + 无感刷新 + Redis 吊销」。  
- 后续你问到时，直接说「按 `docs/AUTH_TOKEN_PLAN.md` 第 x 阶段」即可接着实现。

---

*版本 v1.0 · 与第一周 M1 并行不冲突*
