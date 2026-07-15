package com.xuan.erp.common.security.servlet;

import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayIdentityAuthenticationFilterTest {

    private final GatewayIdentityAuthenticationFilter filter = new GatewayIdentityAuthenticationFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void replacesAnonymousAuthenticationWithGatewayIdentityHeaders() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tenants");
        request.addHeader("X-User-Id", "7");
        request.addHeader("X-Tenant-Id", "1001");
        request.addHeader("X-Username", "tenant-admin");
        request.addHeader("X-Roles", "tenant_admin");
        request.addHeader("X-Auth-Version", "5");
        request.addHeader("X-Permissions", "tenant:view,tenant:create");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal).isInstanceOf(CurrentUser.class);
        assertThat(((CurrentUser) principal).username()).isEqualTo("tenant-admin");
    }
}
