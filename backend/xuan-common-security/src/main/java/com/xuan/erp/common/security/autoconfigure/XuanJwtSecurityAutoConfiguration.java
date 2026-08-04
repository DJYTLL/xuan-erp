package com.xuan.erp.common.security.autoconfigure;

import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import com.xuan.erp.common.security.jwt.jwk.JwkSetUriSupplier;
import com.xuan.erp.common.security.jwt.jwk.RemoteJwkSetFetcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

/**
 * 普通业务服务使用的 JWT/JWK 自动配置。
 *
 * <p>启用 {@code xuan.security.jwt.enabled=true} 后，业务服务会优先通过服务发现解析
 * {@code xuan.security.jwt.iam-service-name + xuan.security.jwt.jwk-set-path}，只有在当前服务
 * 未接入注册发现时，才回退使用 {@code xuan.security.jwt.jwk-set-uri} 固定地址。</p>
 */
@AutoConfiguration
@ConditionalOnClass({WebClient.class, JwkJwtTokenParser.class})
@EnableConfigurationProperties(XuanJwtSecurityProperties.class)
@ConditionalOnProperty(prefix = "xuan.security.jwt", name = "enabled", havingValue = "true")
public class XuanJwtSecurityAutoConfiguration {

    /**
     * 提供业务服务 JWT 自动配置使用的 WebClient Builder。
     *
     * @return WebClient 构建器
     */
    @Bean
    @ConditionalOnMissingBean
    WebClient.Builder xuanJwtWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * 创建远程 JWK Set 获取器。
     *
     * @param webClientBuilder WebClient 构建器
     * @param properties JWT 自动配置属性
     * @return 远程 JWK Set 获取器
     */
    @Bean
    @ConditionalOnMissingBean
    JwkSetUriSupplier xuanJwtJwkSetUriSupplier(
            XuanJwtSecurityProperties properties,
            ListableBeanFactory beanFactory) {
        ServiceDiscoveryUriResolverSupport discoverySupport = new ServiceDiscoveryUriResolverSupport(
                beanFactory,
                XuanJwtSecurityAutoConfiguration.class.getClassLoader());
        return discoverySupport.buildUriSupplier(properties.getIamServiceName(), properties.getJwkSetPath())
                .<JwkSetUriSupplier>map(uriSupplier -> uriSupplier::get)
                .orElseGet(() -> {
                    URI jwkSetUri = requiredJwkSetUri(properties);
                    return () -> reactor.core.publisher.Mono.just(jwkSetUri);
                });
    }

    @Bean
    @ConditionalOnMissingBean
    @Lazy
    RemoteJwkSetFetcher xuanJwtRemoteJwkSetFetcher(
            WebClient.Builder webClientBuilder,
            JwkSetUriSupplier jwkSetUriSupplier) {
        return new RemoteJwkSetFetcher(webClientBuilder.build(), jwkSetUriSupplier::get);
    }

    /**
     * 创建带本地缓存的 JWK Provider。
     *
     * @param fetcher 远程 JWK Set 获取器
     * @param properties JWT 自动配置属性
     * @return 带缓存的 JWK Provider
     */
    @Bean
    @ConditionalOnMissingBean
    CachingJwkKeyProvider xuanJwtCachingJwkKeyProvider(
            @Lazy RemoteJwkSetFetcher fetcher,
            XuanJwtSecurityProperties properties) {
        return new CachingJwkKeyProvider(
                fetcher,
                requiredPositiveCacheTtl(properties),
                requiredStaleCacheTtl(properties),
                requiredNegativeCacheTtl(properties),
                requiredRefreshInterval(properties),
                Clock.systemUTC());
    }

    /**
     * 创建 JWKS 定期刷新调度器。
     *
     * @param jwkProvider 带缓存的 JWK Provider
     * @param properties JWT 自动配置属性
     * @return JWKS 定期刷新调度器
     */
    @Bean
    @ConditionalOnMissingBean
    JwkSetRefreshScheduler xuanJwkSetRefreshScheduler(
            CachingJwkKeyProvider jwkProvider,
            XuanJwtSecurityProperties properties) {
        return new JwkSetRefreshScheduler(jwkProvider, requiredRefreshInterval(properties));
    }

    /**
     * 创建默认 Bearer Token 解析器。
     *
     * @return Bearer Token 解析器
     */
    @Bean
    @ConditionalOnMissingBean
    BearerTokenResolver xuanBearerTokenResolver() {
        return new BearerTokenResolver();
    }

    /**
     * 创建业务服务可直接注入使用的 JWT Parser。
     *
     * @param jwkProvider 带缓存的 JWK Provider
     * @param properties JWT 自动配置属性
     * @return JWT Parser
     */
    @Bean
    @ConditionalOnMissingBean
    JwkJwtTokenParser xuanJwkJwtTokenParser(
            CachingJwkKeyProvider jwkProvider,
            XuanJwtSecurityProperties properties) {
        return new JwkJwtTokenParser(
                jwkProvider,
                requiredText(properties.getIssuer(), "xuan.security.jwt.issuer 未配置"),
                requiredText(properties.getAudience(), "xuan.security.jwt.audience 未配置"));
    }

    private static URI requiredJwkSetUri(XuanJwtSecurityProperties properties) {
        URI jwkSetUri = properties.getJwkSetUri();
        if (jwkSetUri == null || jwkSetUri.toString().isBlank()) {
            throw new IllegalStateException("未找到可用的 DiscoveryClient，且 xuan.security.jwt.jwk-set-uri 未配置");
        }
        return jwkSetUri;
    }

    private static Duration requiredPositiveCacheTtl(XuanJwtSecurityProperties properties) {
        Duration positiveCacheTtl = properties.getCache().getPositiveCacheTtl();
        if (positiveCacheTtl == null || positiveCacheTtl.isZero() || positiveCacheTtl.isNegative()) {
            throw new IllegalStateException("xuan.security.jwt.cache.positive-cache-ttl 必须大于 0");
        }
        return positiveCacheTtl;
    }

    private static Duration requiredStaleCacheTtl(XuanJwtSecurityProperties properties) {
        Duration staleCacheTtl = properties.getCache().getStaleCacheTtl();
        if (staleCacheTtl == null || staleCacheTtl.isNegative()) {
            throw new IllegalStateException("xuan.security.jwt.cache.stale-cache-ttl 不能小于 0");
        }
        return staleCacheTtl;
    }

    private static Duration requiredNegativeCacheTtl(XuanJwtSecurityProperties properties) {
        Duration negativeCacheTtl = properties.getCache().getNegativeCacheTtl();
        if (negativeCacheTtl == null || negativeCacheTtl.isZero() || negativeCacheTtl.isNegative()) {
            throw new IllegalStateException("xuan.security.jwt.cache.negative-cache-ttl 必须大于 0");
        }
        return negativeCacheTtl;
    }

    private static Duration requiredRefreshInterval(XuanJwtSecurityProperties properties) {
        Duration refreshInterval = properties.getCache().getRefreshInterval();
        if (refreshInterval == null || refreshInterval.isZero() || refreshInterval.isNegative()) {
            throw new IllegalStateException("xuan.security.jwt.cache.refresh-interval 必须大于 0");
        }
        return refreshInterval;
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
