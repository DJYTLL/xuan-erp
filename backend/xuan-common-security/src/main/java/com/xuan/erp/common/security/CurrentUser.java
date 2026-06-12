package com.xuan.erp.common.security;

import java.util.Set;

public record CurrentUser(Long userId, Long tenantId, String username, Set<String> permissions) {

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
