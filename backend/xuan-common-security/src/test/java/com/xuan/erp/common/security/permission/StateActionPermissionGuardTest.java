package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 状态动作权限 Guard 测试，验证业务服务命令入口可以复用统一拒绝逻辑。
 */
class StateActionPermissionGuardTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // 测试状态动作允许时不抛异常，业务服务可以继续进入领域模型。
    @Test
    void allowsConfiguredStateAction() {
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "operator", Set.of(), 3L, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, "access-token", Set.of()));
        StateActionPermissionGuard guard = new StateActionPermissionGuard(
                (user, accessToken) -> new PermissionSnapshot(
                        user.tenantId(),
                        user.userId(),
                        user.username(),
                        user.roles(),
                        Set.of(),
                        Map.of(),
                        Set.of(),
                        Map.of("sales-order:DRAFT", Set.of("submit")),
                        user.authVersion()));

        assertThatCode(() -> guard.requireAllowed("sales-order", "DRAFT", "submit"))
                .doesNotThrowAnyException();
    }

    // 测试状态动作未授权时抛统一业务异常，方便业务服务返回一致的 403 语义。
    @Test
    void deniesUnconfiguredStateAction() {
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "operator", Set.of(), 3L, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, "access-token", Set.of()));
        StateActionPermissionGuard guard = new StateActionPermissionGuard(
                (user, accessToken) -> PermissionSnapshot.empty(user));

        assertThatThrownBy(() -> guard.requireAllowed("sales-order", "APPROVED", "delete"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("SECURITY_STATE_ACTION_DENIED");
    }
}
