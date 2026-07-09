package com.xuan.erp.iam.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IAM JWT 配置属性，定义签发方、受众、过期时间和可选静态 JWK。
 */
@ConfigurationProperties(prefix = "xuan.iam.jwt")
public class IamJwtProperties {

    private String issuer = "xuan-iam";
    private String audience = "xuan-gateway";
    private Duration accessTokenTtl = Duration.ofHours(2);
    private String keyId = "xuan-iam-local";
    private String signingJwkJson;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getSigningJwkJson() {
        return signingJwkJson;
    }

    public void setSigningJwkJson(String signingJwkJson) {
        this.signingJwkJson = signingJwkJson;
    }
}
