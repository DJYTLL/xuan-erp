package com.xuan.erp.tenant;

import com.xuan.erp.tenant.application.service.TenantPlanAssignmentApplicationService;
import com.xuan.erp.tenant.application.service.TenantPermissionSyncFingerprint;
import com.xuan.erp.tenant.application.service.TenantResourceApplicationService;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import com.xuan.erp.tenant.domain.model.resource.TenantResourceDefinition;
import com.xuan.erp.tenant.domain.repository.TenantResourceRepository;
import com.xuan.erp.tenant.domain.service.TenantResourceCatalog;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantPlanAssignmentApplicationServiceTest {

    @Test
    void createAssignmentDefaultsRequiredDateFieldsWhenRequestOmitsThem() {
        RecordingResourceRepository repository = new RecordingResourceRepository();
        TenantPlanAssignmentApplicationService service = createService(repository);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tenantId", 2L);
        values.put("planId", 1L);
        values.put("status", null);
        values.put("effectiveAt", null);
        values.put("assignedAt", null);

        service.createAssignment(values);

        assertEquals("tenant_plan_assignment", repository.lastResource().tableName());
        assertEquals("ACTIVE", repository.lastValues().get("status"));
        assertInstanceOf(OffsetDateTime.class, repository.lastValues().get("effective_at"));
        assertInstanceOf(OffsetDateTime.class, repository.lastValues().get("assigned_at"));
    }

    @Test
    void updateAssignmentDefaultsRequiredDateFieldsWhenRequestOmitsThem() {
        RecordingResourceRepository repository = new RecordingResourceRepository();
        TenantPlanAssignmentApplicationService service = createService(repository);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("planId", 3L);
        values.put("status", "");
        values.put("effectiveAt", null);
        values.put("assignedAt", null);

        service.updateAssignment(33L, values);

        assertEquals("tenant_plan_assignment", repository.lastResource().tableName());
        assertEquals("ACTIVE", repository.lastValues().get("status"));
        assertInstanceOf(OffsetDateTime.class, repository.lastValues().get("effective_at"));
        assertInstanceOf(OffsetDateTime.class, repository.lastValues().get("assigned_at"));
    }

    @Test
    void updateAssignmentPublishesIamTemplateSyncRequestForNewPlanTemplate() {
        RecordingResourceRepository repository = new RecordingResourceRepository();
        InMemoryTenantPlanRepository planRepository = new InMemoryTenantPlanRepository();
        RecordingTenantOutboxEventRepository outboxRepository = new RecordingTenantOutboxEventRepository();
        RecordingTenantIamBootstrapGateway iamBootstrapGateway = new RecordingTenantIamBootstrapGateway();
        planRepository.save(tenantPlan(3L, "premium", """
                {
                  "iamInitTemplateCode": "premium-template",
                  "columnPermissionTemplateCodes": ["tenant-basic", "iam-user-basic"],
                  "defaultColumnPermissionTemplateCode": "tenant-basic"
                }
                """));
        TenantPlanAssignmentApplicationService service = createService(
                repository,
                planRepository,
                outboxRepository,
                iamBootstrapGateway);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tenantId", 2L);
        values.put("planId", 3L);
        values.put("assignedBy", "super_admin");
        values.put("changeReason", "切换高级套餐");

        service.updateAssignment(33L, values);

        TenantOutboxEvent event = outboxRepository.lastEvent();
        assertNotNull(event);
        assertEquals("TenantIamBootstrapRequested", event.eventType());
        assertEquals("xuan-tenant-event", event.topic());
        assertEquals(2L, event.tenantId());
        assertEquals("TENANT_PLAN_ASSIGNMENT", event.aggregateType());
        assertEquals(33L, event.aggregateId());
        assertEquals("super_admin", event.createdBy());
        assertEquals("super_admin", event.updatedBy());
        assertEquals("{\"sourceService\":\"xuan-tenant\"}", event.headersJson());
        org.assertj.core.api.Assertions.assertThat(event.payloadJson())
                .contains("\"tenantId\":2")
                .contains("\"planId\":3")
                .contains("\"assignmentId\":33")
                .contains("\"provisionStep\":\"IAM_BOOTSTRAP\"")
                .contains("\"iamInitTemplateCode\":\"premium-template\"")
                .contains("\"columnPermissionTemplateCodes\":[\"tenant-basic\",\"iam-user-basic\"]")
                .contains("\"defaultColumnPermissionTemplateCode\":\"tenant-basic\"")
                .contains("\"callbackRequired\":false");
        String permissionHash = TenantPermissionSyncFingerprint.hash(
                "premium-template",
                List.of("tenant-basic", "iam-user-basic"),
                "tenant-basic");
        assertEquals(List.of("2:premium-template:tenant-basic,iam-user-basic:tenant-basic:" + permissionHash + ":super_admin"),
                iamBootstrapGateway.requests);
    }

    @Test
    void scheduledRepairQueuesOutboxWithoutCallingIamHttpGateway() {
        RecordingResourceRepository repository = new RecordingResourceRepository();
        repository.listRows = List.of(new LinkedHashMap<>(Map.of(
                "id", 33L,
                "tenantId", 2L,
                "planId", 3L,
                "status", "ACTIVE",
                "permissionSyncStatus", "PENDING_REPAIR"
        )));
        InMemoryTenantPlanRepository planRepository = new InMemoryTenantPlanRepository();
        RecordingTenantOutboxEventRepository outboxRepository = new RecordingTenantOutboxEventRepository();
        RecordingTenantIamBootstrapGateway iamBootstrapGateway = new RecordingTenantIamBootstrapGateway();
        planRepository.save(tenantPlan(3L, "premium", """
                {
                  "iamInitTemplateCode": "premium-template",
                  "columnPermissionTemplateCodes": ["tenant-basic", "iam-user-basic"],
                  "defaultColumnPermissionTemplateCode": "tenant-basic"
                }
                """));
        TenantPlanAssignmentApplicationService service = createService(
                repository,
                planRepository,
                outboxRepository,
                iamBootstrapGateway);

        service.repairPendingPermissionSyncs(10);

        assertNotNull(outboxRepository.lastEvent());
        assertEquals("TenantIamBootstrapRequested", outboxRepository.lastEvent().eventType());
        assertEquals(List.of(), iamBootstrapGateway.requests);
    }

    @Test
    void repairTenantPermissionSyncSendsPageTemplateColumnTemplatePoolAndPermissionHashToIam() {
        RecordingResourceRepository repository = new RecordingResourceRepository();
        repository.listRows = List.of(new LinkedHashMap<>(Map.of(
                "id", 33L,
                "tenantId", 2L,
                "planId", 3L,
                "status", "ACTIVE",
                "assignedAt", "2026-08-04T00:00:00Z"
        )));
        InMemoryTenantPlanRepository planRepository = new InMemoryTenantPlanRepository();
        RecordingTenantOutboxEventRepository outboxRepository = new RecordingTenantOutboxEventRepository();
        RecordingTenantIamBootstrapGateway iamBootstrapGateway = new RecordingTenantIamBootstrapGateway();
        planRepository.save(tenantPlan(3L, "premium", """
                {
                  "iamInitTemplateCode": "premium-template",
                  "columnPermissionTemplateCodes": ["tenant-basic", "iam-user-basic"],
                  "defaultColumnPermissionTemplateCode": "tenant-basic"
                }
                """));
        TenantPlanAssignmentApplicationService service = createService(
                repository,
                planRepository,
                outboxRepository,
                iamBootstrapGateway);
        String permissionHash = TenantPermissionSyncFingerprint.hash(
                "premium-template",
                List.of("tenant-basic", "iam-user-basic"),
                "tenant-basic");

        service.repairTenantPermissionSync(2L, "security-admin");

        assertNotNull(outboxRepository.lastEvent());
        org.assertj.core.api.Assertions.assertThat(outboxRepository.lastEvent().payloadJson())
                .contains("\"iamInitTemplateCode\":\"premium-template\"")
                .contains("\"columnPermissionTemplateCodes\":[\"tenant-basic\",\"iam-user-basic\"]")
                .contains("\"defaultColumnPermissionTemplateCode\":\"tenant-basic\"")
                .contains("\"permissionHash\":\"" + permissionHash + "\"");
        assertEquals(List.of("2:premium-template:tenant-basic,iam-user-basic:tenant-basic:" + permissionHash + ":security-admin"),
                iamBootstrapGateway.requests);
        assertEquals("SYNCED", repository.lastValues().get("permission_sync_status"));
        assertEquals(permissionHash, repository.lastValues().get("permission_sync_expected_hash"));
    }

    private TenantPlanAssignmentApplicationService createService(RecordingResourceRepository repository) {
        return new TenantPlanAssignmentApplicationService(new TenantResourceApplicationService(
                TenantResourceCatalog.defaultCatalog(),
                repository
        ));
    }

    private TenantPlanAssignmentApplicationService createService(
            RecordingResourceRepository repository,
            TenantPlanRepository planRepository,
            TenantOutboxEventRepository outboxRepository) {
        return createService(repository, planRepository, outboxRepository, null);
    }

    private TenantPlanAssignmentApplicationService createService(
            RecordingResourceRepository repository,
            TenantPlanRepository planRepository,
            TenantOutboxEventRepository outboxRepository,
            com.xuan.erp.tenant.domain.repository.TenantIamBootstrapGateway iamBootstrapGateway) {
        return new TenantPlanAssignmentApplicationService(
                new TenantResourceApplicationService(
                        TenantResourceCatalog.defaultCatalog(),
                        repository
                ),
                planRepository,
                outboxRepository,
                iamBootstrapGateway
        );
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
                0,
                null,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static final class RecordingResourceRepository implements TenantResourceRepository {

        private TenantResourceDefinition lastResource;
        private Map<String, Object> lastValues = Map.of();
        private List<Map<String, Object>> listRows = List.of();

        @Override
        public List<Map<String, Object>> list(TenantResourceDefinition resource) {
            lastResource = resource;
            return listRows;
        }

        @Override
        public Optional<Map<String, Object>> findById(TenantResourceDefinition resource, Long id) {
            lastResource = resource;
            return Optional.empty();
        }

        @Override
        public Map<String, Object> create(TenantResourceDefinition resource, Map<String, Object> values) {
            lastResource = resource;
            lastValues = new LinkedHashMap<>(values);
            return values;
        }

        @Override
        public Map<String, Object> update(TenantResourceDefinition resource, Long id, Map<String, Object> values) {
            lastResource = resource;
            lastValues = new LinkedHashMap<>(values);
            return values;
        }

        @Override
        public void softDelete(TenantResourceDefinition resource, Long id, String reason, String operator) {
            lastResource = resource;
        }

        TenantResourceDefinition lastResource() {
            return lastResource;
        }

        Map<String, Object> lastValues() {
            return lastValues;
        }
    }

    private static final class InMemoryTenantPlanRepository implements TenantPlanRepository {
        private final Map<Long, TenantPlan> store = new LinkedHashMap<>();

        @Override
        public Optional<TenantPlan> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<TenantPlan> findActiveByCode(String code) {
            return store.values().stream()
                    .filter(plan -> plan.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<TenantPlan> findActivePlans() {
            return List.copyOf(store.values());
        }

        @Override
        public TenantPlan save(TenantPlan plan) {
            store.put(plan.id(), plan);
            return plan;
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

    private static final class RecordingTenantIamBootstrapGateway
            implements com.xuan.erp.tenant.domain.repository.TenantIamBootstrapGateway {
        private final List<String> requests = new java.util.ArrayList<>();

        @Override
        public void bootstrapTenant(Long tenantId, String iamInitTemplateCode, String operator) {
            requests.add(tenantId + ":" + iamInitTemplateCode + ":-:-:" + operator);
        }

        @Override
        public void bootstrapTenant(
                Long tenantId,
                String iamInitTemplateCode,
                List<String> columnPermissionTemplateCodes,
                String defaultColumnPermissionTemplateCode,
                String operator) {
            requests.add(tenantId + ":"
                    + iamInitTemplateCode + ":"
                    + String.join(",", columnPermissionTemplateCodes) + ":"
                    + defaultColumnPermissionTemplateCode + ":"
                    + operator);
        }

        @Override
        public void bootstrapTenant(
                Long tenantId,
                String iamInitTemplateCode,
                List<String> columnPermissionTemplateCodes,
                String defaultColumnPermissionTemplateCode,
                String permissionHash,
                String operator) {
            requests.add(tenantId + ":"
                    + iamInitTemplateCode + ":"
                    + String.join(",", columnPermissionTemplateCodes) + ":"
                    + defaultColumnPermissionTemplateCode + ":"
                    + permissionHash + ":"
                    + operator);
        }
    }
}
