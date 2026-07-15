package com.xuan.erp.iam.infrastructure.config;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.iam.application.port.IamAccessTokenIssuer;
import java.text.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IAM JWT 配置，负责签名 JWK、验签解析器与访问令牌签发器装配。
 */
@Configuration
@EnableConfigurationProperties({IamJwtProperties.class, IamAuthProperties.class})
public class IamJwtConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IamJwtConfiguration.class);

    @Bean
    RSAKey iamSigningRsaKey(IamJwtProperties properties) throws ParseException {
        if (properties.getSigningJwkJson() != null && !properties.getSigningJwkJson().isBlank()) {
            return RSAKey.parse(properties.getSigningJwkJson());
        }
        try {
            log.warn("xuan.iam.jwt.signing-jwk-json 未配置，IAM 将使用本地临时 JWK 签发令牌；服务重启后旧 token 会失效。正式环境请在 xuan-iam.yaml 的 Nacos 配置中固定该项。");
            return new RSAKeyGenerator(2048)
                    .keyID(properties.getKeyId())
                    .generate();
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成 IAM 默认 RSA JWK", ex);
        }
    }

    @Bean
    JwkJwtTokenParser iamJwkJwtTokenParser(RSAKey rsaKey, IamJwtProperties properties) {
        return new JwkJwtTokenParser(
                new com.nimbusds.jose.jwk.JWKSet(rsaKey.toPublicJWK()),
                properties.getIssuer(),
                properties.getAudience());
    }

    @Bean
    IamAccessTokenIssuer iamAccessTokenIssuer(RSAKey rsaKey, IamJwtProperties properties) {
        return new com.xuan.erp.iam.infrastructure.security.IamAccessTokenIssuer(
                rsaKey,
                properties.getIssuer(),
                properties.getAudience(),
                properties.getAccessTokenTtl());
    }
}
