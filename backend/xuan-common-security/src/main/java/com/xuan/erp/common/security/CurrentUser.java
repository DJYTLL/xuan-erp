package com.xuan.erp.common.security;

import java.util.Set;

public record CurrentUser(Long userId, Long tenantId, String username, Set<String> roles, Long authVersion, Set<String> permissions) {

    public CurrentUser(Long userId, Long tenantId, String username, Set<String> permissions) {
        this(userId, tenantId, username, Set.of(), 0L, permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && (permissions.contains("*") || permissions.contains(permission));
    }
}
