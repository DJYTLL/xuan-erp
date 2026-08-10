---
title: "网关设计"
---

Gateway 负责统一入口、路由转发、认证入口、跨域、限流和 Trace 透传。

## 重启后运行态验证

修改 Gateway 路由、CORS、公开路径、安全审计、TraceId 或身份 Header 透传策略后，不能只刷新前端，也不能只看源码测试通过。必须重启 `xuan-gateway` 运行进程，再对当前监听端口做运行态验证。

Windows 本地验证：

```powershell
cd D:\xuan-erp\backend
.\scripts\verify-gateway-runtime.ps1 -GatewayBaseUrl http://127.0.0.1:8100 -FrontendOrigin http://127.0.0.1:5173
```

如果要同时验证真实登录后的 Tenant 受保护接口 `200`，先准备一个真实 access token：

```powershell
$env:XUAN_GATEWAY_VERIFY_ACCESS_TOKEN = "<真实登录拿到的 accessToken>"
.\scripts\verify-gateway-runtime.ps1
```

验证前端 Origin 经 Gateway 调 IAM 当前用户、当前菜单、当前权限快照，以及 Tenant 列权限裁剪接口：

```powershell
cd D:\xuan-erp\backend
$env:XUAN_GATEWAY_VERIFY_TENANT_CODE = "<租户编码>"
$env:XUAN_GATEWAY_VERIFY_USERNAME = "<用户名>"
$env:XUAN_GATEWAY_VERIFY_PASSWORD = "<密码>"
.\scripts\verify-gateway-authz-runtime.ps1 -GatewayBaseUrl http://127.0.0.1:8100 -FrontendOrigin http://127.0.0.1:5173
```

如果已经有 access token，也可以不传密码：

```powershell
$env:XUAN_GATEWAY_VERIFY_ACCESS_TOKEN = "<真实登录拿到的 accessToken>"
.\scripts\verify-gateway-authz-runtime.ps1
```

如果要验证 `403`，再准备一个没有对应权限的真实 access token：

```powershell
$env:XUAN_GATEWAY_VERIFY_FORBIDDEN_TOKEN = "<低权限用户 accessToken>"
.\scripts\verify-gateway-runtime.ps1
```

这组检查会确认：

- `/actuator/health` 可访问，证明打到的是运行中的 Gateway。
- `/api/iam/auth/refresh`、Swagger/OpenAPI、JWKS 等公开路径没有被 Gateway 误拦。
- 前端 Origin 的 CORS 预检返回允许头。
- Tenant 相关主路径不会经 Gateway 直接 `404`。
- 未登录访问受保护接口返回统一 `401` JSON。
- 外部伪造的 `X-User-Id`、`X-Tenant-Id`、`X-Permissions` 等身份 Header 不会绕过 Gateway 认证。
- Gateway 会生成或透传 `X-Trace-Id`，用于后续日志和审计串联。

## Sentinel Gateway 限流

`xuan-gateway` 作为统一入口，需要优先接入 Sentinel Gateway 规则，用于保护登录、列表查询、导出、打印、批量提交等高风险入口。

建议：

- Gateway 入口先做整体 QPS 和路由级限流。
- 业务服务再做关键接口、热点参数和服务间调用保护。
- Gateway 限流规则通过 Nacos 独立 dataId 管理，例如 `xuan-gateway-sentinel-gw-flow-rules.json`。
- 限流响应要统一格式，避免前端拿到不可解析的默认文本。

详细接入方式见：[Sentinel 准备](/guide/sentinel-setup/)。

## 待补充

- 路由规则。
- JWT 校验。
- 租户上下文透传。



