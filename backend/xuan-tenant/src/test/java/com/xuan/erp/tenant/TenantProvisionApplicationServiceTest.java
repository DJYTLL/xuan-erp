package com.xuan.erp.tenant;

import com.xuan.erp.tenant.application.query.TenantProvisionTaskStepView;
import com.xuan.erp.tenant.application.query.TenantProvisionTaskView;
import com.xuan.erp.tenant.application.service.TenantProvisionApplicationService;
import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import com.xuan.erp.tenant.domain.model.TenantProvisionTaskStep;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskStepRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantProvisionApplicationServiceTest {

    @Test
    void isDiscoverableAsSpringServiceBean() {
        assertTrue(TenantProvisionApplicationService.class.isAnnotationPresent(Service.class));
    }

    @Test
    void startsTenantProvisionTaskAndCreatesPendingIamBootstrapStep() {
        InMemoryTenantProvisionTaskRepository taskRepository = new InMemoryTenantProvisionTaskRepository();
        InMemoryTenantProvisionTaskStepRepository stepRepository = new InMemoryTenantProvisionTaskStepRepository();
        TenantProvisionApplicationService service = new TenantProvisionApplicationService(
                taskRepository,
                stepRepository,
                new NoOpTenantOutboxEventRepository()
        );

        TenantProvisionTaskView view = service.startProvisioning(101L, "tenant:create:acme", "idem-provision-1", "system");

        assertEquals("TENANT_PROVISION", view.taskType());
        assertEquals("tenant:create:acme", view.taskKey());
        assertEquals(ProvisionTaskStatus.PENDING, view.status());
        assertEquals(1, stepRepository.saved.size());
        TenantProvisionTaskStepView step = view.steps().getFirst();
        assertEquals("IAM_BOOTSTRAP", step.stepKey());
        assertEquals("IAM_BOOTSTRAP", step.stepName());
        assertEquals(ProvisionTaskStepStatus.PENDING, step.status());
    }

    @Test
    void listsTenantProvisionTasksWithNestedSteps() {
        InMemoryTenantProvisionTaskRepository taskRepository = new InMemoryTenantProvisionTaskRepository();
        InMemoryTenantProvisionTaskStepRepository stepRepository = new InMemoryTenantProvisionTaskStepRepository();
        TenantProvisionApplicationService service = new TenantProvisionApplicationService(
                taskRepository,
                stepRepository,
                new NoOpTenantOutboxEventRepository()
        );

        TenantProvisionTask task = taskRepository.save(new TenantProvisionTask(
                null,
                101L,
                "tenant:create:acme",
                "TENANT_PROVISION",
                ProvisionTaskStatus.PENDING,
                "idem-101",
                "TENANT_PROVISION",
                "{}",
                "{}",
                0,
                5,
                null,
                null,
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                null,
                "system",
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                null,
                null,
                null
        ));
        stepRepository.save(new TenantProvisionTaskStep(
                null,
                101L,
                task.id(),
                "IAM_BOOTSTRAP",
                "IAM_BOOTSTRAP",
                ProvisionTaskStepStatus.PENDING,
                1,
                "idem-101",
                "{}",
                "{}",
                0,
                3,
                null,
                null,
                null,
                null,
                "system",
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                null,
                null,
                null
        ));

        List<TenantProvisionTaskView> tasks = service.listTasks(101L);

        assertEquals(1, tasks.size());
        assertEquals("tenant:create:acme", tasks.getFirst().taskKey());
        assertEquals(1, tasks.getFirst().steps().size());
        assertEquals("IAM_BOOTSTRAP", tasks.getFirst().steps().getFirst().stepKey());
    }

    @Test
    void retryTaskMovesTaskAndStepBackToRunningState() {
        InMemoryTenantProvisionTaskRepository taskRepository = new InMemoryTenantProvisionTaskRepository();
        InMemoryTenantProvisionTaskStepRepository stepRepository = new InMemoryTenantProvisionTaskStepRepository();
        TenantProvisionApplicationService service = new TenantProvisionApplicationService(
                taskRepository,
                stepRepository,
                new NoOpTenantOutboxEventRepository()
        );

        TenantProvisionTask task = taskRepository.save(new TenantProvisionTask(
                null,
                101L,
                "tenant:create:acme",
                "TENANT_PROVISION",
                ProvisionTaskStatus.FAILED,
                "idem-101",
                "IAM_BOOTSTRAP",
                "{}",
                "{}",
                0,
                5,
                "BOOTSTRAP_FAILED",
                "boom",
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                OffsetDateTime.parse("2026-07-08T08:05:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-08T08:05:00Z"),
                null,
                null,
                null
        ));
        stepRepository.save(new TenantProvisionTaskStep(
                null,
                101L,
                task.id(),
                "IAM_BOOTSTRAP",
                "IAM_BOOTSTRAP",
                ProvisionTaskStepStatus.FAILED,
                1,
                "idem-101",
                "{}",
                "{}",
                0,
                3,
                "BOOTSTRAP_FAILED",
                "boom",
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                OffsetDateTime.parse("2026-07-08T08:05:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-08T08:00:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-08T08:05:00Z"),
                null,
                null,
                null
        ));

        assertDoesNotThrow(() -> service.retryTask(task.id(), "IAM_BOOTSTRAP", "ops", "人工重试"));
        assertEquals(ProvisionTaskStatus.RUNNING, taskRepository.store.get(task.id()).status());
        TenantProvisionTaskStep retriedStep = stepRepository.saved.get(stepRepository.saved.size() - 1);
        assertEquals(ProvisionTaskStepStatus.RUNNING, retriedStep.status());
        assertEquals("ops", retriedStep.updatedBy());
        assertNotNull(retriedStep.startedAt());
    }

    @Test
    void retryOutboxEventMovesEventBackToPendingState() {
        InMemoryTenantProvisionTaskRepository taskRepository = new InMemoryTenantProvisionTaskRepository();
        InMemoryTenantProvisionTaskStepRepository stepRepository = new InMemoryTenantProvisionTaskStepRepository();
        InMemoryTenantOutboxEventRepository outboxRepository = new InMemoryTenantOutboxEventRepository();
        TenantProvisionApplicationService service = new TenantProvisionApplicationService(
                taskRepository,
                stepRepository,
                outboxRepository
        );

        TenantOutboxEvent event = outboxRepository.append(new TenantOutboxEvent(
                null,
                101L,
                "evt-101",
                "TENANT_PROVISION_TASK",
                9001L,
                "TenantIamBootstrapRequested",
                "tenant-events",
                "{}",
                "{}",
                OutboxEventStatus.FAILED,
                1,
                5,
                null,
                null,
                null,
                OffsetDateTime.parse("2026-07-08T08:15:00Z"),
                null,
                "SEND_FAILED",
                "mq down",
                OffsetDateTime.parse("2026-07-08T08:10:00Z"),
                OffsetDateTime.parse("2026-07-08T08:20:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-08T08:10:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-08T08:20:00Z")
        ));

        assertDoesNotThrow(() -> service.retryOutboxEvent(event.id(), "ops", "死信回放"));
        assertEquals(OutboxEventStatus.PENDING, outboxRepository.store.get(event.id()).status());
        assertEquals("ops", outboxRepository.store.get(event.id()).updatedBy());
    }

    private static final class InMemoryTenantProvisionTaskRepository implements TenantProvisionTaskRepository {
        private final Map<Long, TenantProvisionTask> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<TenantProvisionTask> findById(Long taskId) {
            return Optional.ofNullable(store.get(taskId));
        }

        @Override
        public List<TenantProvisionTask> findActiveByTenantId(Long tenantId) {
            return store.values().stream()
                    .filter(item -> item.tenantId().equals(tenantId))
                    .filter(item -> item.deletedAt() == null)
                    .toList();
        }

        @Override
        public Optional<TenantProvisionTask> findActiveByTenantIdAndTaskKey(Long tenantId, String taskKey) {
            return store.values().stream()
                    .filter(item -> item.tenantId().equals(tenantId))
                    .filter(item -> item.taskKey().equals(taskKey))
                    .filter(item -> item.deletedAt() == null)
                    .findFirst();
        }

        @Override
        public Optional<TenantProvisionTask> findActiveByTaskKeyAndIdempotencyKey(String taskKey, String idempotencyKey) {
            return store.values().stream()
                    .filter(item -> item.taskKey().equals(taskKey))
                    .filter(item -> item.idempotencyKey().equals(idempotencyKey))
                    .filter(item -> item.deletedAt() == null)
                    .findFirst();
        }

        @Override
        public TenantProvisionTask save(TenantProvisionTask task) {
            Long id = task.id() == null ? nextId++ : task.id();
            TenantProvisionTask saved = new TenantProvisionTask(
                    id,
                    task.tenantId(),
                    task.taskKey(),
                    task.taskType(),
                    task.status(),
                    task.idempotencyKey(),
                    task.stepName(),
                    task.requestPayloadJson(),
                    task.resultPayloadJson(),
                    task.retryCount(),
                    task.maxRetryCount(),
                    task.lastErrorCode(),
                    task.lastErrorMessage(),
                    task.startedAt(),
                    task.finishedAt(),
                    task.createdBy(),
                    task.createdAt(),
                    task.updatedBy(),
                    task.updatedAt(),
                    task.deletedBy(),
                    task.deleteReason(),
                    task.deletedAt()
            );
            store.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryTenantProvisionTaskStepRepository implements TenantProvisionTaskStepRepository {
        private final List<TenantProvisionTaskStep> saved = new ArrayList<>();
        private long nextId = 1;

        @Override
        public List<TenantProvisionTaskStep> findByTaskId(Long taskId) {
            return saved.stream()
                    .filter(item -> item.provisionTaskId().equals(taskId))
                    .toList();
        }

        @Override
        public Optional<TenantProvisionTaskStep> findActiveByTaskIdAndStepKey(Long taskId, String stepKey) {
            return saved.stream()
                    .filter(item -> item.provisionTaskId().equals(taskId))
                    .filter(item -> item.stepKey().equals(stepKey))
                    .filter(item -> item.deletedAt() == null)
                    .findFirst();
        }

        @Override
        public TenantProvisionTaskStep save(TenantProvisionTaskStep step) {
            TenantProvisionTaskStep savedStep = new TenantProvisionTaskStep(
                    step.id() == null ? nextId++ : step.id(),
                    step.tenantId(),
                    step.provisionTaskId(),
                    step.stepKey(),
                    step.stepName(),
                    step.status(),
                    step.sequenceNo(),
                    step.idempotencyKey(),
                    step.requestPayloadJson(),
                    step.resultPayloadJson(),
                    step.retryCount(),
                    step.maxRetryCount(),
                    step.lastErrorCode(),
                    step.lastErrorMessage(),
                    step.startedAt(),
                    step.finishedAt(),
                    step.createdBy(),
                    step.createdAt(),
                    step.updatedBy(),
                    step.updatedAt(),
                    step.deletedBy(),
                    step.deleteReason(),
                    step.deletedAt()
            );
            saved.add(savedStep);
            return savedStep;
        }
    }

    private static final class InMemoryTenantOutboxEventRepository implements TenantOutboxEventRepository {
        private final Map<Long, TenantOutboxEvent> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<TenantOutboxEvent> findById(Long eventId) {
            return Optional.ofNullable(store.get(eventId));
        }

        @Override
        public TenantOutboxEvent save(TenantOutboxEvent event) {
            Long id = event.id() == null ? nextId++ : event.id();
            TenantOutboxEvent saved = new TenantOutboxEvent(
                    id,
                    event.tenantId(),
                    event.eventId(),
                    event.aggregateType(),
                    event.aggregateId(),
                    event.eventType(),
                    event.topic(),
                    event.payloadJson(),
                    event.headersJson(),
                    event.status(),
                    event.retryCount(),
                    event.maxRetryCount(),
                    event.lockedBy(),
                    event.lockedAt(),
                    event.lockExpiresAt(),
                    event.nextRetryAt(),
                    event.publishedAt(),
                    event.lastErrorCode(),
                    event.lastErrorMessage(),
                    event.firstFailedAt(),
                    event.deadLetterAt(),
                    event.createdBy(),
                    event.createdAt(),
                    event.updatedBy(),
                    event.updatedAt()
            );
            store.put(id, saved);
            return saved;
        }

        @Override
        public TenantOutboxEvent append(TenantOutboxEvent event) {
            return save(event);
        }
    }

    private static final class NoOpTenantOutboxEventRepository implements TenantOutboxEventRepository {
        @Override
        public Optional<TenantOutboxEvent> findById(Long eventId) {
            return Optional.empty();
        }

        @Override
        public TenantOutboxEvent save(TenantOutboxEvent event) {
            return event;
        }

        @Override
        public com.xuan.erp.tenant.domain.model.TenantOutboxEvent append(com.xuan.erp.tenant.domain.model.TenantOutboxEvent event) {
            return event;
        }
    }
}
