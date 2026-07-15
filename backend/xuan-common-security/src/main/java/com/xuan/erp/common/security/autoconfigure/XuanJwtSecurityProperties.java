package com.xuan.erp.common.security.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * 业务服务 JWT/JWK 自动配置属性。
 *
 * <p>该配置只用于普通业务服务验证访问令牌。IAM 签发私钥、Gateway 入口认证规则分别由各自服务配置，
 * 这里不保存私钥、不保存密码，也不承载租户业务语义。</p>
 */
@ConfigurationProperties(prefix = "xuan.security.jwt")
public class XuanJwtSecurityProperties {

    /**
     * 是否启用业务服务侧 JWT/JWK 自动配置。
     */
    private boolean enabled = false;

    /**
     * 期望的 JWT 签发方。
     */
    private String issuer = "xuan-iam";

    /**
     * 期望的 JWT 受众，通常填写当前业务服务名。
     */
    private String audience;

    /**
     * IAM 发布 JWK 公钥集的地址。
     */
    private URI jwkSetUri;

    /**
     * JWK 本地缓存配置。
     */
    private Cache cache = new Cache();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    public URI getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(URI jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache == null ? new Cache() : cache;
    }

    /**
     * JWK 缓存相关配置。
     */
    public static class Cache {

        /**
         * 未知 kid 的负缓存时间，避免恶意 token 反复触发远程 JWK 刷新。
         */
        private Duration negativeCacheTtl = Duration.ofSeconds(30);

        public Duration getNegativeCacheTtl() {
            return negativeCacheTtl;
        }

        public void setNegativeCacheTtl(Duration negativeCacheTtl) {
            this.negativeCacheTtl = negativeCacheTtl;
        }
    }
}
