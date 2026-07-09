---
title: "xuan-tenant 事件文档"
---

本文记录 `xuan-tenant` 发布和订阅的领域事件。事件只表达已经发生的业务事实，不承载远程过程调用语义。

## 发布事件

| 事件 | Topic | 触发时机 | 主要消费者 | 说明 |
| --- | --- | --- | --- | --- |
| TenantCreated | `xuan-tenant-event` | 租户主档创建成功 | IAM、Audit、Query | 携带 `tenantId`、`code`、`normalizedCode`、初始状态 |
| TenantProvisioningStarted | `xuan-tenant-event` | `POST /api/tenants` 创建租户并落初始化任务后立即发布 | IAM、Query、Audit | 表示编排已异步启动；事件必须带 `provisionStep = IAM_BOOTSTRAP`，接口返回状态保持 `PROVISIONING` |
| TenantIamBootstrapRequested | `xuan-tenant-event` | 编排进入首个 IAM 初始化步骤时发布 | IAM、Audit | 首期只允许请求 `IAM_BOOTSTRAP` 步骤，驱动 IAM 完成租户菜单、角色、权限基础数据准备 |
| TenantProvisioned | `xuan-tenant-event` | 收到 `TenantIamProvisionStepCompleted` 成功回执并确认 `IAM_BOOTSTRAP` 完成后发布 | Gateway、IAM、业务服务 | `TenantIamProvisionStepCompleted` 是推进编排的唯一权威成功回执；首期只有 `IAM_BOOTSTRAP` 一个步骤，因此该事件表示当前编排链路全部完成，可进入 ENABLED 流程 |
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
| IAM | IamTenantBootstrapped | 记录 IAM 自己的领域事实，用于审计、读模型或跨域消费，不直接推进租户编排状态 | `eventId` |
| IAM | TenantIamProvisionStepCompleted | 记录 `IAM_BOOTSTRAP` 的编排成功回执，作为推进 `TenantProvisioned` 的唯一权威成功回执 | `eventId` |
| IAM | TenantIamProvisionStepFailed | 记录 `IAM_BOOTSTRAP` 的编排失败回执，触发重试或人工处理 | `eventId` |

## 事件要求

- 每个事件必须包含 `eventId`、`tenantId`、`occurredAt`、`traceId`、`sourceService`。
- 编排事件必须补充 `provisionStep`，首期固定为 `IAM_BOOTSTRAP`。
- `IamTenantBootstrapped` 是 IAM 自己的领域事实事件；`TenantIamProvisionStepCompleted` / `TenantIamProvisionStepFailed` 才是面向 `xuan-tenant` 的编排回执事件。
- 事件发布使用 Outbox Pattern，避免本地事务成功但消息丢失。
- 消费端必须按 `eventId` 做幂等。
- 失败事件进入重试和死信队列，并提供人工补偿入口。
- Outbox 表只使用普通索引和应用层锁字段 `lockedBy/lockedAt/lockExpiresAt` 协调并发发布，不依赖数据库唯一索引或数据库锁表达业务唯一性。
- 生命周期事件必须能对应到 `tenant_status_history` 中的一条状态变更记录。
- 初始化事件必须能对应到 `tenant_provision_task` 和 `tenant_provision_task_step` 中的任务或步骤记录。
- 联系人事件必须能对应到 `tenant_contact` 中的一条联系人记录，事件里只发布脱敏后的手机号或邮箱。
- 配置变更事件如果涉及 `isSensitive` 或 `isEncrypted` 配置，只能发布配置键、值类型和变更摘要，不发布明文配置值。
- Outbox 死信收敛必须记录 `lastErrorCode`、`firstFailedAt`、`deadLetterAt`，并提供人工重试入口。
