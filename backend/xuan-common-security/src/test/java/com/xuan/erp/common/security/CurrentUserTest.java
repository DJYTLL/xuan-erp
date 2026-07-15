package com.xuan.erp.common.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserTest {

    @Test
    void wildcardPermissionMatchesAnyPermission() {
        CurrentUser currentUser = new CurrentUser(
                1L,
                0L,
                "superadmin",
                Set.of("super_admin"),
                1L,
                Set.of("*"));

        assertTrue(currentUser.hasPermission("tenant-plan:manage"));
    }

    @Test
    void missingPermissionDoesNotMatchWithoutWildcard() {
        CurrentUser currentUser = new CurrentUser(
                2L,
                1001L,
                "tenant-admin",
                Set.of("tenant_admin"),
                1L,
                Set.of("tenant:view"));

        assertFalse(currentUser.hasPermission("tenant-plan:manage"));
    }
}
