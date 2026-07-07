---
title: "xuan-tenant"
---

xuan-tenant 是 Xuan ERP 的租户服务，主要负责：租户、租户启停、租户套餐、租户配置、租户初始化。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-tenant` |
| 职责 | 租户、租户启停、租户套餐、租户配置、租户初始化 |
| 权限前缀 | `tenant` |
| 数据库 | `xuan_tenant` |
| 事件 Topic | `xuan-tenant-event` |
| Java 包名 | `com.xuan.erp.tenant` |

## 限界上下文

租户生命周期上下文。它定义租户是否存在、是否可用、使用什么套餐和基础配置，但不保存业务授权关系。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | Tenant, TenantPlan, TenantConfig, TenantDomain, TenantContact | 本服务内部一致性边界 |
| 领域实体 | TenantPlanAssignment, TenantProvisionTask, TenantProvisionTaskStep, TenantStatusHistory, TenantOutboxEvent | 记录套餐分配、初始化流程、生命周期历史、联系人变更和待发布事件 |
| 值对象 | TenantId, TenantCode, TenantStatus, TenantDomainName, TenantConfigValueType | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | TenantLifecyclePolicy, TenantProvisionPolicy, TenantPlanPolicy, TenantDomainVerifyPolicy | 处理无法自然归属到单个实体的领域规则 |

租户当前状态保存在 `tenant`，状态变更历史写入 `tenant_status_history`。套餐当前绑定关系写入 `tenant_plan_assignment`，套餐定义写入 `tenant_plan`。域名写入 `tenant_domain`，联系人写入 `tenant_contact`。租户开通过程写入 `tenant_provision_task` 和 `tenant_provision_task_step`，跨服务事件先写入 `tenant_outbox_event`，再由发布器投递到消息系统；发布失败超过重试上限后进入 `DEAD_LETTERED` 状态，并记录失败码、首次失败时间和死信时间。

## 数据所有权

本服务连接并只直接读写独立数据库 `xuan_tenant` 中属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
