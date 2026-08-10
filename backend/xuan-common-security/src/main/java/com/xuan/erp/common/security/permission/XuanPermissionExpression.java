package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.CurrentUserHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Objects;

/**
 * 统一的业务权限表达式，供 {@code @PreAuthorize("@xuanPermission.has('xxx')")} 使用。
 */
public class XuanPermissionExpression {

    private final PermissionSnapshotProvider permissionSnapshotProvider;

    public XuanPermissionExpression(PermissionSnapshotProvider permissionSnapshotProvider) {
        this.permissionSnapshotProvider = Objects.requireNonNull(
                permissionSnapshotProvider,
                "permissionSnapshotProvider must not be null");
    }

    /**
     * 判断当前用户是否拥有指定权限。
     *
     * @param permission 权限编码
     * @return 是否允许访问
     */
    public boolean has(String permission) {
        return CurrentUserHolder.current()
                .map(currentUser -> has(currentUser, permission))
                .orElse(false);
    }

    /**
     * 判断当前用户是否拥有任意一个权限。
     *
     * @param permissions 权限编码列表
     * @return 是否允许访问
     */
    public boolean hasAny(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        return Arrays.stream(permissions).anyMatch(this::has);
    }

    /**
     * 判断当前用户在指定资源状态下是否允许执行动作。
     *
     * @param resourceKey 业务资源标识
     * @param stateCode 状态编码
     * @param actionCode 动作编码
     * @return 是否允许执行
     */
    public boolean canStateAction(String resourceKey, String stateCode, String actionCode) {
        return CurrentUserHolder.current()
                .map(currentUser -> canStateAction(currentUser, resourceKey, stateCode, actionCode))
                .orElse(false);
    }

    /**
     * 同时判断基础权限和状态动作权限，供命令入口的 {@code @PreAuthorize} 复用。
     *
     * @param permission 基础权限编码
     * @param resourceKey 业务资源标识
     * @param stateCode 状态编码
     * @param actionCode 动作编码
     * @return 是否允许访问
     */
    public boolean hasAndCanStateAction(String permission, String resourceKey, String stateCode, String actionCode) {
        return CurrentUserHolder.current()
                .map(currentUser -> has(currentUser, permission)
                        && canStateAction(currentUser, resourceKey, stateCode, actionCode))
                .orElse(false);
    }

    private boolean has(CurrentUser currentUser, String permission) {
        if (isSuperAdmin(currentUser) || currentUser.hasPermission("*")) {
            return true;
        }
        return permissionSnapshotProvider.load(currentUser, accessToken()).has(permission);
    }

    private boolean canStateAction(CurrentUser currentUser, String resourceKey, String stateCode, String actionCode) {
        if (isSuperAdmin(currentUser) || currentUser.hasPermission("*")) {
            return true;
        }
        return permissionSnapshotProvider.load(currentUser, accessToken())
                .isStateActionAllowed(resourceKey, stateCode, actionCode);
    }

    private boolean isSuperAdmin(CurrentUser currentUser) {
        return Long.valueOf(0L).equals(currentUser.tenantId())
                && (currentUser.roles().contains("super_admin")
                || "super_admin".equals(currentUser.username())
                || "superadmin".equals(currentUser.username()));
    }

    private String accessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getCredentials() instanceof String token)) {
            return null;
        }
        return token;
    }
}
