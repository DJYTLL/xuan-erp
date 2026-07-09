package com.xuan.erp.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关 JWT / JWKS 安全配置属性。
 *
 * <p>该配置用于控制网关是否开启 Bearer Token 验签，以及验签时
 * 需要使用的签发方、受众和 IAM 服务名。</p>
 */
@ConfigurationProperties(prefix = "xuan.gateway.security")
public class GatewaySecurityProperties {

    /**
     * 是否启用网关认证。
     *
     * <p>true 表示启用 JWT / JWKS 验签；
     * false 表示关闭这套认证链路。</p>
     */
    private boolean enabled = true;

    /**
     * 期望的 JWT 签发方。
     *
     * <p>网关验签时会校验 token 中的 iss 是否与该值一致。</p>
     */
    private String issuer = "xuan-iam";

    /**
     * 期望的 JWT 受众。
     *
     * <p>网关验签时会校验 token 中的 aud 是否包含该值。</p>
     */
    private String audience = "xuan-gateway";

    /**
     * IAM 服务名。
     *
     * <p>网关会通过服务发现使用该服务名定位 xuan-iam，
     * 然后拉取 `/.well-known/jwks.json` 公钥集合。</p>
     */
    private String iamServiceName = "xuan-iam";

    /**
     * 返回是否启用网关认证。
     *
     * @return true 表示启用；false 表示关闭
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用网关认证。
     *
     * @param enabled true 表示启用；false 表示关闭
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回期望的 JWT 签发方。
     *
     * @return JWT issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 设置期望的 JWT 签发方。
     *
     * @param issuer JWT issuer
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * 返回期望的 JWT 受众。
     *
     * @return JWT audience
     */
    public String getAudience() {
        return audience;
    }

    /**
     * 设置期望的 JWT 受众。
     *
     * @param audience JWT audience
     */
    public void setAudience(String audience) {
        this.audience = audience;
    }

    /**
     * 返回 IAM 服务名。
     *
     * @return 服务发现中的 IAM 服务名
     */
    public String getIamServiceName() {
        return iamServiceName;
    }

    /**
     * 设置 IAM 服务名。
     *
     * @param iamServiceName 服务发现中的 IAM 服务名
     */
    public void setIamServiceName(String iamServiceName) {
        this.iamServiceName = iamServiceName;
    }
}