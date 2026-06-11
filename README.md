# AI写作助手 | 个人自媒体技术经验分享
面向**自媒体创作者**的 AI 内容创作平台：从选题、写稿、改稿到版本管理与知识库检索，一站式完成多平台（小红书 / 公众号 / B站等）内容生产。
## 项目是干什么的
| 能力 | 说明 |
|------|------|
| **AI 写稿** | 快速 / 思考 / 智能路由三种模式；支持流式输出、多轮改稿 |
| **联网搜索** | Tavily 实时检索，成稿可附带参考链接 |
| **RAG 知识库** | 定稿写入 Qdrant 向量库，后续创作可检索历史风格 |
| **内容与版本** | 项目、稿件版本、MinIO 附件存储 |
| **用户与额度** | 注册登录、会员等级、每日创作次数（Redis 计数） |
| **意见反馈** | 内测意见箱，全员可见 |
典型流程：**登录 → 选项目 → 填主题与要求 → Agent 生成草稿 → 改稿 / 定稿 → 可选写入 RAG**。
## 后端技术栈
| 类别 | 技术 |
|------|------|
| 语言 / 运行时 | Java 17 |
| 核心框架 | Spring Boot 3.2、Spring Cloud 2023、Spring Security |
| 微服务治理 | Nacos（配置 + 注册发现）、Spring Cloud Gateway、OpenFeign |
| 数据层 | MySQL、MyBatis-Plus、Redis |
| 对象存储 | MinIO |
| AI 编排 | LangChain4j 1.15、通义千问（DashScope 兼容 API） |
| Agent 能力 | SSE 流式、Tavily 联网搜索、Qdrant 向量 RAG、多模型路由（fast / quality / fallback） |
| 其他 | JWT 鉴权、敏感词过滤、阿里云号码认证短信 |
## 模块
| 模块 | 说明 |
|------|------|
| `contentgenius-common` | 公共依赖（JWT、DTO、统一异常与 Result 等） |
| `gateway-service` | 网关、鉴权、限流 |
| `user-service` | 用户、会员、短信验证码、意见箱 |
| `content-service` | 项目与内容版本、MinIO 文件 |
| `agent-service` | LangChain4j Agent（写稿 / 改稿 / RAG / 联网） |
