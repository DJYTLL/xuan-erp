package com.xuan.erp.tenant;

import com.xuan.erp.tenant.application.service.TenantResourceApplicationService;
import com.xuan.erp.tenant.domain.service.TenantResourceCatalog;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantResourceCatalogTest {

    @Test
    void registersCrudResourcesForEveryTenantTable() {
        TenantResourceCatalog catalog = TenantResourceCatalog.defaultCatalog();

        assertEquals(10, catalog.resources().size());
        assertTrue(catalog.find("tenants").isPresent());
        assertTrue(catalog.find("tenant-plans").isPresent());
        assertTrue(catalog.find("tenant-plan-assignments").isPresent());
        assertTrue(catalog.find("tenant-domains").isPresent());
        assertTrue(catalog.find("tenant-contacts").isPresent());
        assertTrue(catalog.find("tenant-status-histories").isPresent());
        assertTrue(catalog.find("tenant-configs").isPresent());
        assertTrue(catalog.find("tenant-provision-tasks").isPresent());
        assertTrue(catalog.find("tenant-provision-task-steps").isPresent());
        assertTrue(catalog.find("tenant-outbox-events").isPresent());
    }

    @Test
    void exposesGenericCrudServiceForWhitelistedResources() {
        RecordingTenantResourceRepository repository = new RecordingTenantResourceRepository();
        TenantResourceApplicationService service = new TenantResourceApplicationService(
                TenantResourceCatalog.defaultCatalog(),
                repository
        );

        service.create("tenant-configs", Map.of("tenantId", 1L, "configKey", "system.name", "configValue", "Xuan ERP"));
        service.update("tenant-configs", 10L, Map.of("configValue", "Xuan ERP Pro"));
        service.delete("tenant-configs", 10L, "清理无效配置", "admin");

        assertEquals("tenant_config", repository.lastResource().tableName());
        assertEquals("清理无效配置", repository.lastDeleteReason());
    }
}
