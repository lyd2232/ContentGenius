# Agent 写稿：Spring @AiService 说明

## 依赖

`agent-service/pom.xml` 增加 `langchain4j-spring-boot-starter`（版本由父 POM `langchain4j-bom` 1.15 管理）。

## 三个 AI 助手接口

| 接口 | `@AiService(chatModel=...)` | 用途 |
|------|---------------------------|------|
| `ArticleWriterQualityAssistant` | `qwenMaxChatModel` | 写长文主模型 |
| `ArticleWriterFallbackAssistant` | `qwenPlusChatModel` | 主模型失败备胎 |
| `ArticleWriterFastAssistant` | `qwenTurboChatModel` | 快速任务 |

`LLMConfig` 仍负责创建上述三个 `ChatModel` Bean；Spring 启动时为每个 `@AiService` 接口生成代理实现并注册为 Bean。

## 调用链

```text
POST /api/agent/chat
  → ContentGeniusAgent
  → PromptBuilder（system + user 文本）
  → ArticleWriter.write(ARTICLE, ...)
  → ArticleWriterQualityAssistant.write(...)  // Spring 生成的 @AiService 实现
  → qwenMaxChatModel.chat(...)
```

## 与手写 ChatMessage 的区别

- **之前**：`ArticleWriter` 内 `SystemMessage.from` + `ChatRequest.builder`
- **现在**：接口上 `@SystemMessage` / `@UserMessage` / `@V`，由 LangChain4j 组装同样结构的消息

业务逻辑（`prompt_hint`、topic、fallback）不变。
