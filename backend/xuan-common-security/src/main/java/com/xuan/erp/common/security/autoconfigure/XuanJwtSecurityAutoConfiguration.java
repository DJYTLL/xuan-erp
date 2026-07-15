package com.xuan.erp.common.security.autoconfigure;

import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import com.xuan.erp.common.security.jwt.jwk.RemoteJwkSetFetcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

/**
 * 普通业务服务使用的 JWT/JWK 自动配置。
 *
 * <p>启用 {@code xuan.security.jwt.enabled=true} 后，业务服务会通过固定的
 * {@code xuan.security.jwt.jwk-set-uri} 拉取 IAM 公钥，缓存后交给 {@link JwkJwtTokenParser}
 * 完成访问令牌验签与身份解析。</p>
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
    RemoteJwkSetFetcher xuanJwtRemoteJwkSetFetcher(
            WebClient.Builder webClientBuilder,
            XuanJwtSecurityProperties properties) {
        return new RemoteJwkSetFetcher(webClientBuilder.build(), requiredJwkSetUri(properties));
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
            RemoteJwkSetFetcher fetcher,
            XuanJwtSecurityProperties properties) {
        return new CachingJwkKeyProvider(fetcher, requiredNegativeCacheTtl(properties), Clock.systemUTC());
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
            throw new IllegalStateException("xuan.security.jwt.jwk-set-uri 未配置");
        }
        return jwkSetUri;
    }

    private static Duration requiredNegativeCacheTtl(XuanJwtSecurityProperties properties) {
        Duration negativeCacheTtl = properties.getCache().getNegativeCacheTtl();
        if (negativeCacheTtl == null || negativeCacheTtl.isZero() || negativeCacheTtl.isNegative()) {
            throw new IllegalStateException("xuan.security.jwt.cache.negative-cache-ttl 必须大于 0");
        }
        return negativeCacheTtl;
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
