---
title: "xuan-tenant 接口文档"
---

本文记录 `xuan-tenant` 的前端接口、内部服务接口和管理接口。实际开发时，每个接口必须继续补齐请求参数、响应结构、错误码、幂等规则和审计要求。

当前 `xuan-tenant` 沿用已落地的 V1 权限码：租户主资源使用 `tenant:view/create/update/delete`，启用和停用统一收敛到 `tenant:lifecycle`，配置写操作统一使用 `tenant-config:manage`。这组旧码受平台统一权限模板约束，后续如需拆分为更细粒度动作，必须走受控演进。

## 前端接口清单

| 方法 | 路径 | 调用方 | 权限码 | 说明 |
| --- | --- | --- | --- | --- |
| GET | /api/tenants | Vue 管理端 | `tenant:view` | 租户列表查询 |
| GET | /api/tenants/{id} | Vue 管理端 | `tenant:view` | 租户详情，包含当前套餐、主域名和生命周期摘要 |
| POST | /api/tenants | Vue 管理端 | `tenant:create` | 异步启动租户开通编排，创建成功后立即返回 `PROVISIONING`；首期只接 `IAM_BOOTSTRAP`，必须带幂等键 |
| PUT | /api/tenants/{id} | Vue 管理端 | `tenant:update` | 更新租户基础信息，编码规范化和重复校验由应用层完成 |
| POST | /api/tenants/{id}/enable | Vue 管理端 | `tenant:lifecycle` | 启用租户，写入生命周期历史并发布事件 |
| POST | /api/tenants/{id}/suspend | Vue 管理端 | `tenant:lifecycle` | 暂停租户，必须填写原因 |
| POST | /api/tenants/{id}/disable | Vue 管理端 | `tenant:lifecycle` | 停用租户，必须填写原因 |
| DELETE | /api/tenants/{id} | Vue 管理端 | `tenant:delete` | 逻辑删除租户，删除前应用层校验状态和关联关系 |
| GET | /api/tenant-plans | Vue 管理端 | `tenant-plan:view` | 套餐列表查询 |
| POST | /api/tenant-plans | Vue 管理端 | `tenant-plan:manage` | 新增套餐，套餐编码活动态唯一由应用层校验 |
| PUT | /api/tenant-plans/{id} | Vue 管理端 | `tenant-plan:manage` | 更新套餐定义、额度和功能开关 |
| POST | /api/tenants/{id}/plan-assignments | Vue 管理端 | `tenant-plan:assign` | 变更租户套餐，记录 `previousPlanId`、`assignedAt`、`assignedBy` 和变更原因 |
| GET | /api/tenants/{id}/domains | Vue 管理端 | `tenant-domain:view` | 查询租户域名 |
| POST | /api/tenants/{id}/domains | Vue 管理端 | `tenant-domain:manage` | 绑定域名，域名规范化和活动态唯一由应用层校验 |
| POST | /api/tenant-domains/{id}/verify | Vue 管理端 | `tenant-domain:manage` | 触发域名验证并记录验证结果 |
| GET | /api/tenants/{id}/contacts | Vue 管理端 | `tenant-contact:view` | 查询租户管理员、商务、技术、财务等联系人 |
| POST | /api/tenants/{id}/contacts | Vue 管理端 | `tenant-contact:manage` | 新增租户联系人，主联系人唯一由应用层校验 |
| PUT | /api/tenant-contacts/{id} | Vue 管理端 | `tenant-contact:manage` | 更新租户联系人 |
| DELETE | /api/tenant-contacts/{id} | Vue 管理端 | `tenant-contact:manage` | 逻辑删除租户联系人 |
| GET | /api/tenants/{id}/configs | Vue 管理端 | `tenant-config:view` | 查询租户配置，敏感配置必须脱敏 |
| PUT | /api/tenants/{id}/configs/{configKey} | Vue 管理端 | `tenant-config:manage` | 更新租户配置，应用层校验 valueType、敏感和加密标记 |
| GET | /api/tenants/{id}/provision-tasks | Vue 管理端 | `tenant-provision:view` | 查询租户初始化任务和步骤 |
| POST | /api/tenant-provision-tasks/{id}/retry | Vue 管理端 | `tenant-provision:manage` | 重试失败初始化任务或失败步骤 |
| POST | /api/tenant-outbox-events/{id}/retry | Vue 管理端 | `tenant-provision:manage` | 重试失败或死信 Outbox 事件，清理 `deadLetterAt` 后重新调度 |
| GET | /api/tenants/{id}/status-history | Vue 管理端 | `tenant:view` | 查询租户生命周期历史 |

## 内部接口

| 方法 | 路径 | 调用服务 | 说明 |
| --- | --- | --- | --- |
| GET | /internal/tenants/{id}/status | Gateway、IAM、业务服务 | 查询租户是否存在、是否启用、是否暂停 |
| GET | /internal/tenants/by-code/{code} | Gateway、IAM | 按规范化编码查询租户 |
| GET | /internal/tenants/by-domain/{domain} | Gateway | 按规范化域名解析租户入口 |
| GET | /internal/tenants/{id}/plan | IAM、业务服务 | 查询租户当前套餐和额度 |
| GET | /internal/tenants/{id}/configs | 业务服务 | 查询内部可读租户配置，敏感配置默认不返回明文 |
| POST | /internal/tenants/{id}/provision-callbacks | IAM、业务服务 | 回写初始化步骤结果，必须带事件 ID 或幂等键 |
| POST | /internal/tenant-outbox/publish-result | 消息发布器 | 回写 Outbox 发布成功、失败、重试和锁状态 |

## 接口要求

- Controller 只做协议适配和 DTO 转换。
- Controller 必须补 `@PreAuthorize`，并与文档中的权限码保持一致。
- 写接口必须说明幂等键、重复提交处理和事务边界。
- `POST /api/tenants` 是异步启动入口：创建租户主档和初始化任务成功后立即返回 `PROVISIONING`，不等待 IAM 实际完成。
- `POST /api/tenants` 只要求 `tenant:create`，它负责“创建租户并异步启动首期编排”；查看任务、步骤和失败原因使用 `tenant-provision:view`，重试、死信和人工补偿使用 `tenant-provision:manage`。
- 当前编排首期只接 `IAM_BOOTSTRAP`，后续如扩展更多步骤必须先更新事件契约、权限说明和回调接口文档。
- 需要租户上下文的接口必须校验 `tenantId`。
- 每个前端接口必须绑定权限码，super admin 只能旁路鉴权，不能省略权限定义。
- 内部接口必须使用服务间 token、来源服务白名单和 TraceId。
- 数据库不使用外键、唯一索引、CHECK 或 `ON CONFLICT`；所有引用有效性、活动态唯一性、状态枚举和字段关系都在应用层校验。
- `tenant.code`、`tenant_domain.domain` 保存原始输入，同时写入规范化字段，重复判断以规范化字段为准。
- `tenant_config.is_sensitive = true` 时不得作为公开配置返回；`is_encrypted = true` 时应用层负责加解密和脱敏。
- `tenant_contact` 的主联系人、联系人类型和联系方式完整性由应用层校验；数据库只保留普通索引辅助查询。
- Outbox 发布器抢占事件时只更新应用层锁字段 `lockedBy/lockedAt/lockExpiresAt`，不得依赖数据库唯一约束实现并发控制。
- Outbox 发布失败时记录 `lastErrorCode/lastErrorMessage/firstFailedAt`；超过最大重试次数后写入 `deadLetterAt` 并进入 `DEAD_LETTERED`，人工重试时由应用层重新调度。
