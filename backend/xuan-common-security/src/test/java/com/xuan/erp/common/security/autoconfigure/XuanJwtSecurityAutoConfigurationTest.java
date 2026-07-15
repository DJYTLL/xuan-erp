package com.xuan.erp.common.security.autoconfigure;

import com.nimbusds.jose.jwk.JWKSet;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import com.xuan.erp.common.security.jwt.jwk.RemoteJwkSetFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class XuanJwtSecurityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(XuanJwtSecurityAutoConfiguration.class));

    // 测试业务服务配置完整时，会按 xuan.security.jwt.* 自动创建远程 JWK 获取器、缓存 Provider 和 JWT Parser。
    @Test
    void createsJwtInfrastructureWhenBusinessJwtPropertiesAreComplete() {
        contextRunner
                .withPropertyValues(
                        "xuan.security.jwt.enabled=true",
                        "xuan.security.jwt.issuer=xuan-iam",
                        "xuan.security.jwt.audience=xuan-tenant",
                        "xuan.security.jwt.jwk-set-uri=http://xuan-iam/.well-known/jwks.json",
                        "xuan.security.jwt.cache.negative-cache-ttl=45s")
                .run(context -> {
                    assertThat(context).hasSingleBean(XuanJwtSecurityProperties.class);
                    assertThat(context).hasSingleBean(RemoteJwkSetFetcher.class);
                    assertThat(context).hasSingleBean(CachingJwkKeyProvider.class);
                    assertThat(context).hasSingleBean(JwkJwtTokenParser.class);
                    assertThat(context).hasSingleBean(BearerTokenResolver.class);
                    assertThat(context.getBean(XuanJwtSecurityProperties.class).getIssuer()).isEqualTo("xuan-iam");
                });
    }

    // 测试业务方自定义 Parser 时，自动配置会退让，避免覆盖业务服务自己的验签策略。
    @Test
    void backsOffWhenBusinessServiceProvidesCustomParser() {
        contextRunner
                .withUserConfiguration(CustomParserConfiguration.class)
                .withPropertyValues(
                        "xuan.security.jwt.enabled=true",
                        "xuan.security.jwt.issuer=xuan-iam",
                        "xuan.security.jwt.audience=xuan-tenant",
                        "xuan.security.jwt.jwk-set-uri=http://xuan-iam/.well-known/jwks.json")
                .run(context -> assertThat(context).hasSingleBean(JwkJwtTokenParser.class));
    }

    // 测试关闭业务服务 JWT 自动配置时，不创建远程 JWK 获取器、缓存 Provider 和 JWT Parser。
    @Test
    void doesNotCreateJwtInfrastructureWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "xuan.security.jwt.enabled=false",
                        "xuan.security.jwt.issuer=xuan-iam",
                        "xuan.security.jwt.audience=xuan-tenant")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RemoteJwkSetFetcher.class);
                    assertThat(context).doesNotHaveBean(CachingJwkKeyProvider.class);
                    assertThat(context).doesNotHaveBean(JwkJwtTokenParser.class);
                });
    }

    // 测试启用自动配置但缺少 jwk-set-uri 时启动失败，并给出能直接定位配置项的中文错误信息。
    @Test
    void failsClearlyWhenJwkSetUriIsMissing() {
        contextRunner
                .withPropertyValues(
                        "xuan.security.jwt.enabled=true",
                        "xuan.security.jwt.issuer=xuan-iam",
                        "xuan.security.jwt.audience=xuan-tenant")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasMessageContaining("xuan.security.jwt.jwk-set-uri 未配置"));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomParserConfiguration {

        @Bean
        JwkJwtTokenParser customParser() {
            return new JwkJwtTokenParser(new JWKSet(), "custom-issuer", "custom-audience");
        }
    }
}
