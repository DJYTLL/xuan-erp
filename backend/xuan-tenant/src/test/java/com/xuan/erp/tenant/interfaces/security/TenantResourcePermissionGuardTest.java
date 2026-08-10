package com.xuan.erp.tenant.interfaces.security;

import com.xuan.erp.common.security.permission.XuanPermissionExpression;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TenantResourcePermissionGuardTest {

    private final TenantResourcePermissionGuard guard = new TenantResourcePermissionGuard(new XuanPermissionExpression(
            (user, accessToken) -> {
                throw new UnsupportedOperationException("mapping test should not load permission snapshot");
            }));

    @Test
    void mapsReadPermissionsByResourceName() {
        assertEquals(Optional.of("tenant-domain:view"), guard.requiredPermission("tenant-domains", TenantResourcePermissionGuard.Action.READ));
        assertEquals(Optional.of("tenant-contact:view"), guard.requiredPermission("tenant-contacts", TenantResourcePermissionGuard.Action.READ));
        assertEquals(Optional.of("tenant-plan:view"), guard.requiredPermission("tenant-plans", TenantResourcePermissionGuard.Action.READ));
        assertEquals(Optional.of("tenant-plan:view"), guard.requiredPermission("tenant-plan-assignments", TenantResourcePermissionGuard.Action.READ));
        assertEquals(Optional.of("tenant-config:view"), guard.requiredPermission("tenant-configs", TenantResourcePermissionGuard.Action.READ));
        assertEquals(Optional.of("tenant-provision:view"), guard.requiredPermission("tenant-provision-tasks", TenantResourcePermissionGuard.Action.READ));
        assertEquals(Optional.of("tenant-provision:view"), guard.requiredPermission("tenant-outbox-events", TenantResourcePermissionGuard.Action.READ));
    }

    @Test
    void mapsWritePermissionsByResourceName() {
        assertEquals(Optional.of("tenant-domain:manage"), guard.requiredPermission("tenant-domains", TenantResourcePermissionGuard.Action.CREATE));
        assertEquals(Optional.of("tenant-contact:manage"), guard.requiredPermission("tenant-contacts", TenantResourcePermissionGuard.Action.UPDATE));
        assertEquals(Optional.of("tenant-domain:manage"), guard.requiredPermission("tenant-domains", TenantResourcePermissionGuard.Action.DELETE));
    }

    @Test
    void rejectsWriteAccessForResourcesThatMustGoThroughDedicatedApplicationServices() {
        assertFalse(guard.requiredPermission("tenants", TenantResourcePermissionGuard.Action.CREATE).isPresent());
        assertFalse(guard.requiredPermission("tenant-plans", TenantResourcePermissionGuard.Action.UPDATE).isPresent());
        assertFalse(guard.requiredPermission("tenant-plan-assignments", TenantResourcePermissionGuard.Action.DELETE).isPresent());
        assertFalse(guard.requiredPermission("tenant-configs", TenantResourcePermissionGuard.Action.DELETE).isPresent());
        assertFalse(guard.requiredPermission("tenant-provision-task-steps", TenantResourcePermissionGuard.Action.UPDATE).isPresent());
    }

    @Test
    void rejectsUnsupportedResourceNames() {
        assertFalse(guard.requiredPermission("unknown-resource", TenantResourcePermissionGuard.Action.READ).isPresent());
    }
}
