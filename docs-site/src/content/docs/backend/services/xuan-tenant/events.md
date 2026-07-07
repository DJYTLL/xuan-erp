---
title: "xuan-tenant 事件文档"
---

本文记录 `xuan-tenant` 发布和订阅的领域事件。事件只表达已经发生的业务事实，不承载远程过程调用语义。

## 发布事件

| 事件 | Topic | 触发时机 | 主要消费者 | 说明 |
| --- | --- | --- | --- | --- |
| TenantCreated | `xuan-tenant-event` | 租户主档创建成功 | IAM、Audit、Query | 携带 `tenantId`、`code`、`normalizedCode`、初始状态 |
| TenantProvisioningStarted | `xuan-tenant-event` | 租户初始化任务创建并开始执行 | IAM、业务服务、Query | 驱动跨服务初始化 |
| TenantProvisionStepCompleted | `xuan-tenant-event` | 单个初始化步骤完成 | Tenant、Audit、Query | 用于追踪初始化进度 |
| TenantProvisionStepFailed | `xuan-tenant-event` | 单个初始化步骤失败 | Tenant、Audit、告警服务 | 携带错误码、错误信息和重试次数 |
| TenantProvisioned | `xuan-tenant-event` | 租户全部初始化完成 | Gateway、IAM、业务服务 | 租户可进入 ENABLED 流程 |
| TenantEnabled | `xuan-tenant-event` | 租户启用或恢复启用 | Gateway、IAM、业务服务 | 触发缓存刷新和访问放行 |
| TenantSuspended | `xuan-tenant-event` | 租户因欠费、风控等原因暂停 | Gateway、IAM、业务服务 | 触发登录和业务访问拦截 |
| TenantDisabled | `xuan-tenant-event` | 租户被人工停用或关闭 | Gateway、IAM、业务服务 | 触发访问拦截和后台任务停止 |
| TenantPlanChanged | `xuan-tenant-event` | 租户套餐变更生效 | IAM、业务服务、Query | 携带 `previousPlanId`、`planId`、`assignedAt`、`assignedBy` |
| TenantDomainBound | `xuan-tenant-event` | 租户绑定新域名 | Gateway、Query | 触发域名路由缓存刷新 |
| TenantDomainVerified | `xuan-tenant-event` | 域名验证通过 | Gateway、Query | 触发入口生效 |
| TenantContactChanged | `xuan-tenant-event` | 租户联系人新增、修改、删除或主联系人调整 | Audit、Query | 携带联系人类型和脱敏联系方式 |
| TenantConfigChanged | `xuan-tenant-event` | 租户配置变更 | 相关业务服务、Query | 敏感配置事件不得携带明文值 |
| TenantOutboxDeadLettered | `xuan-tenant-event` | 租户服务 Outbox 事件超过最大重试次数 | Audit、告警服务 | 携带原始 `eventId`、`lastErrorCode`、`deadLetterAt` |

## 订阅事件

| 来源服务 | 事件 | 处理目的 | 幂等键 |
| --- | --- | --- | --- |
| IAM | TenantIamProvisionStepCompleted | 标记 IAM 初始化步骤完成 | `eventId` |
| IAM | TenantIamProvisionStepFailed | 标记 IAM 初始化步骤失败并触发重试或人工处理 | `eventId` |
| 业务服务 | TenantServiceProvisionStepCompleted | 标记指定业务服务初始化步骤完成 | `eventId` |
| 业务服务 | TenantServiceProvisionStepFailed | 标记指定业务服务初始化步骤失败 | `eventId` |

## 事件要求

- 每个事件必须包含 `eventId`、`tenantId`、`occurredAt`、`traceId`、`sourceService`。
- 事件发布使用 Outbox Pattern，避免本地事务成功但消息丢失。
- 消费端必须按 `eventId` 做幂等。
- 失败事件进入重试和死信队列，并提供人工补偿入口。
- Outbox 表只使用普通索引和应用层锁字段 `lockedBy/lockedAt/lockExpiresAt` 协调并发发布，不依赖数据库唯一索引或数据库锁表达业务唯一性。
- 生命周期事件必须能对应到 `tenant_status_history` 中的一条状态变更记录。
- 初始化事件必须能对应到 `tenant_provision_task` 和 `tenant_provision_task_step` 中的任务或步骤记录。
- 联系人事件必须能对应到 `tenant_contact` 中的一条联系人记录，事件里只发布脱敏后的手机号或邮箱。
- 配置变更事件如果涉及 `isSensitive` 或 `isEncrypted` 配置，只能发布配置键、值类型和变更摘要，不发布明文配置值。
- Outbox 死信收敛必须记录 `lastErrorCode`、`firstFailedAt`、`deadLetterAt`，并提供人工重试入口。
