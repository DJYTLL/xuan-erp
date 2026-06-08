---
title: "xuan-document 接口文档"
---

本文记录 `xuan-document` 的前端接口、内部服务接口和管理接口。实际开发时，每个接口必须继续补齐请求参数、响应结构、错误码、幂等规则和审计要求。

## 前端接口清单

| 方法 | 路径 | 调用方 | 权限码 | 说明 |
| --- | --- | --- | --- | --- |
| GET | /api/document | Vue 管理端 | `document:view` | 列表查询 |
| GET | /api/document/{id} | Vue 管理端 | `document:view` | 详情查询 |
| POST | /api/document | Vue 管理端 | `document:create` | 创建业务数据 |
| PUT | /api/document/{id} | Vue 管理端 | `document:update` | 更新业务数据 |
| DELETE | /api/document/{id} | Vue 管理端 | `document:delete` | 删除或作废业务数据 |

## 内部接口

| 方法 | 路径 | 调用服务 | 说明 |
| --- | --- | --- | --- |
| GET | /internal/document/{id} | 其它后端服务 | 内部查询接口，按需开放 |
| POST | /internal/document/commands | 其它后端服务 | 内部命令接口，必须有服务间鉴权和幂等键 |

## 接口要求

- Controller 只做协议适配和 DTO 转换。
- 写接口必须说明幂等键、重复提交处理和事务边界。
- 需要租户上下文的接口必须校验 `tenantId`。
- 每个前端接口必须绑定权限码，super admin 只能旁路鉴权，不能省略权限定义。
- 内部接口必须使用服务间 token、来源服务白名单和 TraceId。
