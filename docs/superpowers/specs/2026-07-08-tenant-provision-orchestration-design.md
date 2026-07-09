# Tenant Provision Orchestration Design

## 背景

`xuan-tenant` 已经承担租户生命周期与初始化任务管理职责，并拥有 `tenant_provision_task`、`tenant_provision_task_step`、`tenant_outbox_event` 三类核心表。`xuan-iam` 已具备幂等的租户初始化能力，并在本地事务内发布 `IamTenantBootstrapped` 事件。

当前仓库已经具备“任务表 + 步骤表 + Outbox 表 + 示例 MQ Producer/Consumer + 事件文档”的基础，但真正的初始化编排还没有在业务代码中闭环：`POST /api/tenants` 还没有把“创建租户”与“启动初始化任务”统一建模，`xuan-iam` 也还没有作为标准步骤执行者接入请求事件与结果回流。

## 目标

- 明确租户初始化的编排归属：`xuan-tenant` 是总编排者。
- 把 `POST /api/tenants` 收敛为异步启动入口：创建成功后立即返回 `PROVISIONING`，不等待下游服务完成。
- 建立可扩展的“总任务 + 多步骤 + 结果回流 + 汇总发布”模型，第一期先真实落地 `IAM_BOOTSTRAP`。
- 让重试、死信、人工补偿都回到 `xuan-tenant` 的统一运维视图中。

## 非目标

- 本次设计不实现完整的 Saga 反向补偿事务，只实现失败回流、重试、死信和人工重放入口。
- 本次设计不要求所有业务服务一次性接入，只要求先支持 `xuan-iam` 作为首个标准步骤。
- 本次设计不修改既有 Flyway 历史；如果实施阶段需要新增表结构字段或索引，必须按当前 migration 最高版本顺序新增。

## 核心决策

### 1. 创建接口采用异步启动语义

`POST /api/tenants` 在本地事务内完成租户主档、初始化总任务、初始化步骤和 Outbox 事件写入后立即返回，不同步等待 `xuan-iam` 完成。

返回后：

- 租户状态保持 `PROVISIONING`
- 初始化任务状态为 `RUNNING`
- 前端或运维通过任务查询接口跟踪进度

这样可以避免把 `xuan-tenant` 的创建链路与下游服务时延、故障和扩容能力强耦合。

### 2. 生命周期事件与编排事件分离

- `TenantCreated`、`TenantEnabled`、`TenantProvisioned` 是业务事实事件，只表达“已经发生了什么”。
- `TenantIamBootstrapRequested` 是编排请求事件，只表达“请某服务执行某个初始化步骤”。

下游服务不能只靠 `TenantCreated` 自行推断“现在轮到我初始化”，否则会失去步骤顺序、重试控制和统一任务视图。

### 3. `xuan-tenant` 负责总编排

`xuan-tenant` 负责：

- 创建 `tenant_provision_task`
- 创建 `tenant_provision_task_step`
- 生成步骤清单和顺序
- 发布步骤请求事件
- 订阅步骤完成/失败事件并更新任务状态
- 判断整个租户是否初始化完成
- 提供重试、死信和人工补偿入口

### 4. `xuan-iam` 只负责执行自己的步骤

`xuan-iam` 负责：

- 订阅 `TenantIamBootstrapRequested`
- 按 `eventId` 或步骤幂等键做消费幂等
- 调用已有 `bootstrap_iam_tenant`
- 发布 `TenantIamProvisionStepCompleted` 或 `TenantIamProvisionStepFailed`

`xuan-iam` 不直接修改 `xuan-tenant` 的任务表或租户表。

## 总体架构

### 参与者

- `xuan-tenant`
  - 编排中心
  - 任务与步骤事实源
  - 运维补偿入口
- `xuan-iam`
  - 首个标准初始化步骤执行者
  - 只负责 IAM 本地 bootstrap
- RocketMQ
  - 跨服务事件传输层
- Outbox
  - 各服务本地事务内的事件持久化保障

### 第一阶段边界

第一阶段先支持一个真实步骤：

- `IAM_BOOTSTRAP`

但编排器和任务模型必须允许后续追加：

- `AUDIT_BOOTSTRAP`
- `QUERY_BOOTSTRAP`
- 其他服务初始化步骤

## 数据与状态模型

### 总任务

`tenant_provision_task` 表示一次租户初始化总任务，关键字段语义如下：

- `task_type`: 固定为 `TENANT_PROVISION`
- `task_key`: 建议按 `tenant:provision:{tenantId}` 生成
- `status`: 总任务状态
- `idempotency_key`: 对应创建接口幂等键
- `step_name`: 当前推进中的步骤或最近处理步骤
- `request_payload` / `result_payload`: 存放总任务上下文
- `retry_count` / `max_retry_count`: 总任务级别的运维重试控制

### 任务步骤

`tenant_provision_task_step` 表示某次初始化任务下的单个步骤，关键字段语义如下：

- `step_key`: 机器可读步骤键，例如 `IAM_BOOTSTRAP`
- `step_name`: 人类可读名称，例如“初始化 IAM 默认授权”
- `sequence_no`: 顺序层级；相同层级可并行，不同层级按层推进
- `status`: 步骤状态
- `idempotency_key`: 单步骤请求幂等键
- `request_payload` / `result_payload`: 请求参数与执行结果
- `retry_count` / `max_retry_count`: 单步骤重试控制

### 推荐状态流转

总任务状态：

- `PENDING`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`

步骤状态：

- `PENDING`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`
- `SKIPPED`
- `CANCELLED`

第一阶段采用以下流转：

- 创建步骤时写 `PENDING`
- 发布步骤请求后转 `RUNNING`
- 收到成功回流后转 `SUCCEEDED`
- 收到失败回流后转 `FAILED`
- 手工跳过时才允许 `SKIPPED`

## 创建入口设计

### 接口语义

`POST /api/tenants` 在本地事务内执行以下动作：

1. 创建租户主档，租户状态写为 `PROVISIONING`
2. 创建一条 `tenant_provision_task`，状态写为 `RUNNING`
3. 生成步骤清单，第一期至少创建一条 `IAM_BOOTSTRAP` 步骤
4. 写入 `tenant_outbox_event`
   - `TenantCreated`
   - `TenantProvisioningStarted`
   - `TenantIamBootstrapRequested`

事务提交后接口立即返回租户详情，不等待下游回流。

### 返回语义

返回对象包含：

- 新建租户 ID
- 基础租户信息
- 当前状态 `PROVISIONING`
- 可选返回当前初始化任务 ID 或任务摘要

不在创建接口内返回“初始化已完成”假象，也不对单步骤成功做短轮询等待。

### 幂等要求

创建接口继续使用幂等键：

- 同一幂等键重复请求，不重复创建租户
- 不重复创建总任务
- 不重复创建步骤
- 不重复写步骤请求 Outbox

幂等命中后，返回已存在租户及其当前初始化状态。

## 编排步骤设计

### 轻量步骤注册表

本次不引入重量级通用状态机框架，而是采用轻量步骤注册表：

- 一个任务类型对应一组步骤定义
- 每个步骤定义包含：
  - `stepKey`
  - `stepName`
  - `sequenceNo`
  - `maxRetryCount`
  - `required`

第一期注册：

- `IAM_BOOTSTRAP`
  - `sequenceNo = 10`
  - `required = true`
  - `maxRetryCount = 5`

### 顺序与并行策略

第一阶段只有 `IAM_BOOTSTRAP` 一个步骤，因此没有并行问题。

后续扩展规则：

- 相同 `sequenceNo` 的步骤可并行请求
- 只有上一层必需步骤都成功，才推进下一层
- 非必需步骤失败可根据业务策略决定是否阻塞总任务

## 事件模型

### Topic 设计

第一阶段沿用现有 Topic：

- `xuan-tenant-event`
  - 生命周期事实事件
  - 编排请求事件
  - 编排汇总结果事件
- `xuan-iam-event`
  - IAM 自身领域事实事件
  - IAM 编排步骤结果事件

### 事件清单

`xuan-tenant` 发布：

- `TenantCreated`
- `TenantProvisioningStarted`
- `TenantIamBootstrapRequested`
- `TenantProvisioned`

`xuan-iam` 发布：

- `IamTenantBootstrapped`
- `TenantIamProvisionStepCompleted`
- `TenantIamProvisionStepFailed`

其中：

- `IamTenantBootstrapped` 是 IAM 本地事实事件
- `TenantIamProvisionStepCompleted` / `TenantIamProvisionStepFailed` 是面向编排回流的结果事件

两类事件不能混为一个，以免把服务内事实和跨服务编排语义耦死。

### 事件字段最小规范

所有编排事件必须包含：

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
- `retryCount`
- `maxRetryCount`
- `payload`

步骤结果事件额外包含：

- `status`
- `resultPayload`
- `lastErrorCode`
- `lastErrorMessage`
- `finishedAt`

## 端到端流程

### 首次初始化流程

1. `xuan-tenant` 接收 `POST /api/tenants`
2. 本地事务内创建租户、总任务、步骤和 Outbox 事件
3. Outbox 发布器发布 `TenantProvisioningStarted`
4. Outbox 发布器发布 `TenantIamBootstrapRequested`
5. `xuan-iam` 消费请求事件，调用 `bootstrap_iam_tenant`
6. `xuan-iam` 发布：
   - `IamTenantBootstrapped`
   - `TenantIamProvisionStepCompleted` 或 `TenantIamProvisionStepFailed`
7. `xuan-tenant` 消费编排结果事件，更新步骤和总任务
8. 全部必需步骤成功后：
   - 总任务转 `SUCCEEDED`
   - 租户状态从 `PROVISIONING` 推进到 `PROVISIONED`
   - 发布 `TenantProvisioned`

### 失败与重试流程

1. `xuan-iam` 执行失败，发布 `TenantIamProvisionStepFailed`
2. `xuan-tenant` 更新对应步骤为 `FAILED`
3. 如果 `retry_count < max_retry_count`：
   - 总任务保持 `RUNNING`
   - 允许运维或调度器重新派发该步骤请求
4. 如果超过最大重试次数：
   - 步骤保持 `FAILED`
   - 总任务转 `FAILED`
   - 运维通过人工补偿入口重放步骤或重调度 Outbox

## 状态汇总规则

总任务状态由步骤聚合得出：

- 全部必需步骤成功：总任务 `SUCCEEDED`
- 存在失败步骤且仍可继续重试：总任务保持 `RUNNING`
- 存在失败步骤且已超重试上限：总任务 `FAILED`
- 手工取消任务：总任务 `CANCELLED`

租户主档状态推进规则：

- 创建后写 `PROVISIONING`
- 全部必需步骤成功后写 `PROVISIONED`
- 本次设计不自动写 `ENABLED`

启用仍由显式生命周期接口触发，避免把“初始化完成”和“正式放行使用”混为一体。

## 重试、死信与人工补偿

### 运维接口

第一阶段需要补齐并打通：

- `GET /api/tenants/{id}/provision-tasks`
  - 查询总任务、步骤状态、失败原因、重试次数
- `POST /api/tenant-provision-tasks/{id}/retry`
  - 重试失败任务或失败步骤
- `POST /api/tenant-outbox-events/{id}/retry`
  - 重试发布失败或死信 Outbox 事件

### 人工补偿策略

人工补偿先收敛为两类操作：

- 重新派发步骤请求
- 重新调度 Outbox 事件

本次不引入通用人工补偿 DSL，也不在后台直接提供“任意修改步骤状态”的危险能力。

### 死信规则

步骤和 Outbox 事件都必须保留：

- `lastErrorCode`
- `lastErrorMessage`
- `firstFailedAt`
- `deadLetterAt`

当 Outbox 或步骤超过最大重试次数时进入死信或失败终态，由运维显式干预恢复。

## 幂等与一致性边界

### `xuan-tenant`

- 创建接口按幂等键防重
- 总任务按 `taskKey + idempotencyKey` 防重
- 单步骤按 `stepKey + idempotencyKey` 防重
- 消费步骤结果事件按 `eventId` 防重

### `xuan-iam`

- 消费 `TenantIamBootstrapRequested` 按事件幂等键防重
- 本地 `bootstrap_iam_tenant` 本身必须保持幂等
- 完成/失败回流事件通过本地 Outbox 最终发布

### 一致性原则

- Outbox 是各服务内“事件最终可发出”的事实来源
- RocketMQ 是传输层，不承担业务去重和业务唯一性
- 下游服务永远不直接修改 `xuan-tenant` 的任务事实

## 控制器与权限设计

第一阶段需要明确的权限边界：

- 创建租户：`tenant:create`
- 查询初始化任务：`tenant-provision:view`
- 重试任务/步骤：`tenant-provision:manage`
- 重试 Outbox 死信：`tenant-provision:manage`

控制器只负责：

- 协议适配
- DTO 校验
- 权限注解
- 调用编排应用服务

不在 Controller 中直接拼装编排状态机逻辑。

## 测试策略

### `xuan-tenant`

- 创建接口契约测试
  - 创建后立即返回 `PROVISIONING`
- 编排应用服务单测
  - 创建总任务和 `IAM_BOOTSTRAP` 步骤
- Outbox 写入测试
  - 写出 `TenantProvisioningStarted`
  - 写出 `TenantIamBootstrapRequested`
- 结果消费测试
  - 成功回流更新步骤
  - 失败回流更新步骤和总任务
- 汇总完成测试
  - 全部必需步骤成功后发布 `TenantProvisioned`
- 运维接口测试
  - 查询任务
  - 重试失败步骤
  - 重试死信 Outbox

### `xuan-iam`

- 请求事件消费测试
  - 收到 `TenantIamBootstrapRequested` 后调用 bootstrap
- 成功回流测试
  - 写出 `TenantIamProvisionStepCompleted`
- 失败回流测试
  - 写出 `TenantIamProvisionStepFailed`
- 幂等消费测试
  - 重复请求不产生重复副作用

## 第一阶段实施范围

本次实施只要求打通以下闭环：

1. `POST /api/tenants` 创建租户并启动初始化任务
2. `xuan-tenant` 发布 `TenantProvisioningStarted`
3. `xuan-tenant` 发布 `TenantIamBootstrapRequested`
4. `xuan-iam` 消费请求并执行 bootstrap
5. `xuan-iam` 回发成功/失败结果
6. `xuan-tenant` 汇总步骤状态
7. `xuan-tenant` 发布 `TenantProvisioned`
8. 补齐查询、重试、死信和人工补偿入口

## 验证标准

- `POST /api/tenants` 成功后立即返回，租户状态为 `PROVISIONING`
- 同一幂等键重复提交不重复创建租户和总任务
- `tenant_provision_task_step` 至少生成一条 `IAM_BOOTSTRAP`
- `TenantIamBootstrapRequested` 能稳定写入并最终发布
- `xuan-iam` 能稳定消费请求并执行 `bootstrap_iam_tenant`
- `TenantIamProvisionStepCompleted` / `TenantIamProvisionStepFailed` 能稳定回流到 `xuan-tenant`
- 所有必需步骤成功后才发布 `TenantProvisioned`
- 失败步骤和死信 Outbox 能通过运维入口重试或人工重放
