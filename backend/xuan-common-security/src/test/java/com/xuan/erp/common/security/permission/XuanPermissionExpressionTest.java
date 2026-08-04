package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一权限表达式测试，验证 @PreAuthorize 可通过 @xuanPermission 复用同一套授权规则。
 */
class XuanPermissionExpressionTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // 测试普通用户通过远程权限快照判断是否具备指定权限。
    @Test
    void checksPermissionFromSnapshotProvider() {
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of(), 5L, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, "access-token", Set.of()));
        XuanPermissionExpression expression = new XuanPermissionExpression(
                (user, accessToken) -> new PermissionSnapshot(
                        user.tenantId(),
                        user.userId(),
                        user.username(),
                        user.roles(),
                        Set.of("tenant:create"),
                        user.authVersion()));

        assertThat(expression.has("tenant:create")).isTrue();
        assertThat(expression.has("tenant:delete")).isFalse();
        assertThat(expression.hasAny("tenant:update", "tenant:create")).isTrue();
    }

    // 测试 super_admin 不需要远程加载快照，直接拥有所有业务权限。
    @Test
    void letsSuperAdminBypassRemoteSnapshot() {
        AtomicInteger loads = new AtomicInteger();
        CurrentUser currentUser = new CurrentUser(1L, 0L, "super_admin", Set.of("super_admin"), 1L, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, "access-token", Set.of()));
        XuanPermissionExpression expression = new XuanPermissionExpression((user, accessToken) -> {
            loads.incrementAndGet();
            return PermissionSnapshot.empty(user);
        });

        assertThat(expression.has("tenant:delete")).isTrue();
        assertThat(loads).hasValue(0);
    }

    // 测试网关或 IAM 快照下发的 * 通配权限在业务服务 @xuanPermission 中也能生效。
    @Test
    void letsWildcardPermissionBypassRemoteSnapshot() {
        AtomicInteger loads = new AtomicInteger();
        CurrentUser currentUser = new CurrentUser(1L, 0L, "platform-admin", Set.of(), 1L, Set.of("*"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, "access-token", Set.of()));
        XuanPermissionExpression expression = new XuanPermissionExpression((user, accessToken) -> {
            loads.incrementAndGet();
            return PermissionSnapshot.empty(user);
        });

        assertThat(expression.has("iam-role-column-permission:view")).isTrue();
        assertThat(expression.hasAny("tenant:view", "iam-role-column-permission:view")).isTrue();
        assertThat(loads).hasValue(0);
    }
}
