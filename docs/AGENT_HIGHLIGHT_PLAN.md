# Agent 亮点专项计划

> 本文档是 `DEVELOPMENT_PLAN.md` 的 Agent 深化附录。原计划在第三、四周仅覆盖「单轮生成 + mock 工具」，**未包含**流式输出、联网搜索、向量 RAG、模型集群等能力；本专项将其作为**简历与演示核心**，建议投入总工时的 **40%～50%**。

---

## 一、现状对比（原 plan vs 本专项）

| 能力 | 原 DEVELOPMENT_PLAN | 本专项目标 |
|------|---------------------|------------|
| 对话接口 | `POST /chat` 同步 JSON | **SSE 流式** + 同步兜底 |
| 热点/事实 | `HotTopicSearcher` mock | **Web Search API**（Tavily / 博查 / Serper 可插拔） |
| 上下文 | 仅 `MessageWindowChatMemory` | **RAG**：用户历史文稿 + 平台模板 + 热点摘要入库检索 |
| 模型 | 单一 OpenAI | **Model Router**：fast / quality / fallback 三档 + Nacos 热切换 |
| 长文 | RabbitMQ 异步（有） | 保留，并与流式「先吐大纲再吐正文」结合 |
| 可观测 | 无 | Token 用量、工具调用链、模型路由命中写入日志/Redis |

---

## 二、目标架构（文字）

```
用户消息
   ↓
SessionMemory (Redis) ←→ 摘要持久化 (MySQL, 可选)
   ↓
ContentGeniusAgent (ReAct + Tools)
   ├─ ModelRouter ──→ fast: gpt-4o-mini / qwen-turbo
   │                  quality: gpt-4o / deepseek-v3
   │                  fallback: 备用 key 或国产模型
   ├─ WebSearchTool (Tavily 等)     → 实时热点/新闻
   ├─ RagRetriever (Redis Vector) → 用户旧文、模板、选题库
   └─ 写作类 Tool (Outline / Writer / Style / Title)
   ↓
StreamingChatLanguageModel → SSE (/api/agent/chat/stream)
   ↓
content-service Feign 存 version
```

**包结构建议（agent-service）**

```
com.contentgenius.agent
├── core/           ContentGeniusAgent、AgentExecutor
├── config/         AgentConfig、ModelRouterConfig、RagConfig
├── model/          ModelRouter、ModelProfile、FallbackPolicy
├── tool/           @Tool 实现 + WebSearchTool 封装
├── rag/            EmbeddingStore、IngestionService、ContentRetriever
├── memory/         RedisChatMemoryStore、SessionService
├── controller/     ChatController（sync + stream）
├── mq/             长文异步
└── observability/  AgentTrace、TokenMeter
```

---

## 三、技术选型与 Nacos 配置项（只列 key，值你自填）

| 配置前缀 | 用途 |
|----------|------|
| `contentgenius.llm.models.fast` | 短对话、改标题、工具决策 |
| `contentgenius.llm.models.quality` | 长文正文、多平台改写 |
| `contentgenius.llm.models.fallback` | 主模型 429/5xx 时切换 |
| `contentgenius.llm.router.rules` | 按任务类型路由，如 `outline→fast`, `article→quality` |
| `contentgenius.websearch.provider` | `tavily` / `bocha` / `serper` |
| `contentgenius.websearch.api-key` | 搜索 API 密钥 |
| `contentgenius.rag.redis.host` | Redis Stack 向量索引 |
| `contentgenius.rag.top-k` | 默认 5 |
| `contentgenius.rag.min-score` | 相似度阈值 0.7 |
| `contentgenius.agent.stream.enabled` | 是否默认走 SSE |

**Maven 依赖（已加入 agent-service）**：`langchain4j-reactor`、`langchain4j-web-search-engine-tavily`、`langchain4j-embeddings-all-minilm-l6-v2-q`、`langchain4j-easy-rag`、`langchain4j-community-redis-spring`、`langchain4j-dashscope`（国产模型可选）。

---

## 四、分阶段实施（与主计划日历对齐）

### 阶段 A：模型集群与基础 Agent（6/8 — 6/10，3 天）

| 日 | 任务 | 验收 |
|----|------|------|
| 6/8 | `ModelProfile` + `ModelRouter`：从 Nacos 读取多模型列表；封装 `ChatLanguageModel` Bean | 单元测试：路由到 fast/quality |
| 6/9 | `ContentGeniusAgent` + 首版 `ArticleWriter`；同步 `/api/agent/chat` | 主题 → 文章草稿 |
| 6/10 | Fallback：主模型失败自动切 fallback；Feign 存 content_version | 模拟 429 时仍能返回 |

### 阶段 B：SSE 流式输出（6/11 — 6/13，3 天）

| 日 | 任务 | 验收 |
|----|------|------|
| 6/11 | `StreamingChatLanguageModel` + `TokenStream`；`GET/POST .../chat/stream` 返回 `text/event-stream` | curl 可见逐 token |
| 6/12 | Gateway 对 stream 路由关闭缓冲（`X-Accel-Buffering: no`）；前端 EventSource 消费 | 浏览器打字机效果 |
| 6/13 | 流式中断（用户 stop）、超时关闭连接；同步接口保留作降级 | 断连不泄漏线程 |

### 阶段 C：Web Search 联网（6/14 — 6/16，3 天）

| 日 | 任务 | 验收 |
|----|------|------|
| 6/14 | 接入 Tavily（或你选的 API）；`WebSearchTool` 封装为 @Tool | 问「最近某某热点」返回带 URL 摘要 |
| 6/15 | `HotTopicSearcher` 改为先 WebSearch 再 LLM 归纳；结果缓存 Redis 1h | 同关键词二次请求命中缓存 |
| 6/16 | 搜索配额与敏感词过滤；失败降级为「暂无实时数据，基于通用知识回答」 | 无 key 时服务仍可启动 |

### 阶段 D：向量 RAG（6/17 — 6/20，4 天）

| 日 | 任务 | 验收 |
|----|------|------|
| 6/17 | Redis Stack / Redis 向量索引；`EmbeddingStore` + 本地 embedding 模型 | 写入一条文档后可 similaritySearch |
| 6/18 | **入库管道**：用户 `content_version` 发布时异步 embedding；平台 `template` 入库 | 新稿生成前 retrieve top-k |
| 6/19 | `ContentRetriever` 注入 Agent system prompt：「参考以下历史风格片段…」 | 明确模仿用户过往语气 |
| 6/20 | 去重：新稿与历史稿 cosine > 0.92 提示「与第 N 版过于相似」 | 演示时能说出去重逻辑 |

### 阶段 E：多工具 ReAct + 记忆（6/21 — 6/23，3 天）

| 日 | 任务 | 验收 |
|----|------|------|
| 6/21 | 全量 Tool：Outline / Writer / Style / Title / UserAnalyzer | 标准流程：搜索→大纲→确认→正文→标题 |
| 6/22 | `MessageWindowChatMemory` + Redis `ChatMemoryStore`；sessionId 头传递 | 刷新后续聊 |
| 6/23 | RabbitMQ 长文：流式先返 outline，taskId 轮询全文；**M-Agent 核心完成** | 10s+ 不卡死 |

### 阶段 F：打磨与可观测（6/24 — 6/25，2 天）

| 日 | 任务 | 验收 |
|----|------|------|
| 6/24 | 每次请求记录：modelUsed、toolsCalled、tokens、latency → Redis/日志 | 排查一次对话完整链路 |
| 6/25 | 简历话术 + 演示脚本（3 分钟：流式写小红书 + 联网热点 + RAG 模仿风格） | 可复述技术亮点 |

---

## 五、接口约定（实现时参考）

| 接口 | 说明 |
|------|------|
| `POST /api/agent/chat` | 同步，body: `{ sessionId, message, platform, projectId }` |
| `POST /api/agent/chat/stream` | SSE 流式，同上 |
| `GET /api/agent/tasks/{taskId}` | 异步长文轮询 |
| `POST /api/agent/rag/ingest` | 手动触发某 project 版本入库（管理/调试） |

---

## 六、演示话术（面试/答辩）

1. **模型集群**：按任务类型路由 fast/quality，主模型失败自动 fallback，配置在 Nacos 热更新。  
2. **流式体验**：LangChain4j `TokenStream` + SSE，网关关闭缓冲，前端打字机。  
3. **联网增强**：ReAct 自主调用 Web Search，热点不再 mock。  
4. **RAG**：用户历史文稿向量检索，生成时注入风格片段，并做相似度去重。  
5. **工程化**：异步长文、Redis 会话、工具链路与 Token 可观测。

---

## 七、scope 裁剪（时间不够时）

| 优先级 | 保留 | 可延后 |
|--------|------|--------|
| P0 | 流式 + ModelRouter + ArticleWriter + 多轮记忆 | — |
| P1 | Web Search + 基础 RAG（仅用户历史） | 模板库向量 |
| P2 | 去重、Token 看板、DashScope 第二厂商 | easy-rag 全自动入库 |

---

*版本 v1.0 · 与 agent-service 依赖同步*
