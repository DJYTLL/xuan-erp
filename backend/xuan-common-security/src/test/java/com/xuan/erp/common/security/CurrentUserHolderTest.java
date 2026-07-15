package com.xuan.erp.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserHolderTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // 测试当前请求已经认证时，可以从 SecurityContext 中读取 CurrentUser。
    @Test
    void returnsCurrentUserFromSecurityContext() {
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of("tenant_admin"), 5L, Set.of("tenant:view"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, "N/A", Set.of()));

        assertThat(CurrentUserHolder.current()).contains(currentUser);
        assertThat(CurrentUserHolder.required()).isEqualTo(currentUser);
        assertThat(CurrentUserHolder.usernameOrSystem()).isEqualTo("tenant-admin");
    }

    // 测试没有当前登录人时，required 明确失败，而审计操作人可回退为 system。
    @Test
    void failsClearlyWhenCurrentUserIsMissing() {
        assertThat(CurrentUserHolder.current()).isEmpty();
        assertThat(CurrentUserHolder.usernameOrSystem()).isEqualTo("system");
        assertThatThrownBy(CurrentUserHolder::required)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("当前登录用户不存在");
    }
}
