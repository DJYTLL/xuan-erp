package com.xuan.erp.tenant;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.ChangeTenantPlanStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantPlanCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantPlanCommand;
import com.xuan.erp.tenant.application.query.TenantPlanDetailView;
import com.xuan.erp.tenant.application.service.TenantPlanApplicationService;
import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
