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

    private boolean has(CurrentUser currentUser, String permission) {
        if (isSuperAdmin(currentUser) || currentUser.hasPermission("*")) {
            return true;
        }
        return permissionSnapshotProvider.load(currentUser, accessToken()).has(permission);
    }

    private boolean isSuperAdmin(CurrentUser currentUser) {
        return currentUser.roles().contains("super_admin")
                || "super_admin".equals(currentUser.username())
                || "superadmin".equals(currentUser.username());
    }

    private String accessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getCredentials() instanceof String token)) {
            return null;
        }
        return token;
    }
}
