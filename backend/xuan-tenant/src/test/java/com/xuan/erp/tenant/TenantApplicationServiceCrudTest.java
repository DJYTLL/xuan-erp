package com.xuan.erp.tenant;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.api.PageResult;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.tenant.application.service.TenantApplicationService;
import com.xuan.erp.tenant.application.service.TenantProvisionApplicationService;
import com.xuan.erp.tenant.application.command.ChangeTenantStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantCommand;
import com.xuan.erp.tenant.application.query.TenantDetailView;
import com.xuan.erp.tenant.application.query.TenantInternalStatusView;
import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantDetailSupplement;
import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.TenantPlanAssignment;
import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import com.xuan.erp.tenant.domain.model.TenantProvisionTaskStep;
import com.xuan.erp.tenant.domain.model.TenantStatusHistory;
import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.PlanAssignmentStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;
import com.xuan.erp.tenant.domain.repository.TenantRepository;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.domain.repository.TenantPlanAssignmentRepository;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskStepRepository;
import com.xuan.erp.tenant.domain.repository.TenantStatusHistoryRepository;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantApplicationServiceCrudTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsTenantWithNormalizedCodeAndInitialStatusHistory() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        InMemoryTenantProvisionTaskRepository taskRepository = new InMemoryTenantProvisionTaskRepository();
        InMemoryTenantProvisionTaskStepRepository stepRepository = new InMemoryTenantProvisionTaskStepRepository();
        TenantProvisionApplicationService provisionService =
                new TenantProvisionApplicationService(taskRepository, stepRepository, new NoOpTenantOutboxEventRepository());
        TenantApplicationService service = new TenantApplicationService(tenantRepository, historyRepository, null, provisionService);

        TenantDetailView view = service.createTenant(new CreateTenantCommand(" Acme ", "玄云", "张三", "13800000000", "首个租户"));

        assertEquals("Acme", view.code());
        assertEquals(TenantStatus.PROVISIONING, view.status());
        assertEquals("acme", tenantRepository.findById(view.id()).orElseThrow().normalizedCode());
        assertEquals(1, historyRepository.saved.size());
        assertEquals(TenantStatus.PROVISIONING, historyRepository.saved.getFirst().toStatus());
        assertEquals(1, taskRepository.store.size());
        assertEquals("TENANT_PROVISION", taskRepository.store.values().iterator().next().taskType());
        assertEquals(1, stepRepository.saved.size());
        assertEquals(ProvisionTaskStepStatus.PENDING, stepRepository.saved.getFirst().status());
        String iamBootstrapPayload = stepRepository.saved.getFirst().requestPayloadJson();
        assertTrue(iamBootstrapPayload.contains("\"adminUsername\":\"admin\""));
        assertTrue(iamBootstrapPayload.contains("\"adminPasswordHash\""));
        assertFalse(iamBootstrapPayload.contains("\"adminPassword\""));
        assertFalse(iamBootstrapPayload.contains("123456"));
    }

    @Test
    void usesCurrentUserAsDefaultOperatorWhenCreatingTenant() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(7L, 1001L, "tenant-admin", Set.of("tenant_admin"), 5L, Set.of("tenant:create")),
                "N/A",
                Set.of()));
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, historyRepository);

        TenantDetailView view = service.createTenant(new CreateTenantCommand("acme", "玄云", null, null, null));

        Tenant tenant = tenantRepository.findById(view.id()).orElseThrow();
        assertEquals("tenant-admin", tenant.createdBy());
        assertEquals("tenant-admin", tenant.updatedBy());
        assertEquals("tenant-admin", historyRepository.saved.getFirst().createdBy());
    }

    @Test
    void createsTenantWithSelectedPlanAssignment() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        InMemoryTenantPlanRepository planRepository = new InMemoryTenantPlanRepository();
        InMemoryTenantPlanAssignmentRepository assignmentRepository = new InMemoryTenantPlanAssignmentRepository();
        planRepository.store.put(7L, tenantPlan(7L, "standard", TenantPlanStatus.ENABLED));
        OffsetDateTime planExpiresAt = OffsetDateTime.parse("2026-08-15T15:59:59Z");
        TenantApplicationService service = new TenantApplicationService(
                tenantRepository,
                historyRepository,
                null,
                null,
                null,
                planRepository,
                assignmentRepository);

        TenantDetailView view = service.createTenant(new CreateTenantCommand(
                "acme",
                "玄云",
                null,
                null,
                null,
                null,
                "admin",
                "123456",
                "租户管理员",
                null,
                null,
                7L,
                planExpiresAt));

        assertEquals(view.id(), assignmentRepository.saved.getFirst().tenantId());
        assertEquals(7L, assignmentRepository.saved.getFirst().planId());
        assertEquals(planExpiresAt, assignmentRepository.saved.getFirst().expiresAt());
        assertEquals(PlanAssignmentStatus.ACTIVE, assignmentRepository.saved.getFirst().status());
        assertEquals("CREATE_TENANT", assignmentRepository.saved.getFirst().source());
    }

    @Test
    void selectedPlanTemplateCodeDrivesIamBootstrapPayload() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        InMemoryTenantProvisionTaskRepository taskRepository = new InMemoryTenantProvisionTaskRepository();
        InMemoryTenantProvisionTaskStepRepository stepRepository = new InMemoryTenantProvisionTaskStepRepository();
        InMemoryTenantPlanRepository planRepository = new InMemoryTenantPlanRepository();
        InMemoryTenantPlanAssignmentRepository assignmentRepository = new InMemoryTenantPlanAssignmentRepository();
        planRepository.store.put(7L, tenantPlan(
                7L,
                "standard",
                TenantPlanStatus.ENABLED,
                "{\"modules\":[\"product\"],\"iamInitTemplateCode\":\"standard\"}"));
        TenantProvisionApplicationService provisionService = new TenantProvisionApplicationService(
                taskRepository,
                stepRepository,
                new NoOpTenantOutboxEventRepository());
        TenantApplicationService service = new TenantApplicationService(
                tenantRepository,
                historyRepository,
                null,
                provisionService,
                null,
                planRepository,
                assignmentRepository);

        service.createTenant(new CreateTenantCommand(
                "acme",
                "玄云",
                null,
                null,
                null,
                null,
                "admin",
                "123456",
                "租户管理员",
                null,
                null,
                7L,
                null));

        assertTrue(stepRepository.saved.getFirst().requestPayloadJson().contains("\"iamInitTemplateCode\":\"standard\""));
    }

    @Test
    void createTenantReturnsCurrentPlanSummaryWhenInitialPlanAssigned() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        InMemoryTenantPlanRepository planRepository = new InMemoryTenantPlanRepository();
        InMemoryTenantPlanAssignmentRepository assignmentRepository =
                new InMemoryTenantPlanAssignmentRepository(tenantRepository, planRepository);
        planRepository.store.put(7L, tenantPlan(7L, "standard", TenantPlanStatus.ENABLED));
        OffsetDateTime planExpiresAt = OffsetDateTime.parse("2026-08-15T15:59:59Z");
        TenantApplicationService service = new TenantApplicationService(
                tenantRepository,
                historyRepository,
                null,
                null,
                null,
                planRepository,
                assignmentRepository);

        TenantDetailView view = service.createTenant(new CreateTenantCommand(
                "acme",
                "玄云",
                null,
                null,
                null,
                null,
                "admin",
                "123456",
                "租户管理员",
                null,
                null,
                7L,
                planExpiresAt));

        assertEquals(7L, view.currentPlanId());
        assertEquals("standard", view.currentPlanCode());
        assertEquals("standard", view.currentPlanName());
        assertEquals(planExpiresAt, view.currentPlanExpiresAt());
    }

    @Test
    void rejectsDuplicateActiveTenantCode() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, historyRepository);
        service.createTenant(new CreateTenantCommand("acme", "玄云", null, null, null));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createTenant(new CreateTenantCommand("ACME", "另一个租户", null, null, null)));

        assertEquals("TENANT_CODE_EXISTS", error.code());
    }

    @Test
    void updatesTenantProfileWithoutChangingCode() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, new InMemoryTenantStatusHistoryRepository());
        Long tenantId = service.createTenant(new CreateTenantCommand("acme", "玄云", "张三", "138", null)).id();

        TenantDetailView view = service.updateTenant(tenantId, new UpdateTenantCommand("玄云 ERP", "李四", "139", "更新备注"));

        assertEquals("acme", view.code());
        assertEquals("玄云 ERP", view.name());
        assertEquals("李四", view.contactName());
    }

    @Test
    void disablesEnablesAndSoftDeletesTenantWithReason() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, historyRepository);
        Long tenantId = service.createTenant(new CreateTenantCommand("acme", "玄云", null, null, null)).id();

        Tenant created = tenantRepository.store.get(tenantId);
        tenantRepository.store.put(tenantId, created.markProvisioned("初始化完成", "system", OffsetDateTime.now()));

        service.enableTenant(tenantId, new ChangeTenantStatusCommand("开通启用", "admin"));
        assertEquals(TenantStatus.ENABLED, tenantRepository.findById(tenantId).orElseThrow().status());

        service.disableTenant(tenantId, new ChangeTenantStatusCommand("欠费停用", "admin"));
        Tenant disabled = tenantRepository.findById(tenantId).orElseThrow();
        assertEquals(TenantStatus.DISABLED, disabled.status());
        assertEquals("欠费停用", disabled.disabledReason());

        service.enableTenant(tenantId, new ChangeTenantStatusCommand("续费恢复", "admin"));
        assertEquals(TenantStatus.ENABLED, tenantRepository.findById(tenantId).orElseThrow().status());

        service.disableTenant(tenantId, new ChangeTenantStatusCommand("手动下线", "admin"));
        service.deleteTenant(tenantId, new DeleteTenantCommand("测试数据清理", "admin"));
        Tenant deleted = tenantRepository.store.get(tenantId);
        assertEquals("测试数据清理", deleted.deleteReason());
        assertTrue(deleted.deletedAt() != null);
        assertEquals(Optional.empty(), tenantRepository.findById(tenantId));
        assertEquals(6, historyRepository.saved.stream().filter(item -> item.tenantId().equals(tenantId)).count());
    }

    @Test
    void rejectsDisablingProvisionedTenantBeforeEnable() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, historyRepository);
        Long tenantId = service.createTenant(new CreateTenantCommand("acme", "玄云", null, null, null)).id();

        Tenant created = tenantRepository.store.get(tenantId);
        tenantRepository.store.put(tenantId, created.markProvisioned("初始化完成", "system", OffsetDateTime.now()));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.disableTenant(tenantId, new ChangeTenantStatusCommand("欠费停用", "admin")));

        assertEquals("tenant must be enabled before disabling", error.getMessage());
    }

    @Test
    void rejectsDeletingEnabledTenant() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, historyRepository);
        Long tenantId = service.createTenant(new CreateTenantCommand("acme", "玄云", null, null, null)).id();

        Tenant created = tenantRepository.store.get(tenantId);
        tenantRepository.store.put(tenantId, created.markProvisioned("初始化完成", "system", OffsetDateTime.now()));

        service.enableTenant(tenantId, new ChangeTenantStatusCommand("开通完成", "admin"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.deleteTenant(tenantId, new DeleteTenantCommand("误操作清理", "admin")));

        assertEquals("TENANT_DELETE_FORBIDDEN", error.code());
    }

    @Test
    void provisionedTenantAllowsLoginInInternalStatusView() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, new InMemoryTenantStatusHistoryRepository());
        Long tenantId = service.createTenant(new CreateTenantCommand("acme", "玄云", null, null, null)).id();
        Tenant created = tenantRepository.store.get(tenantId);
        tenantRepository.store.put(tenantId, created.markProvisioned("初始化完成", "system", OffsetDateTime.now()));

        TenantInternalStatusView status = service.getTenantInternalStatus(tenantId);

        assertEquals(TenantStatus.PROVISIONED, status.status());
        assertTrue(status.loginAllowed());
        assertEquals(null, status.loginDeniedReason());
    }

    @Test
    void disabledTenantDoesNotAllowLoginInInternalStatusView() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, new InMemoryTenantStatusHistoryRepository());
        Long tenantId = service.createTenant(new CreateTenantCommand("acme", "玄云", null, null, null)).id();
        Tenant created = tenantRepository.store.get(tenantId);
        tenantRepository.store.put(tenantId, created.markProvisioned("初始化完成", "system", OffsetDateTime.now()));
        service.enableTenant(tenantId, new ChangeTenantStatusCommand("开通启用", "admin"));
        service.disableTenant(tenantId, new ChangeTenantStatusCommand("欠费停用", "admin"));

        TenantInternalStatusView status = service.getTenantInternalStatus(tenantId);

        assertEquals(TenantStatus.DISABLED, status.status());
        assertFalse(status.loginAllowed());
        assertEquals("租户状态不允许登录: DISABLED", status.loginDeniedReason());
    }

    @Test
    void findsInternalStatusByTenantCodeForLogin() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, new InMemoryTenantStatusHistoryRepository());
        Long tenantId = service.createTenant(new CreateTenantCommand(" Acme ", "玄云", null, null, null)).id();
        Tenant created = tenantRepository.store.get(tenantId);
        tenantRepository.store.put(tenantId, created.markProvisioned("初始化完成", "system", OffsetDateTime.now()));

        TenantInternalStatusView status = service.getTenantInternalStatusByCode(" ACME ");

        assertEquals(tenantId, status.tenantId());
        assertEquals("Acme", status.code());
        assertTrue(status.loginAllowed());
    }

    @Test
    void supportsProvisionedStatusWithoutMixingItIntoEnableSemantics() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-08T08:00:00Z");
        Tenant tenant = new Tenant(
                1L,
                "acme",
                "acme",
                "玄云",
                TenantStatus.PROVISIONING,
                "张三",
                "138",
                null,
                null,
                null,
                null,
                null,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null
        );

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> tenant.enable("越级启用", "system", now.plusMinutes(1)));
        assertEquals("tenant must be provisioned before enabling", error.getMessage());

        Tenant provisioned = tenant.markProvisioned("初始化完成", "system", now.plusMinutes(5));
        Tenant enabled = provisioned.enable("手动启用", "system", now.plusMinutes(10));

        assertEquals(TenantStatus.PROVISIONED, provisioned.status());
        assertEquals(now.plusMinutes(5), provisioned.provisionedAt());
        assertNull(provisioned.disabledReason());
        assertEquals(TenantStatus.ENABLED, enabled.status());
        assertEquals(now.plusMinutes(5), enabled.provisionedAt());
        assertEquals(now.plusMinutes(10), enabled.enabledAt());
        assertNull(enabled.disabledReason());
    }

    @Test
    void listsOnlyActiveTenants() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, new InMemoryTenantStatusHistoryRepository());
        Long first = service.createTenant(new CreateTenantCommand("a", "A", null, null, null)).id();
        service.createTenant(new CreateTenantCommand("b", "B", null, null, null));
        service.deleteTenant(first, new DeleteTenantCommand("清理", "admin"));

        List<TenantDetailView> tenants = service.listTenants();

        assertEquals(1, tenants.size());
        assertEquals("b", tenants.getFirst().code());
    }

    @Test
    void paginatesActiveTenants() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, new InMemoryTenantStatusHistoryRepository());
        service.createTenant(new CreateTenantCommand("a", "A", null, null, null));
        service.createTenant(new CreateTenantCommand("b", "B", null, null, null));
        service.createTenant(new CreateTenantCommand("c", "C", null, null, null));
        service.createTenant(new CreateTenantCommand("d", "D", null, null, null));
        service.createTenant(new CreateTenantCommand("e", "E", null, null, null));

        PageResult<TenantDetailView> page = service.listTenants(2, 2);

        assertEquals(5, page.total());
        assertEquals(2, page.pageNum());
        assertEquals(2, page.pageSize());
        assertEquals(List.of("c", "d"), page.records().stream().map(TenantDetailView::code).toList());
    }

    @Test
    void paginatedTenantListIncludesAggregatedPlanSummary() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, new InMemoryTenantStatusHistoryRepository());
        Long tenantId = service.createTenant(new CreateTenantCommand("acme", "玄云", "张三", "138", null)).id();
        tenantRepository.detailSupplements.put(tenantId, new TenantDetailSupplement(
                12L,
                9L,
                "pro",
                "专业版",
                OffsetDateTime.parse("2026-12-31T15:59:59Z"),
                null,
                null,
                1L,
                "CREATE",
                OffsetDateTime.parse("2026-07-07T08:00:00Z")
        ));

        PageResult<TenantDetailView> page = service.listTenants(1, 20);

        assertEquals(1, page.records().size());
        TenantDetailView row = page.records().getFirst();
        assertEquals(12L, row.currentPlanAssignmentId());
        assertEquals(9L, row.currentPlanId());
        assertEquals("专业版", row.currentPlanName());
        assertEquals(OffsetDateTime.parse("2026-12-31T15:59:59Z"), row.currentPlanExpiresAt());
    }

    @Test
    void returnsAggregatedTenantDetailSummary() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, new InMemoryTenantStatusHistoryRepository());
        Long tenantId = service.createTenant(new CreateTenantCommand("acme", "玄云", "张三", "138", null)).id();
        tenantRepository.detailSupplements.put(tenantId, new TenantDetailSupplement(
                12L,
                9L,
                "pro",
                "专业版",
                OffsetDateTime.parse("2026-12-31T15:59:59Z"),
                5L,
                "acme.example.com",
                3L,
                "DISABLE",
                OffsetDateTime.parse("2026-07-07T08:00:00Z")
        ));

        TenantDetailView detail = service.getTenant(tenantId);

        assertEquals("pro", detail.currentPlanCode());
        assertEquals("专业版", detail.currentPlanName());
        assertEquals(12L, detail.currentPlanAssignmentId());
        assertEquals(OffsetDateTime.parse("2026-12-31T15:59:59Z"), detail.currentPlanExpiresAt());
        assertEquals("acme.example.com", detail.primaryDomain());
        assertEquals(3L, detail.statusHistoryCount());
        assertEquals("DISABLE", detail.latestStatusChangeType());
    }

    @Test
    void reusesSucceededIdempotencyKeyForCreateTenant() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        InMemoryTenantProvisionTaskRepository provisionTaskRepository = new InMemoryTenantProvisionTaskRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, historyRepository, provisionTaskRepository);

        TenantDetailView first = service.createTenant(new CreateTenantCommand("acme", "玄云", "张三", "13800000000", "首个租户", "idem-create-1"));
        TenantDetailView second = service.createTenant(new CreateTenantCommand("acme", "玄云", "张三", "13800000000", "首个租户", "idem-create-1"));

        assertEquals(first.id(), second.id());
        assertEquals(1, tenantRepository.store.size());
        assertEquals(1, historyRepository.saved.size());
        assertEquals(1, provisionTaskRepository.store.size());
        assertEquals(ProvisionTaskStatus.SUCCEEDED, provisionTaskRepository.store.values().iterator().next().status());
    }

    private static final class InMemoryTenantRepository implements TenantRepository {
        private final Map<Long, Tenant> store = new LinkedHashMap<>();
        private final Map<Long, TenantDetailSupplement> detailSupplements = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<Tenant> findById(Long id) {
            return Optional.ofNullable(store.get(id)).filter(Tenant::active);
        }

        @Override
        public Optional<Tenant> findActiveByNormalizedCode(String normalizedCode) {
            return store.values().stream()
                    .filter(Tenant::active)
                    .filter(item -> item.normalizedCode().equals(normalizedCode))
                    .findFirst();
        }

        @Override
        public List<Tenant> findActiveTenants() {
            return store.values().stream()
                    .filter(Tenant::active)
                    .sorted(Comparator.comparing(Tenant::id))
                    .toList();
        }

        @Override
        public List<Tenant> findActiveTenants(long offset, long limit) {
            return findActiveTenants().stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countActiveTenants() {
            return store.values().stream().filter(Tenant::active).count();
        }

        @Override
        public TenantDetailSupplement getDetailSupplement(Long tenantId) {
            return detailSupplements.getOrDefault(tenantId, TenantDetailSupplement.empty());
        }

        @Override
        public Tenant save(Tenant tenant) {
            Long id = tenant.id() == null ? nextId++ : tenant.id();
            Tenant saved = new Tenant(
                    id,
                    tenant.code(),
                    tenant.normalizedCode(),
                    tenant.name(),
                    tenant.status(),
                    tenant.contactName(),
                    tenant.contactPhone(),
                    tenant.provisionedAt(),
                    tenant.enabledAt(),
                    tenant.disabledAt(),
                    tenant.disabledReason(),
                    tenant.remark(),
                    tenant.createdBy(),
                    tenant.createdAt(),
                    tenant.updatedBy(),
                    tenant.updatedAt(),
                    tenant.deletedBy(),
                    tenant.deleteReason(),
                    tenant.deletedAt()
            );
            store.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryTenantStatusHistoryRepository implements TenantStatusHistoryRepository {
        private final List<TenantStatusHistory> saved = new ArrayList<>();

        @Override
        public TenantStatusHistory append(TenantStatusHistory history) {
            TenantStatusHistory savedHistory = new TenantStatusHistory(
                    (long) saved.size() + 1,
                    history.tenantId(),
                    history.fromStatus(),
                    history.toStatus(),
                    history.changeType(),
                    history.changeReason(),
                    history.changedAt() == null ? OffsetDateTime.now() : history.changedAt(),
                    history.changedBy(),
                    history.traceId(),
                    history.requestId(),
                    history.source(),
                    history.createdBy(),
                    history.createdAt()
            );
            saved.add(savedHistory);
            return savedHistory;
        }
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
                    (long) saved.size() + 1,
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

    private static final class InMemoryTenantPlanRepository implements TenantPlanRepository {
        private final Map<Long, TenantPlan> store = new LinkedHashMap<>();

        @Override
        public Optional<TenantPlan> findById(Long id) {
            return Optional.ofNullable(store.get(id)).filter(plan -> plan.deletedAt() == null);
        }

        @Override
        public Optional<TenantPlan> findActiveByCode(String code) {
            return store.values().stream()
                    .filter(plan -> plan.deletedAt() == null)
                    .filter(plan -> plan.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<TenantPlan> findActivePlans() {
            return store.values().stream()
                    .filter(plan -> plan.deletedAt() == null)
                    .toList();
        }

        @Override
        public TenantPlan save(TenantPlan plan) {
            store.put(plan.id(), plan);
            return plan;
        }
    }

    private static final class InMemoryTenantPlanAssignmentRepository implements TenantPlanAssignmentRepository {
        private final List<TenantPlanAssignment> saved = new ArrayList<>();
        private final InMemoryTenantRepository tenantRepository;
        private final InMemoryTenantPlanRepository planRepository;

        private InMemoryTenantPlanAssignmentRepository() {
            this(null, null);
        }

        private InMemoryTenantPlanAssignmentRepository(
                InMemoryTenantRepository tenantRepository,
                InMemoryTenantPlanRepository planRepository) {
            this.tenantRepository = tenantRepository;
            this.planRepository = planRepository;
        }

        @Override
        public TenantPlanAssignment save(TenantPlanAssignment assignment) {
            TenantPlanAssignment savedAssignment = new TenantPlanAssignment(
                    (long) saved.size() + 1,
                    assignment.tenantId(),
                    assignment.previousPlanId(),
                    assignment.planId(),
                    assignment.status(),
                    assignment.effectiveAt(),
                    assignment.expiresAt(),
                    assignment.assignedAt(),
                    assignment.assignedBy(),
                    assignment.changeReason(),
                    assignment.source(),
                    assignment.remark(),
                    assignment.createdBy(),
                    assignment.createdAt(),
                    assignment.updatedBy(),
                    assignment.updatedAt(),
                    assignment.deletedBy(),
                    assignment.deleteReason(),
                    assignment.deletedAt());
            saved.add(savedAssignment);
            if (tenantRepository != null && planRepository != null) {
                TenantPlan plan = planRepository.store.get(savedAssignment.planId());
                tenantRepository.detailSupplements.put(savedAssignment.tenantId(), new TenantDetailSupplement(
                        savedAssignment.id(),
                        plan.id(),
                        plan.code(),
                        plan.name(),
                        savedAssignment.expiresAt(),
                        null,
                        null,
                        0L,
                        null,
                        null));
            }
            return savedAssignment;
        }

        @Override
        public List<TenantPlanAssignment> findActiveByPlanId(Long planId) {
            return saved.stream()
                    .filter(assignment -> assignment.deletedAt() == null)
                    .filter(assignment -> assignment.planId().equals(planId))
                    .filter(assignment -> assignment.status() == PlanAssignmentStatus.ACTIVE)
                    .toList();
        }
    }

    private static TenantPlan tenantPlan(Long id, String code, TenantPlanStatus status) {
        return tenantPlan(id, code, status, "{}");
    }

    private static TenantPlan tenantPlan(Long id, String code, TenantPlanStatus status, String featureFlagsJson) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-14T00:00:00Z");
        return new TenantPlan(
                id,
                code,
                code,
                status,
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

    private static final class NoOpTenantOutboxEventRepository implements TenantOutboxEventRepository {
        @Override
        public Optional<com.xuan.erp.tenant.domain.model.TenantOutboxEvent> findById(Long eventId) {
            return Optional.empty();
        }

        @Override
        public List<com.xuan.erp.tenant.domain.model.TenantOutboxEvent> findPublishable(int limit) {
            return List.of();
        }

        @Override
        public com.xuan.erp.tenant.domain.model.TenantOutboxEvent save(com.xuan.erp.tenant.domain.model.TenantOutboxEvent event) {
            return event;
        }

        @Override
        public com.xuan.erp.tenant.domain.model.TenantOutboxEvent append(com.xuan.erp.tenant.domain.model.TenantOutboxEvent event) {
            return event;
        }
    }
}
