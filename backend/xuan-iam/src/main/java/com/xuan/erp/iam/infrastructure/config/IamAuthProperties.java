package com.xuan.erp.iam.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IAM 认证配置属性，定义 refresh token 等登录态参数。
 */
@ConfigurationProperties(prefix = "xuan.iam.auth")
public class IamAuthProperties {

    private Duration refreshTokenTtl = Duration.ofDays(30);

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }
}
