# 数据库脚本说明

库名需与 Nacos 里各服务 `datasource.url` 一致（默认如下）。

| 库名 | 服务 |
|------|------|
| `contentgenius_user` | user-service |
| `contentgenius_content` | content-service |

## 报错 1044 Access denied … to database 'contentgenius_user'

云数据库上的 `root@%` **常常不能 `CREATE DATABASE`**，不是密码错。

1. 用**控制台主账号**或 DBA 执行 [`00-admin-create-databases.sql`](00-admin-create-databases.sql) 建库（或在面板点「创建数据库」）。
2. 给 Nacos 里配置的 `datasource.username` **授权**这两个库（`GRANT ALL ON contentgenius_user.*` 等）。
3. 再用应用账号执行 [`00-init-tables-only.sql`](00-init-tables-only.sql)（脚本里**不含** `CREATE DATABASE`）。

## 数据丢了：一键恢复（本机 MySQL / root 有建库权时）

```bash
mysql -h<主机> -u<用户> -p < docs/sql/00-init-full.sql
```

会 **DROP 再建** 两库下所有业务表，并灌入权限、测试用户、模板、演示项目。

## 分步执行（保留已有数据时用）

| 顺序 | 文件 | 说明 |
|------|------|------|
| 1 | `01-user-db.sql` | 建 user 库与 RBAC 表 |
| 2 | `02-user-seed.sql` | 用户/权限种子（`ON DUPLICATE KEY UPDATE`） |
| 3 | `03-content-db.sql` | 建 content 库与三张表 |
| 4 | `04-content-seed.sql` | 平台模板 `template` |
| 5 | `05-content-demo-seed.sql` | 可选：演示 `project` / `content_version` |

## 测试账号

| 用户名 | 密码 | 档位 | 说明 |
|--------|------|------|------|
| test01 | 123456 | 免费 | 额外有 `content:write`，适合联调 |
| free01 | 123456 | 免费 | 只读内容 + agent |
| vip01 | 123456 | VIP | 可写内容 |
| admin01 | 123456 | 管理员 | 含 `admin:manage` |

## 与 agent 联调

- 只测大模型、**content 可不开**：agent 拉模板失败会用 `PromptBuilder` 内置默认。
- 要测 `prompt_hint` 从库读出：执行 content 脚本并启动 content-service。

登录示例：

```http
POST /api/users/login
Content-Type: application/json

{"username":"test01","password":"123456"}
```
