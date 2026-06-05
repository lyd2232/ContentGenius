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
| `analytics-service` | 发布效果统计【MVP 不做，仅占位】 |

> 支付/订阅模块已移除（涉及支付需备案）；会员等级与额度仍在 `user-service` + Redis 计数。

## 前端（手绘风 UI）

```bash
cd frontend
npm install
npm run dev
```

见 [frontend/README.md](frontend/README.md)。浏览器 http://localhost:5173 ，API 代理到 Gateway 8080。

## 构建

```bash
mvn clean install -DskipTests
```

## 开发计划

- **总日程（唯一源）**：[docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md)（含 5 月基建、6 月业务、Agent 亮点、可扩展项标注）
- Agent 架构备查：[docs/AGENT_HIGHLIGHT_PLAN.md](docs/AGENT_HIGHLIGHT_PLAN.md)
- 认证深化（双 Token）：[docs/AUTH_TOKEN_PLAN.md](docs/AUTH_TOKEN_PLAN.md)

配置统一在 **Nacos** 维护，仓库内不提供 `application.yml` 模板。
