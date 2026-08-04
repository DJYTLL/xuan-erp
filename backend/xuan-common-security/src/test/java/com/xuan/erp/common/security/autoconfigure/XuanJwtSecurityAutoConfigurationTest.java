package com.xuan.erp.common.security.autoconfigure;

import com.nimbusds.jose.jwk.JWKSet;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import com.xuan.erp.common.security.jwt.jwk.RemoteJwkSetFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.List;

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
                        "xuan.security.jwt.cache.positive-cache-ttl=10m",
                        "xuan.security.jwt.cache.stale-cache-ttl=30m",
                        "xuan.security.jwt.cache.negative-cache-ttl=45s",
                        "xuan.security.jwt.cache.refresh-interval=5m")
                .run(context -> {
                    assertThat(context).hasSingleBean(XuanJwtSecurityProperties.class);
                    assertThat(context).hasSingleBean(RemoteJwkSetFetcher.class);
                    assertThat(context).hasSingleBean(CachingJwkKeyProvider.class);
                    assertThat(context).hasSingleBean(JwkSetRefreshScheduler.class);
                    assertThat(context).hasSingleBean(JwkJwtTokenParser.class);
                    assertThat(context).hasSingleBean(BearerTokenResolver.class);
                    XuanJwtSecurityProperties properties = context.getBean(XuanJwtSecurityProperties.class);
                    assertThat(properties.getIssuer()).isEqualTo("xuan-iam");
                    assertThat(properties.getCache().getPositiveCacheTtl()).isEqualTo(java.time.Duration.ofMinutes(10));
                    assertThat(properties.getCache().getStaleCacheTtl()).isEqualTo(java.time.Duration.ofMinutes(30));
                    assertThat(properties.getCache().getNegativeCacheTtl()).isEqualTo(java.time.Duration.ofSeconds(45));
                    assertThat(properties.getCache().getRefreshInterval()).isEqualTo(java.time.Duration.ofMinutes(5));
                });
    }

    // 测试业务服务已接入注册发现时，即使没有固定 jwk-set-uri，也能直接通过 iam-service-name + path 启动。
    @Test
    void createsJwtInfrastructureWhenDiscoveryClientIsAvailable() {
        contextRunner
                .withUserConfiguration(DiscoveryClientConfiguration.class)
                .withPropertyValues(
                        "xuan.security.jwt.enabled=true",
                        "xuan.security.jwt.issuer=xuan-iam",
                        "xuan.security.jwt.audience=xuan-tenant",
                        "xuan.security.jwt.iam-service-name=xuan-iam",
                        "xuan.security.jwt.jwk-set-path=/.well-known/jwks.json")
                .run(context -> {
                    assertThat(context).hasSingleBean(RemoteJwkSetFetcher.class);
                    assertThat(context).hasSingleBean(CachingJwkKeyProvider.class);
                    assertThat(context).hasSingleBean(JwkJwtTokenParser.class);
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
                        .hasStackTraceContaining("xuan.security.jwt.jwk-set-uri 未配置"));
    }

    // 测试 JWK 正向缓存 TTL 必须大于 0，避免配置成永不过期后无法感知 IAM 换钥。
    @Test
    void failsClearlyWhenPositiveCacheTtlIsInvalid() {
        contextRunner
                .withPropertyValues(
                        "xuan.security.jwt.enabled=true",
                        "xuan.security.jwt.issuer=xuan-iam",
                        "xuan.security.jwt.audience=xuan-tenant",
                        "xuan.security.jwt.jwk-set-uri=http://xuan-iam/.well-known/jwks.json",
                        "xuan.security.jwt.cache.positive-cache-ttl=0s")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasMessageContaining("xuan.security.jwt.cache.positive-cache-ttl 必须大于 0"));
    }

    // 测试 JWK 陈旧缓存兜底 TTL 允许为 0，但不允许为负数。
    @Test
    void failsClearlyWhenStaleCacheTtlIsNegative() {
        contextRunner
                .withPropertyValues(
                        "xuan.security.jwt.enabled=true",
                        "xuan.security.jwt.issuer=xuan-iam",
                        "xuan.security.jwt.audience=xuan-tenant",
                        "xuan.security.jwt.jwk-set-uri=http://xuan-iam/.well-known/jwks.json",
                        "xuan.security.jwt.cache.stale-cache-ttl=-1s")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasMessageContaining("xuan.security.jwt.cache.stale-cache-ttl 不能小于 0"));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomParserConfiguration {

        @Bean
        JwkJwtTokenParser customParser() {
            return new JwkJwtTokenParser(new JWKSet(), "custom-issuer", "custom-audience");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DiscoveryClientConfiguration {

        @Bean
        DiscoveryClient discoveryClient() {
            return serviceId -> List.of(new TestServiceInstance(URI.create("http://127.0.0.1:18080")));
        }
    }

    private record TestServiceInstance(URI uri) implements ServiceInstance {

        @Override
        public URI getUri() {
            return uri;
        }
    }
}
