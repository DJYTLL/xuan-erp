package com.xuan.erp.iam.infrastructure.security;

import com.xuan.erp.iam.application.port.IamRefreshTokenGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 基于 SecureRandom 的不透明 refresh token 生成器。
 */
@Component
public class SecureRandomIamRefreshTokenGenerator implements IamRefreshTokenGenerator {

    private static final int TOKEN_BYTES = 32;
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }
}
