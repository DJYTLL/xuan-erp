package com.xuan.erp.common.security.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BearerTokenResolverTest {

    // 测试能从标准 Authorization: Bearer xxx 请求头中提取原始 token。
    @Test
    void resolvesBearerTokenFromAuthorizationHeader() {
        BearerTokenResolver resolver = new BearerTokenResolver();

        assertThat(resolver.resolve("Bearer access-token")).contains("access-token");
    }

    // 测试空请求头或非 Bearer 请求头不会被误识别为有效 token。
    @Test
    void ignoresBlankOrNonBearerAuthorizationHeader() {
        BearerTokenResolver resolver = new BearerTokenResolver();

        assertThat(resolver.resolve(null)).isEmpty();
        assertThat(resolver.resolve("")).isEmpty();
        assertThat(resolver.resolve("Basic abc")).isEmpty();
    }
}
