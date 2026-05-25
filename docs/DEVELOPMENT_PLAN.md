# ContentGenius 开发计划（2026.5.26 — 2026.6.30）

> **上线目标**：2026 年 7 月 1 日前完成 MVP 并可对外演示。  
> **配置约定**：中间件、数据源、网关路由、LLM Key 等统一在 **Nacos** 配置，本地不写 `application.yml`。  
> **每日投入**：默认 **4～6 小时/天**（可按实际加减，表中「产出」为当日最低可验收标准）。

---

## 一、里程碑总览

| 阶段 | 日期 | 目标 |
|------|------|------|
| M1 基建 | 5/26 — 5/31 | Nacos 环境、Gateway、user-service 注册登录 + JWT |
| M2 内容 | 6/1 — 6/7 | content-service 项目/版本 CRUD、MinIO 上传 |
| M3 Agent 基础 | 6/8 — 6/10 | 模型集群路由、同步对话、存版本 |
| **M-Agent 亮点** | **6/11 — 6/25** | **流式 SSE、Web Search、RAG、全工具链、可观测**（详见 [AGENT_HIGHLIGHT_PLAN.md](AGENT_HIGHLIGHT_PLAN.md)） |
| M5 数据与付费 | 6/22 — 6/26 | analytics + payment（与 Agent 后段并行） |
| M6 前端与上线 | 6/27 — 6/30 | **流式聊天 UI**、联调、Docker 部署 |

---

## 二、模块与依赖（已就绪）

```
contentgenius-parent
├── contentgenius-common      # 公共 jar（JWT、DTO 等后续补充）
├── gateway-service           # Gateway + Nacos + Sentinel
├── user-service              # Security + MyBatis-Plus + Redis + Feign
├── content-service           # Web + MyBatis-Plus + MinIO + Redis + Feign
├── agent-service             # LangChain4j + Redis + RabbitMQ + Feign
├── analytics-service         # MyBatis-Plus + Redis + RabbitMQ + XXL-Job + Feign
└── payment-service           # MyBatis-Plus + Redis + Feign
```

**技术版本（父 POM 锁定）**：Spring Boot 3.2.5 · Spring Cloud 2023.0.3 · Spring Cloud Alibaba 2023.0.1.2 · LangChain4j 0.36.2 · MyBatis-Plus 3.5.7

**推荐包结构**（各服务按层自建，配置放 Nacos）：

```
com.contentgenius.{service}
├── config / controller / service / mapper / entity / dto / client
```

agent-service 额外：`core`、`model`（ModelRouter）、`tool`、`rag`、`memory`、`observability`、`mq`。  
**Agent 亮点专项**（流式 / 联网搜索 / 向量 RAG / 模型集群）见 **[AGENT_HIGHLIGHT_PLAN.md](AGENT_HIGHLIGHT_PLAN.md)**，建议占总工时 **40%～50%**。

---

## 三、每日任务明细

### 第 1 周：基建与用户（5/26 — 5/31）

| 日期 | 任务（做什么） | 产出 / 验收 |
|------|----------------|-------------|
| **5/26 周一** | 本地拉起 Nacos、MySQL、Redis（Docker 或本机）；在 Nacos 建 `dev` 命名空间；父工程 `mvn clean install` 通过 | 7 个模块编译成功；Nacos 控制台可访问 |
| **5/27 周二** | Nacos 配置 `gateway-service.yaml`（端口 8080、路由占位）；启动 gateway，确认注册到 Nacos | Gateway 启动无报错，Nacos 可见实例 |
| **5/28 周三** | user 库表 `user`；Nacos 配 `user-service` 数据源 + Redis；实体 + Mapper | 表结构落地，单测或控制台能查库 |
| **5/29 周四** | 注册、登录 API；BCrypt 密码；JWT 签发（common 模块补 JwtUtil） | Postman：注册→登录拿到 token |
| **5/30 周五** | Gateway 全局 JWT 过滤器（白名单：登录/注册）；路由 `/api/users/**` → user-service | 无 token 访问业务接口 401；带 token 200 |
| **5/31 周六** | Sentinel 网关限流规则（Nacos 或控制台）；member_level 字段预留；周复盘文档 | 限流触发有友好响应；**M1 完成** |

---

### 第 2 周：内容服务（6/1 — 6/7）

| 日期 | 任务 | 产出 / 验收 |
|------|------|-------------|
| **6/1 周日** | content 库：`project`、`content_version`；Nacos 配 content-service | 建表 SQL 归档到 `docs/sql/` |
| **6/2 周一** | 项目 CRUD：创建主题、列表、详情、软删除/状态 | 4 个 REST 接口通 |
| **6/3 周二** | 版本保存：每次生成写入 `content_version`，version_no 自增 | 同一 project 多版本可查 |
| **6/4 周三** | MinIO Nacos 配置；上传接口（封面/附件）；返回 URL | 文件可上传并在浏览器打开 |
| **6/5 周四** | Gateway 路由 `/api/content/**`；Feign：user 校验 token 中 userId | 经网关 CRUD 全流程 OK |
| **6/6 周五** | 模板表 `template` 只读接口（可先内置 2 条种子数据） | 模板列表 API |
| **6/7 周六** | 联调 user+content；修 Nacos 配置项；**M2 完成** | 端到端：登录→建项目→存版本 |

---

### 第 3 周：Agent 基础 + 流式（6/8 — 6/14）

> 日级 Agent 任务以 **[AGENT_HIGHLIGHT_PLAN.md](AGENT_HIGHLIGHT_PLAN.md)** 为准；下表为与主线的衔接摘要。

| 日期 | 任务 | 产出 / 验收 |
|------|------|-------------|
| **6/8 周日** | Nacos 多模型配置；`ModelRouter`（fast/quality/fallback） | 按任务类型命中不同模型 |
| **6/9 周一** | `ContentGeniusAgent` + `ArticleWriter`；`POST /api/agent/chat` | 主题→文章草稿 |
| **6/10 周二** | Feign 存 version；Gateway `/api/agent/**` | 经网关完整打通 |
| **6/11 周三** | **SSE 流式** `/api/agent/chat/stream` + Gateway 关缓冲 | 前端/curl 打字机效果 |
| **6/12 周四** | 流式降级与 stop；平台 Prompt（Nacos） | 断连安全；小红书/公众号风格可分 |
| **6/13 周五** | 免费额度 Redis 计数 | 超额 403 |
| **6/14 周六** | 阶段 B 收尾压测；**M3 完成** | 流式 P95 延迟记录 |

---

### 第 4 周：Agent 亮点 — 联网 + RAG + 全工具（6/15 — 6/21）

| 日期 | 任务 | 产出 / 验收 |
|------|------|-------------|
| **6/15 周日** | **Web Search**（Tavily 等）接入 `WebSearchTool` | 实时热点带引用摘要 |
| **6/16 周一** | `HotTopicSearcher` 改真实搜索 + Redis 缓存 | 非 mock 热点 |
| **6/17 周二** | **RAG**：Redis 向量库 + embedding 入库管道 | 检索用户历史文稿 |
| **6/18 周三** | RAG 注入 system prompt；模板向量（可选） | 生成稿明显贴近用户风格 |
| **6/19 周四** | 相似度去重提示；`OutlineGenerator` 多轮确认 | 与旧版过似时告警 |
| **6/20 周五** | 全 Tool 链 + Redis `ChatMemoryStore`；局部改稿 | 「第三段改短」稳定 |
| **6/21 周六** | RabbitMQ 长文异步 + 流式先大纲；**M-Agent 核心完成** | 见 AGENT_HIGHLIGHT 阶段 E |

---

### 第 4.5 周：Agent 打磨 + 业务并行（6/22 — 6/25）

| 日期 | 任务 | 产出 / 验收 |
|------|------|-------------|
| **6/22 周日** | analytics 发布录入（与 Agent 并行） | 见原 M5 |
| **6/23 周一** | 看板 API；`UserAnalyzer` 接真实数据 | Agent 给发布时间建议 |
| **6/24 周二** | Agent **可观测**：model/tools/tokens 链路日志 | 演示可展示调用链 |
| **6/25 周三** | payment 模拟积分；Agent 演示脚本定稿 | 简历可写 5 条技术亮点 |

---

### 第 5 周：收尾与付费（6/26）

| 日期 | 任务 | 产出 / 验收 |
|------|------|-------------|
| **6/26 周四** | XXL-Job 性能同步（可 mock）；订阅字段；全链路冒烟；**M5 完成** | 付费→额度增加；6 服务注册正常 |

---

### 第 6 周：前端与上线（6/27 — 6/30）

| 日期 | 任务 | 产出 / 验收 |
|------|------|-------------|
| **6/27 周五** | 前端：**EventSource 消费 SSE** + Markdown 编辑器；登录态 | 流式聊天体验可演示 |
| **6/28 周六** | 项目列表、版本历史、发布数据表单页 | 核心页面 3 个可用 |
| **6/29 周日** | 各服务 Dockerfile；docker-compose 仅中间件+镜像；云服务器部署 | 公网 IP 可访问 HTTPS（或 IP:端口演示） |
| **6/30 周一** | 冒烟测试清单；修 P0 Bug；写 README + 简历项目描述 | **MVP 上线就绪** |

---

## 四、7/1 前上线检查清单

- [ ] 6 个微服务均在 Nacos 注册，经 Gateway 访问
- [ ] 注册登录 + JWT 全链路
- [ ] 创建项目 → Agent 多轮生成 → 保存版本
- [ ] **SSE 流式对话**可演示（打字机效果）
- [ ] **ModelRouter** fast/quality/fallback 至少 2 档可用
- [ ] **Web Search** 返回实时热点（非 mock）
- [ ] **RAG** 能检索用户历史文稿并影响生成
- [ ] 至少 2 个平台风格 Prompt 可用
- [ ] 异步长文任务可轮询
- [ ] Agent 工具链/Token 调用可追踪（日志或 Redis）
- [ ] 发布链接 + 阅读量录入 + 简单看板
- [ ] 模拟支付/积分扣费
- [ ] 前端可演示完整创作者流程
- [ ] 敏感词过滤或「AI 辅助生成」标注（最低限度合规）

---

## 五、风险缓冲日

若进度落后，按优先级砍 scope（保证 7 月演示）：

1. **可砍**：XXL-Job、真实支付宝、ImagePrompt、Sentinel 精细规则、RAG 模板库、相似度去重  
2. **Agent 必留（亮点）**：**流式 SSE** + **ModelRouter** + **Web Search** + **基础 RAG（用户历史）** + 多轮记忆  
3. **业务必留**：Gateway + user + content + 前端流式聊天 + Docker 部署  

建议在 **6/20** 做 Agent 专项评审：若 RAG 未完成，至少保证 **流式 + 联网搜索 + 模型集群** 可演示。

---

## 六、Nacos 配置清单（自行维护，建议 Data ID）

| Data ID | 说明 |
|---------|------|
| `gateway-service.yaml` | 端口、路由、JWT 白名单 |
| `user-service.yaml` | 数据源、Redis、JWT secret |
| `content-service.yaml` | 数据源、MinIO |
| `agent-service.yaml` | 多模型集群、WebSearch、RAG Redis、流式开关、Redis、RabbitMQ |
| `analytics-service.yaml` | 数据源、定时任务 |
| `payment-service.yaml` | 数据源、mock/支付宝开关 |

---

## 七、参考命令

```bash
# 编译全部模块
mvn clean install -DskipTests

# 单独启动某服务（配置由 Nacos 拉取，需先配 bootstrap 或 spring.config.import）
mvn -pl user-service spring-boot:run
```

---

*文档版本：v1.1 · Agent 亮点见 [AGENT_HIGHLIGHT_PLAN.md](AGENT_HIGHLIGHT_PLAN.md)*
