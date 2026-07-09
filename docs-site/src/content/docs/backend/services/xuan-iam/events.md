---
title: "xuan-iam 事件文档"
---

本文记录 `xuan-iam` 发布和订阅的领域事件。事件只表达已经发生的业务事实，不承载远程过程调用语义。

## 发布事件

| 事件 | Topic | 触发时机 | 主要消费者 | 说明 |
| --- | --- | --- | --- | --- |
| `IamTenantBootstrapped` | `iam.tenant.bootstrapped` | `bootstrap_iam_tenant` 首次完成租户菜单授权初始化 | 租户开通编排、审计、读模型 | IAM 自己的领域事实事件，描述“租户 IAM 基础能力已完成初始化”；通过 `iam_outbox_event.event_id = iam:tenant:{tenantId}:bootstrap:v1:event` 保持重复触发幂等 |
| `TenantIamProvisionStepCompleted` | `xuan-iam-event` | IAM 完成 `IAM_BOOTSTRAP` 步骤并准备向编排回写结果 | `xuan-tenant`、Audit | 面向 `xuan-tenant` 的编排成功回执，是推进 `TenantProvisioned` 的唯一权威成功回执；必须带 `provisionStep = IAM_BOOTSTRAP` |
| `TenantIamProvisionStepFailed` | `xuan-iam-event` | IAM 执行 `IAM_BOOTSTRAP` 失败 | `xuan-tenant`、Audit、告警服务 | 面向 `xuan-tenant` 的编排失败回执；必须带错误码、错误信息、重试次数和 `provisionStep = IAM_BOOTSTRAP` |
| UserCreated, RolePermissionChanged, PermissionCatalogSynced | `xuan-iam-event` | 用户、角色、权限目录和授权变更事件 | 按业务协作需要订阅 | 事件结构开发时补齐 |

## 订阅事件

| 来源服务 | 事件 | 处理目的 | 幂等键 |
| --- | --- | --- | --- |
| 按协作关系确定 | 服务权限清单同步请求、租户状态变更事件 | 构建本服务状态或读模型 | `eventId` |

## 事件要求

- 每个事件必须包含 `eventId`、`tenantId`、`occurredAt`、`traceId`、`sourceService`。
- 编排回执事件必须补充 `provisionStep`，首期固定为 `IAM_BOOTSTRAP`。
- `IamTenantBootstrapped` 与编排回执分层：前者是 IAM 领域事实，后者是 `TenantIamProvisionStepCompleted` / `TenantIamProvisionStepFailed` 两个面向 `xuan-tenant` 的编排回执。
- 事件发布使用 Outbox Pattern，避免本地事务成功但消息丢失。
- 消费端必须按 `eventId` 做幂等。
- 失败事件进入重试和死信队列，并提供人工补偿入口。
