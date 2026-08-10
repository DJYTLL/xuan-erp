package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.CurrentUserHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/**
 * 状态动作权限 Guard，供业务服务命令入口在进入领域模型前统一校验授权。
 */
public class StateActionPermissionGuard {

    public static final String DENIED_CODE = "SECURITY_STATE_ACTION_DENIED";

    private final PermissionSnapshotProvider permissionSnapshotProvider;

    public StateActionPermissionGuard(PermissionSnapshotProvider permissionSnapshotProvider) {
        this.permissionSnapshotProvider = Objects.requireNonNull(
                permissionSnapshotProvider,
                "permissionSnapshotProvider must not be null");
    }

    /**
     * 要求当前用户在指定资源状态下允许执行动作。
     *
     * @param resourceKey 业务资源标识
     * @param stateCode 状态编码
     * @param actionCode 动作编码
     */
    public void requireAllowed(String resourceKey, String stateCode, String actionCode) {
        CurrentUser currentUser = CurrentUserHolder.current()
                .orElseThrow(() -> new BusinessException(DENIED_CODE, "当前请求未包含登录上下文"));
        PermissionSnapshot snapshot = permissionSnapshotProvider.load(currentUser, accessToken());
        if (!snapshot.isStateActionAllowed(resourceKey, stateCode, actionCode)) {
            throw new BusinessException(DENIED_CODE, "当前状态不允许执行该动作");
        }
    }

    private String accessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getCredentials() instanceof String token)) {
            return null;
        }
        return token;
    }
}
