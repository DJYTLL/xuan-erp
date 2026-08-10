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
 * @param dataScopes 当前数据范围规则，支持 `resourceKey:scopeCode` 和全局 `scopeCode`
 * @param stateActionRules 当前状态动作规则，key 使用 `resourceKey:stateCode`，value 为动作编码集合
 * @param authVersion 权限版本
 */
public record PermissionSnapshot(
        Long tenantId,
        Long userId,
        String username,
        Set<String> roles,
        Set<String> permissions,
        Map<String, Map<String, ColumnAccess>> columnPermissions,
        Set<String> dataScopes,
        Map<String, Set<String>> stateActionRules,
        Long authVersion) {

    public PermissionSnapshot {
        roles = immutableCleanSet(roles);
        permissions = immutableCleanSet(permissions);
        columnPermissions = immutableColumnPermissions(columnPermissions);
        dataScopes = immutableCleanSet(dataScopes);
        stateActionRules = immutableStateActionRules(stateActionRules);
    }

    public PermissionSnapshot(
            Long tenantId,
            Long userId,
            String username,
            Set<String> roles,
            Set<String> permissions,
            Map<String, Map<String, ColumnAccess>> columnPermissions,
            Long authVersion) {
        this(tenantId, userId, username, roles, permissions, columnPermissions, Set.of(), Map.of(), authVersion);
    }

    public PermissionSnapshot(
            Long tenantId,
            Long userId,
            String username,
            Set<String> roles,
            Set<String> permissions,
            Long authVersion) {
        this(tenantId, userId, username, roles, permissions, Map.of(), Set.of(), Map.of(), authVersion);
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
     * 读取当前用户对指定业务资源可用的数据范围。
     *
     * @param resourceKey 业务资源标识，例如 sales-order
     * @return 数据范围编码集合，例如 SELF、DEPARTMENT、TENANT
     */
    public Set<String> dataScopes(String resourceKey) {
        if (isSuperAdmin()) {
            return Set.of("*");
        }
        if (!hasText(resourceKey) || dataScopes.isEmpty()) {
            return Set.of();
        }
        String normalizedResourceKey = resourceKey.trim();
        Set<String> result = new LinkedHashSet<>();
        for (String value : dataScopes) {
            DataScopeKey key = DataScopeKey.parse(value);
            if (key == null) {
                continue;
            }
            if (key.resourceKey() == null || "*".equals(key.resourceKey()) || normalizedResourceKey.equals(key.resourceKey())) {
                result.add(key.scopeCode());
            }
        }
        return Set.copyOf(result);
    }

    /**
     * 判断当前用户是否拥有指定业务资源的数据范围。
     *
     * @param resourceKey 业务资源标识
     * @param scopeCode 数据范围编码
     * @return 是否拥有
     */
    public boolean hasDataScope(String resourceKey, String scopeCode) {
        if (isSuperAdmin()) {
            return true;
        }
        if (!hasText(scopeCode)) {
            return false;
        }
        Set<String> scopes = dataScopes(resourceKey);
        String normalizedScopeCode = scopeCode.trim();
        return scopes.contains("*") || scopes.contains(normalizedScopeCode);
    }

    /**
     * 读取当前用户在指定资源状态下允许执行的动作。
     *
     * @param resourceKey 业务资源标识
     * @param stateCode 状态编码
     * @return 动作编码集合
     */
    public Set<String> allowedActions(String resourceKey, String stateCode) {
        if (isSuperAdmin()) {
            return Set.of("*");
        }
        StateActionKey key = StateActionKey.of(resourceKey, stateCode);
        if (key == null) {
            return Set.of();
        }
        Set<String> actions = stateActionRules.get(key.value());
        if (actions != null) {
            return actions;
        }
        Set<String> wildcardActions = stateActionRules.get(key.resourceKey() + ":*");
        return wildcardActions == null ? Set.of() : wildcardActions;
    }

    /**
     * 判断当前用户在指定资源状态下是否允许执行动作。
     *
     * @param resourceKey 业务资源标识
     * @param stateCode 状态编码
     * @param actionCode 动作编码
     * @return 是否允许
     */
    public boolean isStateActionAllowed(String resourceKey, String stateCode, String actionCode) {
        if (isSuperAdmin()) {
            return true;
        }
        if (!hasText(actionCode)) {
            return false;
        }
        Set<String> actions = allowedActions(resourceKey, stateCode);
        String normalizedActionCode = actionCode.trim();
        return actions.contains("*") || actions.contains(normalizedActionCode);
    }

    /**
     * 判断当前快照是否为超级管理员。
     *
     * @return 是否超级管理员
     */
    public boolean isSuperAdmin() {
        return Long.valueOf(0L).equals(tenantId)
                && (roles.contains("super_admin")
                || "super_admin".equals(username)
                || "superadmin".equals(username));
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

    private static Map<String, Set<String>> immutableStateActionRules(Map<String, Set<String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> cleaned = new LinkedHashMap<>();
        values.forEach((key, actions) -> {
            StateActionKey stateActionKey = StateActionKey.parse(key);
            if (stateActionKey == null || actions == null || actions.isEmpty()) {
                return;
            }
            Set<String> cleanedActions = immutableCleanSet(actions);
            if (!cleanedActions.isEmpty()) {
                cleaned.put(stateActionKey.value(), cleanedActions);
            }
        });
        return Map.copyOf(cleaned);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record DataScopeKey(String resourceKey, String scopeCode) {

        private static DataScopeKey parse(String value) {
            if (!hasText(value)) {
                return null;
            }
            String normalized = value.trim();
            int splitIndex = normalized.indexOf(':');
            if (splitIndex < 0) {
                return new DataScopeKey(null, normalized);
            }
            String resourceKey = normalized.substring(0, splitIndex).trim();
            String scopeCode = normalized.substring(splitIndex + 1).trim();
            if (!hasText(resourceKey) || !hasText(scopeCode)) {
                return null;
            }
            return new DataScopeKey(resourceKey, scopeCode);
        }
    }

    private record StateActionKey(String resourceKey, String stateCode) {

        private String value() {
            return resourceKey + ":" + stateCode;
        }

        private static StateActionKey of(String resourceKey, String stateCode) {
            if (!hasText(resourceKey) || !hasText(stateCode)) {
                return null;
            }
            return new StateActionKey(resourceKey.trim(), stateCode.trim());
        }

        private static StateActionKey parse(String value) {
            if (!hasText(value)) {
                return null;
            }
            String normalized = value.trim();
            int splitIndex = normalized.indexOf(':');
            if (splitIndex < 0) {
                return null;
            }
            return of(normalized.substring(0, splitIndex), normalized.substring(splitIndex + 1));
        }
    }
}
