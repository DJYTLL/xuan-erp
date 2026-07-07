package com.xuan.erp.common.security.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TokenTypeTest {

    // 测试 TokenType 枚举存在，并且只定义 ACCESS 和 REFRESH 两种令牌类型。
    @Test
    void definesAccessAndRefreshTokenTypes() {
        assertThatCode(() -> Class.forName("com.xuan.erp.common.security.jwt.TokenType"))
                .doesNotThrowAnyException();

        Class<?> tokenType;
        try {
            tokenType = Class.forName("com.xuan.erp.common.security.jwt.TokenType");
        } catch (ClassNotFoundException ex) {
            throw new AssertionError(ex);
        }

        assertThat(tokenType.isEnum()).isTrue();
        assertThat(tokenType.getEnumConstants())
                .extracting(Object::toString)
                .containsExactly("ACCESS", "REFRESH");
    }
}
