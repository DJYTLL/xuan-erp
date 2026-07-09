# Tenant Provision Orchestration Design

## 背景

`xuan-tenant` 已经承担租户生命周期与初始化任务管理职责，并拥有 `tenant_provision_task`、`tenant_provision_task_step`、`tenant_outbox_event` 三类核心表。`xuan-iam` 已具备幂等的租户初始化能力，并在本地事务内发布 `IamTenantBootstrapped` 事件。

当租户初始化步骤扩展到多个微服务时，单纯由 `xuan-tenant` 串行同步调用每个服务会导致编排代码膨胀、失败恢复复杂、链路耦合过深。因此需要引入消息化编排，但又必须避免让下游服务直接订阅 `TenantCreated` 后自行推断初始化时机。

## 目标

- 明确租户初始化的编排归属：`xuan-tenant` 是总编排者。
- 使用 RocketMQ 承载“初始化步骤请求”和“初始化步骤结果”，而不是直接用生命周期事实事件驱动所有服务。
- 保留每个服务的本地自治：本地事务、本地 Outbox、本地幂等。
- 让初始化任务状态、重试、人工补偿都回到 `xuan-tenant` 统一视图中。

## 非目标

- 本次设计不覆盖 Saga 补偿事务的完整实现。
- 本次设计不要求所有业务服务一次性接入，只要求先支持 `xuan-iam` 作为首个标准步骤。
- 不修改现有 Flyway 历史，不直接定义数据库表变更脚本。

## 核心原则

### 1. 生命周期事件与编排事件分离

- `TenantCreated`、`TenantEnabled` 等事件是业务事实，只表达“已经发生了什么”。
- `TenantIamBootstrapRequested`、`TenantInventoryBootstrapRequested` 等事件是编排请求，只表达“现在请某服务执行某个初始化步骤”。

事实事件不能直接替代编排请求，否则下游服务会把“租户存在”误当成“该我初始化”。

### 2. xuan-tenant 负责总编排

`xuan-tenant` 负责：

- 创建 `tenant_provision_task`
- 创建 `tenant_provision_task_step`
- 决定步骤顺序、是否并行、是否跳过
- 发布步骤请求事件
- 订阅步骤完成/失败事件并更新任务状态
- 判断整个租户是否初始化完成

### 3. 各服务只负责执行自己的步骤

以 `xuan-iam` 为例：

- 订阅 `TenantIamBootstrapRequested`
- 按 `tenantId + stepKey + idempotencyKey` 做消费幂等
- 调用本地 `bootstrap_iam_tenant`
- 在本地事务内写 `IamTenantBootstrapped` Outbox
- 再发布标准化步骤结果事件，例如 `TenantIamProvisionStepCompleted` / `TenantIamProvisionStepFailed`

### 4. 结果事件回流到 xuan-tenant

`xuan-tenant` 订阅所有步骤结果事件，并按 `eventId` 做幂等更新：

- 某步骤成功 -> 更新对应 `tenant_provision_task_step`
- 某步骤失败 -> 记录错误码、错误信息、重试次数
- 所有必需步骤成功 -> 发布 `TenantProvisioned`

## 事件模型

## Topic 设计

- `xuan-tenant-event`
  - 生命周期事实事件
  - 编排步骤请求事件
  - 编排步骤聚合结果事件
- `xuan-iam-event`
  - IAM 自身事实事件，例如 `IamTenantBootstrapped`

可选做法是未来单独拆出 `xuan-tenant-provision-event`，但第一阶段保留在 `xuan-tenant-event` 可降低接入复杂度。

## 推荐事件

### 租户服务发布

- `TenantCreated`
- `TenantProvisioningStarted`
- `TenantIamBootstrapRequested`
- `TenantInventoryBootstrapRequested`
- `TenantDocumentBootstrapRequested`
- `TenantProvisionStepCompleted`
- `TenantProvisionStepFailed`
- `TenantProvisioned`

### IAM 服务发布

- `IamTenantBootstrapped`
- `TenantIamProvisionStepCompleted`
- `TenantIamProvisionStepFailed`

其中：

- `IamTenantBootstrapped` 是 IAM 自身领域事实事件
- `TenantIamProvisionStepCompleted` 是面向编排的结果事件

二者不能混成一个事件，否则会把服务内事实与跨服务编排耦在一起。

## 事件载荷最小规范

每个编排事件必须包含：

- `eventId`
- `traceId`
- `occurredAt`
- `sourceService`
- `tenantId`
- `provisionTaskId`
- `stepKey`
- `stepName`
- `idempotencyKey`

步骤请求事件额外包含：

- `requestedBy`
- `payload`
- `retryCount`
- `maxRetryCount`

步骤结果事件额外包含：

- `status`
- `resultPayload`
- `lastErrorCode`
- `lastErrorMessage`
- `finishedAt`

## 编排流程

### 首次租户初始化

1. `xuan-tenant` 创建租户主档，发布 `TenantCreated`
2. `xuan-tenant` 创建 `tenant_provision_task` 和若干 `tenant_provision_task_step`
3. `xuan-tenant` 发布 `TenantProvisioningStarted`
4. `xuan-tenant` 依次或并行发布步骤请求事件，例如 `TenantIamBootstrapRequested`
5. `xuan-iam` 消费请求，执行本地 bootstrap
6. `xuan-iam` 发布：
   - `IamTenantBootstrapped`
   - `TenantIamProvisionStepCompleted` 或 `TenantIamProvisionStepFailed`
7. `xuan-tenant` 消费步骤结果，更新 step 和 task
8. 全部必需步骤完成后，`xuan-tenant` 发布 `TenantProvisioned`

### 重试

- `xuan-tenant` 是唯一重试决策者
- 下游服务失败后只上报失败，不自己无限重发请求事件
- `xuan-tenant` 根据 `retryCount < maxRetryCount` 决定是否再次发送步骤请求
- 超过上限后保留在 `FAILED`，进入人工补偿

## 并行与顺序策略

建议把步骤分成三类：

- 严格前置步骤：必须先完成，如 IAM、默认配置
- 可并行步骤：库存、文档、查询投影初始化
- 收尾步骤：全部完成后才能发布 `TenantProvisioned`

可在 `tenant_provision_task_step.sequence_no` 上表达顺序层级：

- 相同 `sequence_no` 的步骤可并行
- 更大 `sequence_no` 的步骤必须等待前一层完成

## 幂等与一致性

### xuan-tenant

- `tenant_provision_task.idempotency_key` 保证整次初始化请求幂等
- `tenant_provision_task_step.idempotency_key` 保证单步骤请求幂等
- 消费结果事件按 `eventId` 幂等

### 下游服务

- 消费步骤请求按 `idempotencyKey` 幂等
- 本地初始化逻辑本身必须幂等
- 出站事件通过本地 Outbox 保证“本地事务成功后消息最终可达”

## 为什么不用“所有服务直接订阅 TenantCreated”

这种方式的问题是：

- 无法体现编排开始与步骤依赖关系
- 无法统一重试策略
- 无法控制哪些服务应该执行、哪些步骤要跳过
- 无法在 `xuan-tenant` 形成完整任务视图
- 会把“业务事实事件”污染成“隐式命令事件”

因此 `TenantCreated` 只能作为事实事件存在，不能承担编排触发职责。

## 第一阶段落地建议

第一阶段只接入 `xuan-iam`：

- `xuan-tenant` 新增 `TenantIamBootstrapRequested` 发布能力
- `xuan-iam` 新增对应 RocketMQ 消费者
- `xuan-iam` 在执行完成后发布 `TenantIamProvisionStepCompleted` / `TenantIamProvisionStepFailed`
- `xuan-tenant` 消费步骤结果并驱动 `TenantProvisioned`

等 IAM 跑通后，再复制模式接入库存、文档、查询等服务。

## 验证标准

- 同一 `tenantId + stepKey + idempotencyKey` 重复投递不产生重复初始化副作用
- 某个步骤失败后，`tenant_provision_task_step` 能看到失败原因与重试次数
- 所有必需步骤完成后才会发布 `TenantProvisioned`
- `xuan-tenant` 可以从任务表完整查看每个服务步骤的状态
- `xuan-iam` 本地 `IamTenantBootstrapped` 与编排结果事件都能稳定发布
