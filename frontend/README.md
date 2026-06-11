# AI写作助手 | 个人自媒体技术经验分享（前端）

墨绿 + 深灰工作台主题，Vue 3 + Vite，对接 Gateway `8080`。

Vue 3 + Vite，对接 Gateway `8080`。

## 启动

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173

## 前提

- Gateway、user-service、content-service、agent-service 已启动并注册 Nacos
- 已有可登录用户
- 开发期 Vite 已把 `/api` 代理到 `8080`；直连部署时需 Gateway CORS（已加 `GatewayCorsConfig`）

## 页面

| 路由 | 功能 |
|------|------|
| `/login` | 登录 |
| `/projects` | 项目列表、新建、删除 |
| `/projects/:id` | 版本历史 |
| `/create` | AI 创作（同步/流式）、Markdown 预览 |
| `/materials` | 图片上传、Vision 风格解析、删除 |

发布记录、数据看板未做（M5 已砍）。
