# Tenant Provision Orchestration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `xuan-tenant` 基于 RocketMQ 编排多微服务租户初始化步骤，并首先接通 `xuan-iam` 初始化链路。

**Architecture:** `xuan-tenant` 作为总编排者维护 `tenant_provision_task` 与 `tenant_provision_task_step`，发布步骤请求事件；`xuan-iam` 作为步骤执行者消费 `TenantIamBootstrapRequested`，执行本地幂等 bootstrap，并回发编排结果事件；`xuan-tenant` 汇总步骤结果并决定何时发布 `TenantProvisioned`。

**Tech Stack:** Java 21, Spring Boot, RocketMQ, PostgreSQL, MyBatis, Outbox Pattern, JUnit 5

---

### Task 1: 固化编排事件契约

**Files:**
- Modify: `docs-site/src/content/docs/backend/services/xuan-tenant/events.md`
- Modify: `docs-site/src/content/docs/backend/services/xuan-iam/events.md`
- Create: `backend/xuan-tenant/src/test/java/.../TenantProvisionEventContractTest.java`
- Create: `backend/xuan-iam/src/test/java/.../IamProvisionEventContractTest.java`

- [ ] **Step 1: 写失败契约测试，约束事件名和关键字段**

```java
@Test
void tenantProvisioningEventsExposeRequiredFields() {
    List<String> events = List.of(
            "TenantProvisioningStarted",
            "TenantIamBootstrapRequested",
            "TenantProvisionStepCompleted",
            "TenantProvisionStepFailed",
            "TenantProvisioned");
    assertEquals(5, events.size());
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionEventContractTest test`
Expected: FAIL，因为事件契约类或文档尚未补齐

- [ ] **Step 3: 补齐事件文档和契约类**

```text
TenantIamBootstrapRequested:
- eventId
- traceId
- tenantId
- provisionTaskId
- stepKey
- idempotencyKey
- requestedBy
```

- [ ] **Step 4: 再跑定向测试转绿**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionEventContractTest test`
Expected: PASS

### Task 2: 在 xuan-tenant 建立初始化任务编排入口

**Files:**
- Create: `backend/xuan-tenant/src/main/java/.../application/service/TenantProvisionApplicationService.java`
- Create: `backend/xuan-tenant/src/main/java/.../domain/model/TenantProvisionRequestedEvent.java`
- Modify: `backend/xuan-tenant/src/main/java/.../domain/model/...`
- Test: `backend/xuan-tenant/src/test/java/.../TenantProvisionApplicationServiceTest.java`

- [ ] **Step 1: 写失败测试，约束创建 task 与 step 的行为**

```java
@Test
void startsProvisionTaskAndBuildsIamStep() {
    TenantProvisionApplicationService service = new TenantProvisionApplicationService(...);
    ProvisionResult result = service.startProvision(tenantId, "system");
    assertEquals("IAM_BOOTSTRAP", result.steps().getFirst().stepKey());
}
```

- [ ] **Step 2: 跑测试确认先红**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionApplicationServiceTest test`
Expected: FAIL with missing service or behavior

- [ ] **Step 3: 实现最小编排入口**

```java
public ProvisionResult startProvision(Long tenantId, String requestedBy) {
    TenantProvisionTask task = taskFactory.create(...);
    TenantProvisionTaskStep iamStep = stepFactory.createIamBootstrapStep(...);
    return repository.save(task, List.of(iamStep));
}
```

- [ ] **Step 4: 跑测试转绿**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionApplicationServiceTest test`
Expected: PASS

### Task 3: 在 xuan-tenant 发布 TenantIamBootstrapRequested

**Files:**
- Create: `backend/xuan-tenant/src/main/java/.../application/event/TenantIamBootstrapRequestedEvent.java`
- Create: `backend/xuan-tenant/src/main/java/.../infrastructure/mq/...`
- Modify: `backend/xuan-tenant/src/main/java/.../outbox/...`
- Test: `backend/xuan-tenant/src/test/java/.../TenantProvisionOutboxTest.java`

- [ ] **Step 1: 写失败测试，要求启动编排时写出步骤请求 Outbox**

```java
@Test
void writesIamBootstrapRequestedOutboxEvent() {
    TenantProvisionApplicationService service = new TenantProvisionApplicationService(...);
    service.startProvision(tenantId, "system");
    assertEquals("TenantIamBootstrapRequested", outbox.lastEvent().eventType());
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionOutboxTest test`
Expected: FAIL because event not written

- [ ] **Step 3: 实现 Outbox 写入**

```java
TenantOutboxEvent event = outboxFactory.forIamBootstrapRequested(
        tenantId, provisionTaskId, stepKey, idempotencyKey, requestedBy);
outboxRepository.append(event);
```

- [ ] **Step 4: 跑测试转绿**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionOutboxTest test`
Expected: PASS

### Task 4: 在 xuan-iam 消费 TenantIamBootstrapRequested

**Files:**
- Create: `backend/xuan-iam/src/main/java/.../application/event/TenantIamBootstrapRequestedEvent.java`
- Create: `backend/xuan-iam/src/main/java/.../infrastructure/mq/TenantIamBootstrapRequestedConsumer.java`
- Modify: `backend/xuan-iam/src/main/java/.../application/service/IamTenantBootstrapApplicationService.java`
- Test: `backend/xuan-iam/src/test/java/.../TenantIamBootstrapRequestedConsumerTest.java`

- [ ] **Step 1: 写失败测试，要求消费消息后触发 bootstrap**

```java
@Test
void consumesIamBootstrapRequestedAndCallsBootstrapService() {
    consumer.onMessage(event);
    verify(service).bootstrapTenant(tenantId, "tenant-provision");
}
```

- [ ] **Step 2: 跑测试确认先红**

Run: `mvn -pl xuan-iam -Dtest=TenantIamBootstrapRequestedConsumerTest test`
Expected: FAIL with missing consumer

- [ ] **Step 3: 实现最小消费者**

```java
public void onMessage(TenantIamBootstrapRequestedEvent event) {
    bootstrapApplicationService.bootstrapTenant(event.tenantId(), event.requestedBy());
}
```

- [ ] **Step 4: 跑测试转绿**

Run: `mvn -pl xuan-iam -Dtest=TenantIamBootstrapRequestedConsumerTest test`
Expected: PASS

### Task 5: 在 xuan-iam 发布步骤结果事件

**Files:**
- Create: `backend/xuan-iam/src/main/java/.../application/event/TenantIamProvisionStepCompletedEvent.java`
- Create: `backend/xuan-iam/src/main/java/.../application/event/TenantIamProvisionStepFailedEvent.java`
- Modify: `backend/xuan-iam/src/main/java/.../infrastructure/outbox/...`
- Test: `backend/xuan-iam/src/test/java/.../IamTenantProvisionResultOutboxTest.java`

- [ ] **Step 1: 写失败测试，要求 bootstrap 后写出编排结果事件**

```java
@Test
void writesTenantIamProvisionStepCompletedOutboxEvent() {
    service.handle(requestEvent);
    assertEquals("TenantIamProvisionStepCompleted", outbox.lastEvent().eventType());
}
```

- [ ] **Step 2: 跑测试确认先红**

Run: `mvn -pl xuan-iam -Dtest=IamTenantProvisionResultOutboxTest test`
Expected: FAIL because result event missing

- [ ] **Step 3: 实现完成/失败结果事件写入**

```java
if (success) {
    outboxRepository.append(completedEvent);
} else {
    outboxRepository.append(failedEvent);
}
```

- [ ] **Step 4: 跑测试转绿**

Run: `mvn -pl xuan-iam -Dtest=IamTenantProvisionResultOutboxTest test`
Expected: PASS

### Task 6: 在 xuan-tenant 汇总步骤结果

**Files:**
- Create: `backend/xuan-tenant/src/main/java/.../infrastructure/mq/TenantProvisionResultConsumer.java`
- Modify: `backend/xuan-tenant/src/main/java/.../application/service/TenantProvisionApplicationService.java`
- Test: `backend/xuan-tenant/src/test/java/.../TenantProvisionResultConsumerTest.java`

- [ ] **Step 1: 写失败测试，要求收到完成事件后更新 step 状态**

```java
@Test
void marksProvisionStepSucceededWhenIamCompletedEventArrives() {
    consumer.onMessage(completedEvent);
    assertEquals("SUCCEEDED", repository.findStep(stepId).status());
}
```

- [ ] **Step 2: 跑测试确认先红**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionResultConsumerTest test`
Expected: FAIL because consumer/update logic missing

- [ ] **Step 3: 实现步骤汇总逻辑**

```java
public void onMessage(TenantIamProvisionStepCompletedEvent event) {
    provisionService.markStepSucceeded(event.provisionTaskId(), event.stepKey(), event.resultPayload());
}
```

- [ ] **Step 4: 跑测试转绿**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionResultConsumerTest test`
Expected: PASS

### Task 7: 在 xuan-tenant 发布 TenantProvisioned

**Files:**
- Modify: `backend/xuan-tenant/src/main/java/.../application/service/TenantProvisionApplicationService.java`
- Modify: `backend/xuan-tenant/src/main/java/.../infrastructure/outbox/...`
- Test: `backend/xuan-tenant/src/test/java/.../TenantProvisionCompletedTest.java`

- [ ] **Step 1: 写失败测试，要求所有必需步骤成功后发布 TenantProvisioned**

```java
@Test
void publishesTenantProvisionedAfterAllRequiredStepsSucceed() {
    provisionService.markStepSucceeded(...);
    assertEquals("TenantProvisioned", outbox.lastEvent().eventType());
}
```

- [ ] **Step 2: 跑测试确认先红**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionCompletedTest test`
Expected: FAIL because completion publish logic missing

- [ ] **Step 3: 实现聚合完成判断**

```java
if (allRequiredStepsSucceeded(taskId)) {
    outboxRepository.append(tenantProvisionedEvent);
}
```

- [ ] **Step 4: 跑测试转绿**

Run: `mvn -pl xuan-tenant -Dtest=TenantProvisionCompletedTest test`
Expected: PASS

### Task 8: 完整验证

**Files:**
- Verify only

- [ ] **Step 1: 跑 tenant 模块测试**

Run: `mvn -pl xuan-tenant test`
Expected: BUILD SUCCESS

- [ ] **Step 2: 跑 iam 模块测试**

Run: `mvn -pl xuan-iam test`
Expected: BUILD SUCCESS

- [ ] **Step 3: 跑依赖联动测试**

Run: `mvn -pl xuan-tenant,xuan-iam -am test`
Expected: BUILD SUCCESS

- [ ] **Step 4: 人工验收编排链路**

检查点：
- `tenant_provision_task` 已创建
- `tenant_provision_task_step` 出现 `IAM_BOOTSTRAP`
- `TenantIamBootstrapRequested` 已写入或发出
- `xuan-iam` 已执行 `bootstrap_iam_tenant`
- `TenantIamProvisionStepCompleted` 已回流
- `TenantProvisioned` 仅在所有必需步骤完成后发出
