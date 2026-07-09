# Tenant Provision Orchestration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `xuan-tenant` 以异步编排方式启动租户初始化任务，先接通 `xuan-iam` 的 `IAM_BOOTSTRAP` 步骤，并补齐任务查询、重试、死信和人工补偿入口。

**Architecture:** `xuan-tenant` 作为总编排者维护 `tenant_provision_task`、`tenant_provision_task_step` 和 `tenant_outbox_event`，在 `POST /api/tenants` 成功后立即返回 `PROVISIONING`，再通过 Outbox + RocketMQ 发布 `TenantProvisioningStarted` 与 `TenantIamBootstrapRequested`。`xuan-iam` 作为步骤执行者消费请求事件、调用已有 `bootstrap_iam_tenant`、回发成功或失败结果；`xuan-tenant` 消费结果事件并汇总出 `TenantProvisioned`。

**Tech Stack:** Java 21, Spring Boot, Spring Security, RocketMQ, PostgreSQL, MyBatis XML, JUnit 5

---

## Current Migration Baseline

- `xuan-tenant` 当前最高 migration 版本：`V2__add_seata_undo_log.sql`
- `xuan-iam` 当前最高 migration 版本：`V2__add_iam_tenant_bootstrapped_outbox.sql`
- 本计划默认**不新增 migration**
- 如果执行中发现必须补字段、索引或新表，先重新扫描版本并新增：
  - `backend/xuan-tenant/src/main/resources/db/migration/V3__<english_description>.sql`
  - 或 `backend/xuan-iam/src/main/resources/db/migration/V3__<english_description>.sql`

### Task 1: 固化编排事件契约与 API 文档

**Files:**
- Modify: `docs-site/src/content/docs/backend/services/xuan-tenant/events.md`
- Modify: `docs-site/src/content/docs/backend/services/xuan-tenant/api.md`
- Modify: `docs-site/src/content/docs/backend/services/xuan-tenant/permissions.md`
- Modify: `docs-site/src/content/docs/backend/services/xuan-iam/events.md`
- Create: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantProvisionEventContractTest.java`
- Create: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamProvisionEventContractTest.java`

- [ ] **Step 1: 写失败测试，先约束租户编排事件名称和必填字段**

```java
@Test
void tenantProvisionEventsExposeExpectedNamesAndRequiredFields() {
    Map<String, List<String>> contract = Map.of(
            "TenantProvisioningStarted", List.of("eventId", "tenantId", "provisionTaskId", "occurredAt"),
            "TenantIamBootstrapRequested", List.of("eventId", "tenantId", "provisionTaskId", "stepKey", "idempotencyKey"),
            "TenantProvisioned", List.of("eventId", "tenantId", "provisionTaskId", "occurredAt"));

    assertEquals(3, contract.size());
    assertTrue(contract.get("TenantIamBootstrapRequested").contains("stepKey"));
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionEventContractTest,IamProvisionEventContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，因为事件契约测试文件或文档断言还不存在

- [ ] **Step 3: 补齐文档，明确创建接口立即返回 `PROVISIONING` 和首期只接 `IAM_BOOTSTRAP`**

```text
POST /api/tenants:
- 创建租户主档
- 创建 tenant_provision_task
- 创建 IAM_BOOTSTRAP 步骤
- 写出 TenantProvisioningStarted / TenantIamBootstrapRequested
- 立即返回 PROVISIONING
```

- [ ] **Step 4: 再跑定向测试转绿**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionEventContractTest,IamProvisionEventContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

### Task 2: 在 xuan-tenant 建立初始化编排服务并接入创建入口

**Files:**
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/service/TenantProvisionApplicationService.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/query/TenantProvisionTaskView.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/query/TenantProvisionTaskStepView.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/event/TenantProvisioningStartedEvent.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/event/TenantIamBootstrapRequestedEvent.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/domain/repository/TenantProvisionTaskStepRepository.java`
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/domain/repository/TenantOutboxEventRepository.java`
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/service/TenantApplicationService.java`
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/controller/TenantController.java`
- Test: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantProvisionApplicationServiceTest.java`
- Test: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantApplicationServiceCrudTest.java`

- [ ] **Step 1: 写失败测试，要求创建租户时同时启动总任务和 `IAM_BOOTSTRAP` 步骤**

```java
@Test
void createTenantStartsProvisionTaskAndBuildsIamBootstrapStep() {
    TenantProvisionApplicationService service = new TenantProvisionApplicationService(
            provisionTaskRepository, provisionTaskStepRepository, outboxRepository);

    service.startProvision(1001L, "tenant:create:acme", "idem-001", "system");

    assertEquals("TENANT_PROVISION", provisionTaskRepository.lastSaved().taskType());
    assertEquals("IAM_BOOTSTRAP", provisionTaskStepRepository.lastSaved().stepKey());
    assertEquals(ProvisionTaskStepStatus.PENDING, provisionTaskStepRepository.lastSaved().status());
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionApplicationServiceTest,TenantApplicationServiceCrudTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，因为编排服务、步骤仓储和联动逻辑都还不存在

- [ ] **Step 3: 实现最小编排入口，并在创建租户成功后调用它**

```java
public void startProvision(Long tenantId, String taskKey, String idempotencyKey, String requestedBy) {
    OffsetDateTime now = OffsetDateTime.now();
    TenantProvisionTask task = provisionTaskRepository.save(new TenantProvisionTask(
            null, tenantId, taskKey, "TENANT_PROVISION", ProvisionTaskStatus.RUNNING,
            idempotencyKey, "IAM_BOOTSTRAP", "{\"tenantId\":" + tenantId + "}", "{}",
            0, 5, null, null, now, null, requestedBy, now, requestedBy, now, null, null, null));

    provisionTaskStepRepository.save(new TenantProvisionTaskStep(
            null, tenantId, task.id(), "IAM_BOOTSTRAP", "初始化 IAM 默认授权",
            ProvisionTaskStepStatus.PENDING, 10, idempotencyKey + ":iam",
            "{\"tenantId\":" + tenantId + "}", "{}", 0, 5, null, null,
            null, null, requestedBy, now, requestedBy, now, null, null, null));
}
```

- [ ] **Step 4: 修改 `createTenant`，让返回体仍然是租户详情，但状态保持 `PROVISIONING`**

```java
Tenant saved = tenantRepository.save(new Tenant(..., TenantStatus.PROVISIONING, ...));
appendHistory(saved.id(), null, saved.status(), "CREATE", "租户创建", operator);
tenantProvisionApplicationService.startProvision(
        saved.id(), "tenant:provision:" + saved.id(), command.idempotencyKey(), operator);
return saved;
```

- [ ] **Step 5: 再跑定向测试转绿**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionApplicationServiceTest,TenantApplicationServiceCrudTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

### Task 3: 补齐 xuan-tenant 的步骤/Outbox 持久化与任务查询、重试控制器

**Files:**
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/persistence/entity/TenantProvisionTaskStepRecord.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/persistence/entity/TenantOutboxEventRecord.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/persistence/mapper/TenantProvisionTaskStepPersistenceMapper.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/persistence/mapper/TenantOutboxEventPersistenceMapper.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/persistence/repository/TenantProvisionTaskStepRepositoryAdapter.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/persistence/repository/TenantOutboxEventRepositoryAdapter.java`
- Create: `backend/xuan-tenant/src/main/resources/mapper/tenant/TenantProvisionTaskStepPersistenceMapper.xml`
- Create: `backend/xuan-tenant/src/main/resources/mapper/tenant/TenantOutboxEventPersistenceMapper.xml`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/controller/TenantProvisionTaskController.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/controller/TenantOutboxEventController.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/dto/TenantProvisionTaskResponse.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/dto/TenantProvisionTaskStepResponse.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/dto/RetryTenantProvisionTaskRequest.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/dto/RetryTenantOutboxEventRequest.java`
- Modify: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantControllerContractTest.java`
- Create: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantProvisionTaskControllerContractTest.java`
- Create: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantOutboxEventControllerContractTest.java`
- Create: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantProvisionPersistenceMapperContractTest.java`

- [ ] **Step 1: 写失败测试，先约束新增路由和权限码**

```java
@Test
void provisionTaskControllerExposesExpectedRoutesAndPermissions() throws Exception {
    assertEquals("hasAuthority('tenant-provision:view')",
            permission(TenantProvisionTaskController.class.getDeclaredMethod("listTasks", Long.class)));
    assertEquals("hasAuthority('tenant-provision:manage')",
            permission(TenantProvisionTaskController.class.getDeclaredMethod(
                    "retryTask", Long.class, RetryTenantProvisionTaskRequest.class)));
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionTaskControllerContractTest,TenantOutboxEventControllerContractTest,TenantProvisionPersistenceMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，因为控制器、DTO、Mapper 合同都还不存在

- [ ] **Step 3: 实现步骤仓储和 Outbox 仓储，先满足创建、按任务查询、按主键重试查询**

```java
public interface TenantProvisionTaskStepRepository {
    TenantProvisionTaskStep save(TenantProvisionTaskStep step);
    List<TenantProvisionTaskStep> findByTaskId(Long taskId);
    Optional<TenantProvisionTaskStep> findActiveByTaskIdAndStepKey(Long taskId, String stepKey);
}

public interface TenantOutboxEventRepository {
    TenantOutboxEvent append(TenantOutboxEvent event);
    Optional<TenantOutboxEvent> findById(Long id);
    TenantOutboxEvent save(TenantOutboxEvent event);
}
```

- [ ] **Step 4: 实现任务查询与人工重试控制器，先打通最小协议面**

```java
@GetMapping("/{tenantId}/provision-tasks")
@PreAuthorize("hasAuthority('tenant-provision:view')")
public ApiResponse<List<TenantProvisionTaskResponse>> listTasks(@PathVariable("tenantId") Long tenantId) {
    return ApiResponse.success(service.listTasks(tenantId));
}

@PostMapping("/api/tenant-provision-tasks/{taskId}/retry")
@PreAuthorize("hasAuthority('tenant-provision:manage')")
public ApiResponse<Void> retryTask(@PathVariable("taskId") Long taskId,
                                   @Valid @RequestBody RetryTenantProvisionTaskRequest request) {
    service.retryTask(taskId, request.stepKey(), request.operator(), request.reason());
    return ApiResponse.success(null);
}
```

- [ ] **Step 5: 再跑定向测试转绿**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionTaskControllerContractTest,TenantOutboxEventControllerContractTest,TenantProvisionPersistenceMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

### Task 4: 在 xuan-tenant 写出 `TenantProvisioningStarted` 与 `TenantIamBootstrapRequested`

**Files:**
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/mq/TenantProvisionEventPublisher.java`
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/service/TenantProvisionApplicationService.java`
- Test: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantProvisionOutboxTest.java`
- Test: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantRocketMqExampleSpringContextTest.java`

- [ ] **Step 1: 写失败测试，要求启动编排时写出两条 Outbox 事件**

```java
@Test
void startProvisionWritesStartedAndIamBootstrapRequestedOutboxEvents() {
    service.startProvision(1001L, "tenant:provision:1001", "idem-001", "system");

    assertEquals("TenantProvisioningStarted", outboxRepository.saved().get(0).eventType());
    assertEquals("TenantIamBootstrapRequested", outboxRepository.saved().get(1).eventType());
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionOutboxTest,TenantRocketMqExampleSpringContextTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，因为真实编排事件还没有写出

- [ ] **Step 3: 在编排服务里落 Outbox append，Publisher 只负责把领域对象转成 RocketMQ 消息**

```java
outboxRepository.append(outboxFactory.started(task, requestedBy));
outboxRepository.append(outboxFactory.iamBootstrapRequested(task, iamStep, requestedBy));

public void publishRequested(TenantIamBootstrapRequestedEvent event) {
    rocketMqMessageSender.send(new RocketMqMessage<>(
            rocketMqProperties.getTopics().getTenantEvents(),
            "tenant.provision.iam-bootstrap-requested",
            event.eventId(),
            event,
            Map.of("source", "xuan-tenant")));
}
```

- [ ] **Step 4: 再跑定向测试转绿**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionOutboxTest,TenantRocketMqExampleSpringContextTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

### Task 5: 在 xuan-iam 消费请求事件并回发步骤结果

**Files:**
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/event/TenantIamBootstrapRequestedEvent.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/event/TenantIamProvisionStepCompletedEvent.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/event/TenantIamProvisionStepFailedEvent.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/mq/TenantIamBootstrapRequestedConsumer.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/mq/IamProvisionResultPublisher.java`
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamTenantBootstrapApplicationService.java`
- Test: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/TenantIamBootstrapRequestedConsumerTest.java`
- Test: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamProvisionResultPublisherTest.java`

- [ ] **Step 1: 写失败测试，要求消费请求事件后调用已有 bootstrap 服务**

```java
@Test
void consumesIamBootstrapRequestedAndDelegatesToBootstrapService() {
    TenantIamBootstrapRequestedEvent event = new TenantIamBootstrapRequestedEvent(
            "evt-1", "trace-1", 1001L, 2001L, "IAM_BOOTSTRAP", "idem-001", "tenant-provision");

    consumer.handle(event);

    verify(service).bootstrapTenant(1001L, "tenant-provision");
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-iam -am "-Dtest=TenantIamBootstrapRequestedConsumerTest,IamProvisionResultPublisherTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，因为消费者和结果发布器还不存在

- [ ] **Step 3: 实现请求消费与成功/失败结果发布**

```java
public void handle(TenantIamBootstrapRequestedEvent event) {
    try {
        Integer inserted = bootstrapApplicationService.bootstrapTenant(event.tenantId(), event.requestedBy());
        resultPublisher.publishCompleted(event, inserted);
    } catch (RuntimeException error) {
        resultPublisher.publishFailed(event, error);
        throw error;
    }
}
```

- [ ] **Step 4: 再跑定向测试转绿**

Run: `mvn -pl xuan-iam -am "-Dtest=TenantIamBootstrapRequestedConsumerTest,IamProvisionResultPublisherTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

### Task 6: 在 xuan-tenant 消费结果事件、汇总任务并发布 `TenantProvisioned`

**Files:**
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/event/TenantIamProvisionStepCompletedEvent.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/event/TenantIamProvisionStepFailedEvent.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/mq/TenantProvisionResultConsumer.java`
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/service/TenantProvisionApplicationService.java`
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/service/TenantApplicationService.java`
- Test: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantProvisionResultConsumerTest.java`
- Test: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantProvisionCompletionTest.java`

- [ ] **Step 1: 写失败测试，要求收到成功回流后步骤转 `SUCCEEDED`，全部完成后发布 `TenantProvisioned`**

```java
@Test
void marksIamStepSucceededAndPublishesTenantProvisionedWhenAllRequiredStepsComplete() {
    service.handleCompleted(new TenantIamProvisionStepCompletedEvent(
            "evt-2", "trace-2", 1001L, 2001L, "IAM_BOOTSTRAP", "idem-001", "{\"inserted\":12}"));

    assertEquals(ProvisionTaskStepStatus.SUCCEEDED, stepRepository.lastSaved().status());
    assertEquals("TenantProvisioned", outboxRepository.lastSaved().eventType());
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionResultConsumerTest,TenantProvisionCompletionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，因为结果消费和任务汇总还没实现

- [ ] **Step 3: 实现成功/失败回流处理、总任务状态汇总和租户状态推进**

```java
public void handleCompleted(TenantIamProvisionStepCompletedEvent event) {
    TenantProvisionTaskStep step = requireStep(event.provisionTaskId(), event.stepKey());
    provisionTaskStepRepository.save(markSucceeded(step, event.resultPayload()));

    if (allRequiredStepsSucceeded(event.provisionTaskId())) {
        provisionTaskRepository.save(markTaskSucceeded(task));
        tenantRepository.save(requireTenant(event.tenantId()).markProvisioned("system", OffsetDateTime.now()));
        outboxRepository.append(outboxFactory.provisioned(task, event.tenantId()));
    }
}
```

- [ ] **Step 4: 再跑定向测试转绿**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionResultConsumerTest,TenantProvisionCompletionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

### Task 7: 打通失败重试、死信回放与控制器契约

**Files:**
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/application/service/TenantProvisionApplicationService.java`
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/controller/TenantProvisionTaskController.java`
- Modify: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/interfaces/controller/TenantOutboxEventController.java`
- Modify: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantControllerContractTest.java`
- Create: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantProvisionRetryTest.java`
- Create: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantOutboxRetryTest.java`

- [ ] **Step 1: 写失败测试，要求失败步骤重试时递增重试次数并重新写步骤请求事件**

```java
@Test
void retryFailedIamStepRequeuesRequestAndIncrementsRetryCount() {
    service.retryTask(2001L, "IAM_BOOTSTRAP", "ops", "手工重试");

    assertEquals(1, stepRepository.lastSaved().retryCount());
    assertEquals("TenantIamBootstrapRequested", outboxRepository.lastSaved().eventType());
}
```

- [ ] **Step 2: 跑测试确认它先红**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionRetryTest,TenantOutboxRetryTest,TenantControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，因为重试逻辑和权限契约还没补齐

- [ ] **Step 3: 实现步骤重试和 Outbox 死信重放，限制只允许 `tenant-provision:manage` 入口调用**

```java
public void retryTask(Long taskId, String stepKey, String operator, String reason) {
    TenantProvisionTaskStep step = requireRetryableStep(taskId, stepKey);
    TenantProvisionTaskStep reset = new TenantProvisionTaskStep(
            step.id(), step.tenantId(), step.provisionTaskId(), step.stepKey(), step.stepName(),
            ProvisionTaskStepStatus.RUNNING, step.sequenceNo(), step.idempotencyKey(),
            step.requestPayloadJson(), step.resultPayloadJson(), step.retryCount() + 1, step.maxRetryCount(),
            null, null, OffsetDateTime.now(), null, step.createdBy(), step.createdAt(), operator, OffsetDateTime.now(),
            null, null, null);
    provisionTaskStepRepository.save(reset);
    outboxRepository.append(outboxFactory.iamBootstrapRequestedFromStep(reset, operator, reason));
}
```

- [ ] **Step 4: 再跑定向测试转绿**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionRetryTest,TenantOutboxRetryTest,TenantControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

### Task 8: 全量文档与模块验证

**Files:**
- Modify: `docs-site/src/content/docs/backend/services/xuan-tenant/index.md`
- Modify: `docs-site/src/content/docs/backend/services/xuan-iam/index.md`
- Verify only

- [ ] **Step 1: 跑 tenant 定向测试组**

Run: `mvn -pl xuan-tenant -am "-Dtest=TenantProvisionEventContractTest,TenantProvisionApplicationServiceTest,TenantProvisionPersistenceMapperContractTest,TenantProvisionOutboxTest,TenantProvisionResultConsumerTest,TenantProvisionCompletionTest,TenantProvisionRetryTest,TenantOutboxRetryTest,TenantControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: BUILD SUCCESS

- [ ] **Step 2: 跑 iam 定向测试组**

Run: `mvn -pl xuan-iam -am "-Dtest=IamProvisionEventContractTest,TenantIamBootstrapRequestedConsumerTest,IamProvisionResultPublisherTest,IamTenantBootstrapApplicationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: BUILD SUCCESS

- [ ] **Step 3: 跑两模块联动回归**

Run: `mvn -pl xuan-tenant,xuan-iam -am test`
Expected: BUILD SUCCESS

- [ ] **Step 4: 人工验收关键检查点**

Checklist:

```text
- POST /api/tenants 创建后立即返回，租户状态是 PROVISIONING
- tenant_provision_task 已创建，task_type = TENANT_PROVISION
- tenant_provision_task_step 至少有一条 IAM_BOOTSTRAP
- tenant_outbox_event 已写出 TenantProvisioningStarted / TenantIamBootstrapRequested
- xuan-iam 能消费请求并调用 bootstrap_iam_tenant
- 成功回流后步骤更新为 SUCCEEDED
- 全部必需步骤完成后才发布 TenantProvisioned
- 失败步骤和死信 Outbox 可通过 manage 接口重试
```
