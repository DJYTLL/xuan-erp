package com.xuan.erp.common.security.servlet;

import com.nimbusds.jose.jwk.JWKSet;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import com.xuan.erp.common.security.jwt.jwk.JwkSetUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessJwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // 测试业务服务直接收到 Bearer Token 时，会验签并写入 Spring Security 当前用户上下文。
    @Test
    void authenticatesBearerTokenIntoSecurityContext() throws Exception {
        CurrentUser currentUser = new CurrentUser(
                7L,
                1001L,
                "tenant-admin",
                Set.of("tenant_admin"),
                5L,
                Set.of("tenant:view", "tenant:create"));
        BusinessJwtAuthenticationFilter filter = filter(new StubParser(currentUser));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tenants");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal).isEqualTo(currentUser);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("tenant:view", "tenant:create", "ROLE_tenant_admin");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials())
                .isEqualTo("access-token");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    // 测试 token 无效时直接返回 401，不能让请求继续进入业务 Controller。
    @Test
    void returnsUnauthorizedWhenTokenIsInvalid() throws Exception {
        BusinessJwtAuthenticationFilter filter = filter(new StubParser(new JwtValidationException(
                JwtValidationException.Reason.SIGNATURE_INVALID,
                "bad token")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tenants");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    // 测试 JWK 端点不可用时返回 503，区分认证基础设施故障和普通 token 无效。
    @Test
    void returnsServiceUnavailableWhenJwkInfrastructureIsUnavailable() throws Exception {
        BusinessJwtAuthenticationFilter filter = filter(new StubParser(new JwkSetUnavailableException("jwks down")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tenants");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(chain.getRequest()).isNull();
    }

    private static BusinessJwtAuthenticationFilter filter(JwkJwtTokenParser parser) {
        return new BusinessJwtAuthenticationFilter(new BearerTokenResolver(), parser);
    }

    private static class StubParser extends JwkJwtTokenParser {

        private final CurrentUser currentUser;
        private final RuntimeException failure;

        private StubParser(CurrentUser currentUser) {
            super(new JWKSet(), "issuer", "audience");
            this.currentUser = currentUser;
            this.failure = null;
        }

        private StubParser(RuntimeException failure) {
            super(new JWKSet(), "issuer", "audience");
            this.currentUser = null;
            this.failure = failure;
        }

        @Override
        public CurrentUser parseAccessToken(String token) {
            if (failure != null) {
                throw failure;
            }
            return currentUser;
        }
    }
}
