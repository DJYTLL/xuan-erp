package com.xuan.erp.common.security.jwt;

import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenParserTest {

    // 测试通用 JwtTokenParser 会调用 JwtDecoder，并把 access token 解析成 CurrentUser。
    @Test
    void parsesAccessTokenIntoCurrentUser() {
        JwtDecoder jwtDecoder = token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("userId", 1001L)
                .claim("tenantId", 2002L)
                .claim("username", "zhangsan")
                .claim("roles", List.of("tenant_admin", "sales_manager"))
                .claim("authVersion", 7L)
                .claim("permissions", List.of("sales:view", "sales:create"))
                .build();
        JwtTokenParser parser = new JwtTokenParser(jwtDecoder);

        CurrentUser currentUser = parser.parseAccessToken("access-token");

        assertThat(currentUser.userId()).isEqualTo(1001L);
        assertThat(currentUser.tenantId()).isEqualTo(2002L);
        assertThat(currentUser.username()).isEqualTo("zhangsan");
        assertThat(currentUser.roles()).containsExactlyInAnyOrder("tenant_admin", "sales_manager");
        assertThat(currentUser.authVersion()).isEqualTo(7L);
        assertThat(currentUser.permissions()).containsExactlyInAnyOrder("sales:view", "sales:create");
    }
}
