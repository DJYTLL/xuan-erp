---
title: "网关设计"
---

Gateway 负责统一入口、路由转发、认证入口、跨域、限流和 Trace 透传。

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



