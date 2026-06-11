# Gateway Sentinel 配置说明

## 0. Maven 依赖（gateway-service）

除 `spring-cloud-alibaba-sentinel-datasource` 外，必须再加：

```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>
```

否则启动报 `ClassNotFoundException: com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource`。

## 1. Nacos 两个配置（同一 dev 命名空间）

| Data ID | Group | 格式 | 作用 |
|---------|-------|------|------|
| `gateway-service-dev.yaml` | DEFAULT_GROUP | YAML | 路由、JWT、Sentinel 连接与 datasource |
| `gateway-service-gw-flow-rules` | SENTINEL_GROUP | JSON | 限流规则内容 |

示例文件见本目录：

- `gateway-service-dev.example.yaml` → 合并进你现有的 `gateway-service-dev.yaml`
- `gateway-service-gw-flow-rules.json` → 新建配置，原样粘贴

## 2. 控制台

- 确保已启动 **Sentinel Dashboard**（示例端口 `8858`）
- `transport.dashboard` 填控制台地址，不是 user-service 的 `8081`
- 启动 gateway 后，控制台「簇点链路」应出现 `gateway-service`

## 3. 验收

1. 重启 `gateway-service`
2. 快速多次请求 `POST http://localhost:8080/api/users/login`
3. 触发限流时返回 `429`，body：`{"code":429,"message":"请求过于频繁，请稍后再试"}`

调低 `gateway-service-gw-flow-rules.json` 里 `count`（如改为 `2`）便于测试。

## 4. resource 说明

`resource` 填 Gateway **路由 id**（如 `user-service`），与 `spring.cloud.gateway.routes[].id` 一致。
