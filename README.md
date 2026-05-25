# ContentGenius

面向自媒体创作者的 AI 内容创作与分发平台（Spring Boot 3 微服务 + LangChain4j）。

## 模块

| 模块 | 说明 |
|------|------|
| `contentgenius-common` | 公共依赖（JWT、DTO 等） |
| `gateway-service` | 网关、鉴权、限流 |
| `user-service` | 用户与会员 |
| `content-service` | 项目与内容版本、MinIO |
| `agent-service` | LangChain4j Agent |
| `analytics-service` | 发布效果统计 |
| `payment-service` | 订阅与订单 |

## 构建

```bash
mvn clean install -DskipTests
```

## 开发计划

- 总计划：[docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md)（目标：2026 年 7 月 1 日前 MVP 上线）
- **Agent 亮点专项**：[docs/AGENT_HIGHLIGHT_PLAN.md](docs/AGENT_HIGHLIGHT_PLAN.md)（流式、Web Search、RAG、模型集群）

配置统一在 **Nacos** 维护，仓库内不提供 `application.yml` 模板。
