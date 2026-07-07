package com.xuan.erp.tenant;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.service.TenantApplicationService;
import com.xuan.erp.tenant.application.command.ChangeTenantStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantCommand;
import com.xuan.erp.tenant.application.query.TenantDetailView;
import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantStatusHistory;
import com.xuan.erp.tenant.domain.repository.TenantRepository;
import com.xuan.erp.tenant.domain.repository.TenantStatusHistoryRepository;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantApplicationServiceCrudTest {

    @Test
    void createsTenantWithNormalizedCodeAndInitialStatusHistory() {
        InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
        InMemoryTenantStatusHistoryRepository historyRepository = new InMemoryTenantStatusHistoryRepository();
        TenantApplicationService service = new TenantApplicationService(tenantRepository, historyRepository);

        TenantDetailView view = service.createTenant(new CreateTenantCommand(" Acme ", "玄云", "张三", "13800000000", "首个租户"));

        assertEquals("Acme", view.code());
        assertEquals(TenantStatus.PROVISIONING, view.status());
        assertEquals("acme", tenantRepository.findById(view.id()).orElseThrow().normalizedCode());
        assertEquals(1, historyRepository.saved.size());
        assertEquals(TenantStatus.PROVISIONING, historyRepository.saved.getFirst().toStatus());
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

        service.disableTenant(tenantId, new ChangeTenantStatusCommand("欠费停用", "admin"));
        Tenant disabled = tenantRepository.findById(tenantId).orElseThrow();
        assertEquals(TenantStatus.DISABLED, disabled.status());
        assertEquals("欠费停用", disabled.disabledReason());

        service.enableTenant(tenantId, new ChangeTenantStatusCommand("续费恢复", "admin"));
        assertEquals(TenantStatus.ENABLED, tenantRepository.findById(tenantId).orElseThrow().status());

        service.deleteTenant(tenantId, new DeleteTenantCommand("测试数据清理", "admin"));
        Tenant deleted = tenantRepository.store.get(tenantId);
        assertEquals("测试数据清理", deleted.deleteReason());
        assertTrue(deleted.deletedAt() != null);
        assertEquals(Optional.empty(), tenantRepository.findById(tenantId));
        assertEquals(4, historyRepository.saved.stream().filter(item -> item.tenantId().equals(tenantId)).count());
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

    private static final class InMemoryTenantRepository implements TenantRepository {
        private final Map<Long, Tenant> store = new LinkedHashMap<>();
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
}
