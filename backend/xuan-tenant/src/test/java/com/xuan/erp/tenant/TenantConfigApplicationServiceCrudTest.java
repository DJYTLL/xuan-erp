package com.xuan.erp.tenant;

import com.xuan.erp.common.api.PageResult;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.CreateTenantConfigCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantConfigCommand;
import com.xuan.erp.tenant.application.query.TenantConfigDetailView;
import com.xuan.erp.tenant.application.service.TenantConfigApplicationService;
import com.xuan.erp.tenant.domain.model.TenantConfig;
import com.xuan.erp.tenant.domain.model.type.ConfigValueType;
import com.xuan.erp.tenant.domain.repository.TenantConfigRepository;
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

class TenantConfigApplicationServiceCrudTest {

    @Test
    void createsInternalConfigAndExposesOnlyPublicConfigs() {
        InMemoryTenantConfigRepository repository = new InMemoryTenantConfigRepository();
        TenantConfigApplicationService service = new TenantConfigApplicationService(repository);

        TenantConfigDetailView internal = service.createConfig(new CreateTenantConfigCommand(
                1L, "sms.secret", "top-secret", "string", "短信密钥", false, true, true));
        TenantConfigDetailView publicConfig = service.createConfig(new CreateTenantConfigCommand(
                1L, "brand.name", "玄云 ERP", "string", "品牌名称", true, false, false));

        PageResult<TenantConfigDetailView> page = service.listConfigs(1L, 1, 10);
        List<TenantConfigDetailView> publicConfigs = service.listPublicConfigs(1L);

        assertEquals(2, page.total());
        assertEquals("******", internal.configValue());
        assertEquals(List.of(publicConfig.configKey()), publicConfigs.stream().map(TenantConfigDetailView::configKey).toList());
        assertEquals("玄云 ERP", publicConfigs.getFirst().configValue());
    }

    @Test
    void rejectsPublicSensitiveConfigCombination() {
        TenantConfigApplicationService service = new TenantConfigApplicationService(new InMemoryTenantConfigRepository());

        BusinessException error = assertThrows(BusinessException.class, () -> service.createConfig(new CreateTenantConfigCommand(
                1L, "unsafe.key", "secret", "string", "不安全配置", true, true, false)));

        assertEquals("TENANT_CONFIG_BOUNDARY_INVALID", error.code());
    }

    @Test
    void updatesAndSoftDeletesConfigWithReason() {
        InMemoryTenantConfigRepository repository = new InMemoryTenantConfigRepository();
        TenantConfigApplicationService service = new TenantConfigApplicationService(repository);
        Long configId = service.createConfig(new CreateTenantConfigCommand(
                1L, "brand.name", "玄云 ERP", "string", "品牌名称", true, false, false)).id();

        TenantConfigDetailView updated = service.updateConfig(configId, new UpdateTenantConfigCommand(
                "玄云 ERP Pro", "string", "品牌名称升级", true, false, false));
        service.deleteConfig(configId, "配置废弃", "admin");

        assertEquals("玄云 ERP Pro", updated.configValue());
        assertTrue(repository.store.get(configId).deletedAt() != null);
        assertEquals("配置废弃", repository.store.get(configId).deleteReason());
    }

    @Test
    void updatesConfigByTenantIdAndConfigKey() {
        InMemoryTenantConfigRepository repository = new InMemoryTenantConfigRepository();
        TenantConfigApplicationService service = new TenantConfigApplicationService(repository);
        service.createConfig(new CreateTenantConfigCommand(
                1L, "brand.name", "玄云 ERP", "string", "品牌名称", true, false, false));

        TenantConfigDetailView updated = service.updateConfig(1L, "brand.name", new UpdateTenantConfigCommand(
                "玄云 ERP Max", "string", "品牌名称升级", true, false, false));

        assertEquals("玄云 ERP Max", updated.configValue());
        assertEquals("brand.name", updated.configKey());
        assertEquals(1L, updated.tenantId());
    }

    private static final class InMemoryTenantConfigRepository implements TenantConfigRepository {
        private final Map<Long, TenantConfig> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<TenantConfig> findById(Long id) {
            return Optional.ofNullable(store.get(id)).filter(item -> item.deletedAt() == null);
        }

        @Override
        public Optional<TenantConfig> findActiveByTenantIdAndKey(Long tenantId, String configKey) {
            return store.values().stream()
                    .filter(item -> item.deletedAt() == null)
                    .filter(item -> item.tenantId().equals(tenantId))
                    .filter(item -> item.configKey().equals(configKey))
                    .findFirst();
        }

        @Override
        public List<TenantConfig> findByTenantId(Long tenantId, long offset, long limit) {
            return store.values().stream()
                    .filter(item -> item.deletedAt() == null)
                    .filter(item -> item.tenantId().equals(tenantId))
                    .sorted(Comparator.comparing(TenantConfig::id))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countByTenantId(Long tenantId) {
            return store.values().stream()
                    .filter(item -> item.deletedAt() == null)
                    .filter(item -> item.tenantId().equals(tenantId))
                    .count();
        }

        @Override
        public List<TenantConfig> findPublicByTenantId(Long tenantId) {
            return store.values().stream()
                    .filter(item -> item.deletedAt() == null)
                    .filter(item -> item.tenantId().equals(tenantId))
                    .filter(TenantConfig::publicReadable)
                    .sorted(Comparator.comparing(TenantConfig::id))
                    .toList();
        }

        @Override
        public TenantConfig save(TenantConfig tenantConfig) {
            Long id = tenantConfig.id() == null ? nextId++ : tenantConfig.id();
            TenantConfig saved = new TenantConfig(
                    id,
                    tenantConfig.tenantId(),
                    tenantConfig.configKey(),
                    tenantConfig.configValue(),
                    tenantConfig.valueType(),
                    tenantConfig.description(),
                    tenantConfig.publicConfig(),
                    tenantConfig.sensitive(),
                    tenantConfig.encrypted(),
                    tenantConfig.createdBy(),
                    tenantConfig.createdAt() == null ? OffsetDateTime.now() : tenantConfig.createdAt(),
                    tenantConfig.updatedBy(),
                    tenantConfig.updatedAt() == null ? OffsetDateTime.now() : tenantConfig.updatedAt(),
                    tenantConfig.deletedBy(),
                    tenantConfig.deleteReason(),
                    tenantConfig.deletedAt()
            );
            store.put(id, saved);
            return saved;
        }
    }
}
