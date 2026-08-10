package com.xuan.erp.tenant.interfaces.security;

import com.xuan.erp.common.security.permission.XuanPermissionExpression;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 按通用租户资源名称映射最终权限码，避免通用资源接口绕过专用控制器的权限边界。
 */
@Component("tenantResourcePermissionGuard")
public class TenantResourcePermissionGuard {

    private final XuanPermissionExpression xuanPermission;

    public TenantResourcePermissionGuard(XuanPermissionExpression xuanPermission) {
        this.xuanPermission = Objects.requireNonNull(xuanPermission, "xuanPermission must not be null");
    }

    public boolean canRead(String resourceName) {
        return has(resourceName, Action.READ);
    }

    public boolean canCreate(String resourceName) {
        return has(resourceName, Action.CREATE);
    }

    public boolean canUpdate(String resourceName) {
        return has(resourceName, Action.UPDATE);
    }

    public boolean canDelete(String resourceName) {
        return has(resourceName, Action.DELETE);
    }

    boolean has(String resourceName, Action action) {
        return requiredPermission(resourceName, action)
                .map(xuanPermission::has)
                .orElse(false);
    }

    Optional<String> requiredPermission(String resourceName, Action action) {
        if (resourceName == null || resourceName.isBlank() || action == null) {
            return Optional.empty();
        }
        return switch (resourceName.trim()) {
            case "tenants" -> tenantPermission(action);
            case "tenant-plans" -> readOnlyPermission(action, "tenant-plan:view");
            case "tenant-plan-assignments" -> readOnlyPermission(action, "tenant-plan:view");
            case "tenant-domains" -> readWritePermission(action, "tenant-domain:view", "tenant-domain:manage");
            case "tenant-contacts" -> readWritePermission(action, "tenant-contact:view", "tenant-contact:manage");
            case "tenant-status-histories" -> readOnlyPermission(action, "tenant:view");
            case "tenant-configs" -> readOnlyPermission(action, "tenant-config:view");
            case "tenant-provision-tasks" -> readOnlyPermission(action, "tenant-provision:view");
            case "tenant-provision-task-steps" -> readOnlyPermission(action, "tenant-provision:view");
            case "tenant-outbox-events" -> readOnlyPermission(action, "tenant-provision:view");
            default -> Optional.empty();
        };
    }

    private Optional<String> tenantPermission(Action action) {
        return switch (action) {
            case READ -> Optional.of("tenant:view");
            case CREATE, UPDATE, DELETE -> Optional.empty();
        };
    }

    private Optional<String> readOnlyPermission(Action action, String readPermission) {
        return action == Action.READ ? Optional.of(readPermission) : Optional.empty();
    }

    private Optional<String> readWritePermission(Action action, String readPermission, String writePermission) {
        return switch (action) {
            case READ -> Optional.of(readPermission);
            case CREATE, UPDATE, DELETE -> Optional.of(writePermission);
        };
    }

    enum Action {
        READ,
        CREATE,
        UPDATE,
        DELETE
    }
}
