package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.security.CurrentUser;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 业务服务授权判断使用的当前权限快照。
 *
 * @param tenantId 权限所属租户
 * @param userId 用户 ID
 * @param username 用户名
 * @param roles 当前角色编码集合
 * @param permissions 当前权限编码集合
 * @param columnPermissions 当前列权限规则
 * @param authVersion 权限版本
 */
public record PermissionSnapshot(
        Long tenantId,
        Long userId,
        String username,
        Set<String> roles,
        Set<String> permissions,
        Map<String, Map<String, ColumnAccess>> columnPermissions,
        Long authVersion) {

    public PermissionSnapshot {
        roles = immutableCleanSet(roles);
        permissions = immutableCleanSet(permissions);
        columnPermissions = immutableColumnPermissions(columnPermissions);
    }

    public PermissionSnapshot(
            Long tenantId,
            Long userId,
            String username,
            Set<String> roles,
            Set<String> permissions,
            Long authVersion) {
        this(tenantId, userId, username, roles, permissions, Map.of(), authVersion);
    }

    /**
     * 基于当前用户构建空权限快照，常用于测试或无权限兜底。
     *
     * @param currentUser 当前用户
     * @return 空权限快照
     */
    public static PermissionSnapshot empty(CurrentUser currentUser) {
        return new PermissionSnapshot(
                currentUser.tenantId(),
                currentUser.userId(),
                currentUser.username(),
                currentUser.roles(),
                Set.of(),
                Map.of(),
                currentUser.authVersion());
    }

    /**
     * 判断是否拥有指定权限。
     *
     * @param permission 权限编码
     * @return 是否允许
     */
    public boolean has(String permission) {
        return hasText(permission)
                && (isSuperAdmin() || permissions.contains("*") || permissions.contains(permission.trim()));
    }

    /**
     * 判断是否拥有任意一个权限。
     *
     * @param permissions 权限编码列表
     * @return 是否允许
     */
    public boolean hasAny(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        return Arrays.stream(permissions).anyMatch(this::has);
    }

    public ColumnAccess columnAccess(String resourceKey, String columnKey) {
        if (!hasText(resourceKey) || !hasText(columnKey)) {
            return ColumnAccess.HIDDEN;
        }
        Map<String, ColumnAccess> resourceRules = columnPermissions.get(resourceKey.trim());
        if (resourceRules == null || resourceRules.isEmpty()) {
            return ColumnAccess.VISIBLE;
        }
        return resourceRules.getOrDefault(columnKey.trim(), ColumnAccess.HIDDEN);
    }

    public boolean hasColumnRules(String resourceKey) {
        return hasText(resourceKey) && columnPermissions.containsKey(resourceKey.trim());
    }

    /**
     * 判断当前快照是否为超级管理员。
     *
     * @return 是否超级管理员
     */
    public boolean isSuperAdmin() {
        return roles.contains("super_admin")
                || "super_admin".equals(username)
                || "superadmin".equals(username);
    }

    private static Set<String> immutableCleanSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> cleaned = new LinkedHashSet<>();
        values.stream()
                .filter(PermissionSnapshot::hasText)
                .map(String::trim)
                .forEach(cleaned::add);
        return Set.copyOf(cleaned);
    }

    private static Map<String, Map<String, ColumnAccess>> immutableColumnPermissions(
            Map<String, Map<String, ColumnAccess>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, ColumnAccess>> cleaned = new LinkedHashMap<>();
        values.forEach((resourceKey, columns) -> {
            if (!hasText(resourceKey) || columns == null || columns.isEmpty()) {
                return;
            }
            Map<String, ColumnAccess> cleanedColumns = new LinkedHashMap<>();
            columns.forEach((columnKey, access) -> {
                if (hasText(columnKey)) {
                    cleanedColumns.put(columnKey.trim(), access == null ? ColumnAccess.HIDDEN : access);
                }
            });
            if (!cleanedColumns.isEmpty()) {
                cleaned.put(resourceKey.trim(), Map.copyOf(cleanedColumns));
            }
        });
        return Map.copyOf(cleaned);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
