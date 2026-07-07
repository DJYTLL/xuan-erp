package com.xuan.erp.common.security.jwt;

import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtClaimsTest {

    // 测试 JwtClaims 只从 JWT 中提取允许进入业务层的轻量身份字段。
    @Test
    void createsJwtClaimsFromAllowedIdentityFields() {
        Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .claim(JwtClaims.CLAIM_USER_ID, 1001L)
                .claim(JwtClaims.CLAIM_TENANT_ID, 2002L)
                .claim(JwtClaims.CLAIM_USERNAME, "zhangsan")
                .claim(JwtClaims.CLAIM_ROLES, List.of("tenant_admin", "sales_manager"))
                .claim(JwtClaims.CLAIM_AUTH_VERSION, 7L)
                .claim(JwtClaims.CLAIM_PERMISSIONS, List.of("sales:view", "sales:create"))
                .claim("email", "zhangsan@example.com")
                .build();

        JwtClaims claims = JwtClaims.from(jwt);

        assertThat(claims.userId()).isEqualTo(1001L);
        assertThat(claims.tenantId()).isEqualTo(2002L);
        assertThat(claims.username()).isEqualTo("zhangsan");
        assertThat(claims.roles()).containsExactlyInAnyOrder("tenant_admin", "sales_manager");
        assertThat(claims.authVersion()).isEqualTo(7L);
        assertThat(claims.permissions()).containsExactlyInAnyOrder("sales:view", "sales:create");
    }

    // 测试 JwtClaims 可以转换成业务侧使用的 CurrentUser 上下文。
    @Test
    void convertsJwtClaimsToCurrentUser() {
        JwtClaims claims = new JwtClaims(1001L, 2002L, "zhangsan",
                List.of("tenant_admin"), 7L, List.of("sales:view"));

        CurrentUser currentUser = claims.toCurrentUser();

        assertThat(currentUser.userId()).isEqualTo(1001L);
        assertThat(currentUser.tenantId()).isEqualTo(2002L);
        assertThat(currentUser.username()).isEqualTo("zhangsan");
        assertThat(currentUser.roles()).containsExactly("tenant_admin");
        assertThat(currentUser.authVersion()).isEqualTo(7L);
        assertThat(currentUser.permissions()).containsExactly("sales:view");
    }
}
