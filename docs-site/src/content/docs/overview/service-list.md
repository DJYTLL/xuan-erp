---
title: "服务清单"
---

完整生产边界下，项目计划拆分为 14 个后端微服务和 1 个前端项目。

详细拆分见：[微服务拆分](/architecture/microservice-split/)。

当前首个后端最小闭环先落五个服务：

```text
xuan-gateway
xuan-iam
xuan-tenant
xuan-audit
xuan-product
```

其中 `xuan-audit` 从第一阶段开始接入，用于沉淀登录审计、操作审计、接口耗时、SQL 耗时和异常日志索引，避免第一个业务服务上线后缺少排查证据链。

后端服务的工程结构、接口契约、权限接入和数据库迁移规范见：[后端开发总览](/backend/)。



