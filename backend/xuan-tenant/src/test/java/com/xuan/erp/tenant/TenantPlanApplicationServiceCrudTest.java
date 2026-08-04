package com.xuan.erp.tenant;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.ChangeTenantPlanStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantPlanCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantPlanCommand;
import com.xuan.erp.tenant.application.query.TenantPlanDetailView;
import com.xuan.erp.tenant.application.service.TenantPlanApplicationService;
import com.xuan.erp.tenant.application.service.TenantPlanAssignmentApplicationService;
import com.xuan.erp.tenant.application.service.TenantResourceApplicationService;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.TenantPlanAssignment;
import com.xuan.erp.tenant.domain.model.resource.TenantResourceDefinition;
import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.PlanAssignmentStatus;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.domain.repository.TenantIamBootstrapGateway;
import com.xuan.erp.tenant.domain.repository.TenantPlanAssignmentRepository;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import com.xuan.erp.tenant.domain.repository.TenantResourceRepository;
import com.xuan.erp.tenant.domain.service.TenantResourceCatalog;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantPlanApplicationServiceCrudTest {

    @Test
    void createsTenantPlanWithNormalizedCodeDefaultsAndListsActivePlans() {
        InMemoryTenantPlanRepository repository = new InMemoryTenantPlanRepository();
        TenantPlanApplicationService service = new TenantPlanApplicationService(repository);

        TenantPlanDetailView view = service.createPlan(new CreateTenantPlanCommand(
                " pro ",
                "专业版",
                BillingCycle.MONTHLY,
                new BigDecimal("199.00"),
                "CNY",
                50,
                10,
                new BigDecimal("100.00"),
                "{\"modules\":[\"sales\"]}",
                20,
                "专业租户套餐"
        ));

        assertEquals("pro", view.code());
        assertEquals(TenantPlanStatus.ENABLED, view.status());
        assertEquals(BillingCycle.MONTHLY, view.billingCycle());
        assertEquals(1, service.listPlans().size());
    }

    @Test
    void rejectsDuplicateActiveTenantPlanCode() {
        InMemoryTenantPlanRepository repository = new InMemoryTenantPlanRepository();
        TenantPlanApplicationService service = new TenantPlanApplicationService(repository);
        service.createPlan(new CreateTenantPlanCommand("pro", "专业版", BillingCycle.MONTHLY, BigDecimal.ZERO,
                "CNY", null, null, null, "{}", 1, null));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createPlan(new CreateTenantPlanCommand("PRO", "重复套餐", BillingCycle.MONTHLY, BigDecimal.ZERO,
                        "CNY", null, null, null, "{}", 1, null)));

        assertEquals("TENANT_PLAN_CODE_EXISTS", error.code());
    }

    @Test
    void updatesDisablesEnablesAndSoftDeletesTenantPlanWithReason() {
        InMemoryTenantPlanRepository repository = new InMemoryTenantPlanRepository();
        TenantPlanApplicationService service = new TenantPlanApplicationService(repository);
        Long planId = service.createPlan(new CreateTenantPlanCommand("pro", "专业版", BillingCycle.MONTHLY, BigDecimal.ZERO,
                "CNY", null, null, null, "{}", 1, null)).id();

        TenantPlanDetailView updated = service.updatePlan(planId, new UpdateTenantPlanCommand(
                "专业版 Plus", BillingCycle.YEARLY, new BigDecimal("1999.00"), "CNY",
                100, 20, new BigDecimal("500.00"), "{\"modules\":[\"sales\",\"finance\"]}", 2, "升级套餐"
        ));
        assertEquals("专业版 Plus", updated.name());
        assertEquals(BillingCycle.YEARLY, updated.billingCycle());

        service.disablePlan(planId, new ChangeTenantPlanStatusCommand("停售", "admin"));
        TenantPlan disabled = repository.store.get(planId);
        assertEquals(TenantPlanStatus.DISABLED, disabled.status());

        service.enablePlan(planId, new ChangeTenantPlanStatusCommand("重新上架", "admin"));
        assertEquals(TenantPlanStatus.ENABLED, repository.store.get(planId).status());

        service.deletePlan(planId, new DeleteTenantCommand("套餐废弃", "admin"));
        TenantPlan deleted = repository.store.get(planId);
        assertEquals("套餐废弃", deleted.deleteReason());
        assertTrue(deleted.deletedAt() != null);
        assertEquals(Optional.empty(), repository.findById(planId));
    }

    @Test
    void updatePlanPublishesIamTemplateSyncForActiveTenantsWhenTemplateChanges() {
        InMemoryTenantPlanRepository repository = new InMemoryTenantPlanRepository();
        InMemoryTenantPlanAssignmentRepository assignmentRepository = new InMemoryTenantPlanAssignmentRepository();
        RecordingTenantOutboxEventRepository outboxRepository = new RecordingTenantOutboxEventRepository();
        RecordingTenantIamBootstrapGateway iamBootstrapGateway = new RecordingTenantIamBootstrapGateway();
        Long planId = repository.save(tenantPlan(null, "pro", "{\"iamInitTemplateCode\":\"basic-template\"}")).id();
        assignmentRepository.save(assignment(81L, 2001L, planId));
        TenantPlanAssignmentApplicationService assignmentService = new TenantPlanAssignmentApplicationService(
                new TenantResourceApplicationService(TenantResourceCatalog.defaultCatalog(), new NoOpTenantResourceRepository()),
                repository,
                outboxRepository,
                iamBootstrapGateway);
        TenantPlanApplicationService service = new TenantPlanApplicationService(repository, assignmentRepository, assignmentService);

        service.updatePlan(planId, new UpdateTenantPlanCommand(
                "专业版", BillingCycle.MONTHLY, BigDecimal.ZERO, "CNY",
                null, null, null, "{\"iamInitTemplateCode\":\"premium-template\"}", 1, null
        ));

        TenantOutboxEvent event = outboxRepository.lastEvent();
        assertNotNull(event);
        assertEquals("TenantIamBootstrapRequested", event.eventType());
        assertEquals(2001L, event.tenantId());
        assertEquals("TENANT_PLAN_ASSIGNMENT", event.aggregateType());
        assertEquals(81L, event.aggregateId());
        org.assertj.core.api.Assertions.assertThat(event.payloadJson())
                .contains("\"tenantId\":2001")
                .contains("\"planId\":" + planId)
                .contains("\"assignmentId\":81")
                .contains("\"iamInitTemplateCode\":\"premium-template\"")
                .contains("\"callbackRequired\":false");
        assertEquals(List.of("2001:premium-template:system"), iamBootstrapGateway.requests);
    }

    @Test
    void findsActiveTenantIdsUsingPlansWithIamInitTemplateCode() {
        InMemoryTenantPlanRepository repository = new InMemoryTenantPlanRepository();
        InMemoryTenantPlanAssignmentRepository assignmentRepository = new InMemoryTenantPlanAssignmentRepository();
        Long basicPlanId = repository.save(tenantPlan(null, "basic", "{\"iamInitTemplateCode\":\"basic\"}")).id();
        Long standardPlanId = repository.save(tenantPlan(null, "standard", "{\"iamInitTemplateCode\":\"standard\"}")).id();
        Long blankPlanId = repository.save(tenantPlan(null, "blank", "{\"modules\":[\"product\"]}")).id();
        assignmentRepository.save(assignment(81L, 2001L, basicPlanId));
        assignmentRepository.save(assignment(82L, 2002L, basicPlanId));
        assignmentRepository.save(assignment(83L, 2003L, standardPlanId));
        assignmentRepository.save(assignment(84L, 2004L, blankPlanId));
        TenantPlanApplicationService service = new TenantPlanApplicationService(repository, assignmentRepository, null);

        List<Long> tenantIds = service.findActiveTenantIdsByIamInitTemplateCode(" basic ");

        assertEquals(List.of(2001L, 2002L), tenantIds);
    }

    private static TenantPlan tenantPlan(Long id, String code, String featureFlagsJson) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-16T00:00:00Z");
        return new TenantPlan(
                id,
                code,
                code,
                TenantPlanStatus.ENABLED,
                BillingCycle.MONTHLY,
                BigDecimal.ZERO,
                "CNY",
                null,
                null,
                null,
                featureFlagsJson,
                1,
                null,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static TenantPlanAssignment assignment(Long id, Long tenantId, Long planId) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-16T00:00:00Z");
        return new TenantPlanAssignment(
                id,
                tenantId,
                null,
                planId,
                PlanAssignmentStatus.ACTIVE,
                now,
                null,
                now,
                "system",
                "测试",
                "TEST",
                null,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static final class InMemoryTenantPlanRepository implements TenantPlanRepository {
        private final Map<Long, TenantPlan> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<TenantPlan> findById(Long id) {
            return Optional.ofNullable(store.get(id)).filter(TenantPlan::active);
        }

        @Override
        public Optional<TenantPlan> findActiveByCode(String code) {
            return store.values().stream()
                    .filter(TenantPlan::active)
                    .filter(item -> item.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<TenantPlan> findActivePlans() {
            return store.values().stream()
                    .filter(TenantPlan::active)
                    .sorted(Comparator.comparing(TenantPlan::sortNo).thenComparing(TenantPlan::id))
                    .toList();
        }

        @Override
        public TenantPlan save(TenantPlan plan) {
            Long id = plan.id() == null ? nextId++ : plan.id();
            TenantPlan saved = new TenantPlan(
                    id,
                    plan.code(),
                    plan.name(),
                    plan.status(),
                    plan.billingCycle(),
                    plan.priceAmount(),
                    plan.currency(),
                    plan.maxUserCount(),
                    plan.maxWarehouseCount(),
                    plan.maxStorageGb(),
                    plan.featureFlagsJson(),
                    plan.sortNo(),
                    plan.remark(),
                    plan.createdBy(),
                    plan.createdAt() == null ? OffsetDateTime.now() : plan.createdAt(),
                    plan.updatedBy(),
                    plan.updatedAt() == null ? OffsetDateTime.now() : plan.updatedAt(),
                    plan.deletedBy(),
                    plan.deleteReason(),
                    plan.deletedAt()
            );
            store.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryTenantPlanAssignmentRepository implements TenantPlanAssignmentRepository {
        private final Map<Long, TenantPlanAssignment> store = new LinkedHashMap<>();

        @Override
        public TenantPlanAssignment save(TenantPlanAssignment assignment) {
            store.put(assignment.id(), assignment);
            return assignment;
        }

        @Override
        public List<TenantPlanAssignment> findActiveByPlanId(Long planId) {
            return store.values().stream()
                    .filter(assignment -> assignment.deletedAt() == null)
                    .filter(assignment -> assignment.planId().equals(planId))
                    .filter(assignment -> assignment.status() == PlanAssignmentStatus.ACTIVE)
                    .toList();
        }
    }

    private static final class RecordingTenantOutboxEventRepository implements TenantOutboxEventRepository {
        private TenantOutboxEvent lastEvent;

        @Override
        public Optional<TenantOutboxEvent> findById(Long eventId) {
            return Optional.empty();
        }

        @Override
        public List<TenantOutboxEvent> findPublishable(int limit) {
            return List.of();
        }

        @Override
        public TenantOutboxEvent append(TenantOutboxEvent event) {
            lastEvent = event;
            return event;
        }

        @Override
        public TenantOutboxEvent save(TenantOutboxEvent event) {
            lastEvent = event;
            return event;
        }

        TenantOutboxEvent lastEvent() {
            return lastEvent;
        }
    }

    private static final class RecordingTenantIamBootstrapGateway implements TenantIamBootstrapGateway {
        private final List<String> requests = new java.util.ArrayList<>();

        @Override
        public void bootstrapTenant(Long tenantId, String iamInitTemplateCode, String operator) {
            requests.add(tenantId + ":" + iamInitTemplateCode + ":" + operator);
        }
    }

    private static final class NoOpTenantResourceRepository implements TenantResourceRepository {
        @Override
        public List<Map<String, Object>> list(TenantResourceDefinition resource) {
            return List.of();
        }

        @Override
        public Optional<Map<String, Object>> findById(TenantResourceDefinition resource, Long id) {
            return Optional.empty();
        }

        @Override
        public Map<String, Object> create(TenantResourceDefinition resource, Map<String, Object> values) {
            return values;
        }

        @Override
        public Map<String, Object> update(TenantResourceDefinition resource, Long id, Map<String, Object> values) {
            return values;
        }

        @Override
        public void softDelete(TenantResourceDefinition resource, Long id, String reason, String operator) {
        }
    }
}
