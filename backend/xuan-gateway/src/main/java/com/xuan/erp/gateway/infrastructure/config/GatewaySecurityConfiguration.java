package com.xuan.erp.gateway.infrastructure.config;
import com.xuan.erp.common.audit.SafeAuditWritePublisher;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.autoconfigure.JwkSetRefreshScheduler;
import com.xuan.erp.common.security.audit.SecurityAuditEventFactory;
import com.xuan.erp.common.security.audit.SecurityAuditFailure;
import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import com.xuan.erp.common.security.jwt.jwk.RemoteJwkSetFetcher;
import com.xuan.erp.gateway.infrastructure.security.GatewayAuditWriteGateway;
import com.xuan.erp.gateway.infrastructure.security.GatewaySecurityAuditReporter;
import com.xuan.erp.gateway.infrastructure.security.GatewayBearerAuthenticationWebFilter;
import com.xuan.erp.gateway.infrastructure.security.GatewayJwkSetUriResolver;
import com.xuan.erp.gateway.infrastructure.security.GatewayRequestHeaderWebFilter;
import com.xuan.erp.gateway.infrastructure.security.GatewaySecurityErrorResponseWriter;
import com.xuan.erp.gateway.infrastructure.security.GatewaySentinelBlockRequestHandler;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 网关 WebFlux Security 配置。
 *
 * <p>该配置负责组装网关 JWT/JWKS 验签所需的基础 Bean，并定义
 * Spring Security 在网关入口的最小认证规则。</p>
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfiguration {

    /**
     * 提供网关内部使用的 {@link WebClient.Builder}。
     *
     * @return WebClient 构建器
     */
    @Bean
    WebClient.Builder gatewayWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * 提供用于访问 IAM JWKS 端点的 WebClient。
     *
     * @param gatewayWebClientBuilder WebClient 构建器
     * @return IAM JWKS 调用客户端
     */
    @Bean
    WebClient gatewayJwkWebClient(WebClient.Builder gatewayWebClientBuilder) {
        return gatewayWebClientBuilder.build();
    }

    /**
     * 创建通过服务发现解析 IAM JWKS 地址的解析器。
     *
     * @param discoveryClient Spring Cloud 服务发现客户端
     * @param properties 网关安全配置
     * @return IAM JWKS 地址解析器
     */
    @Bean
    GatewayJwkSetUriResolver gatewayJwkSetUriResolver(
            org.springframework.cloud.client.discovery.DiscoveryClient discoveryClient,
            GatewaySecurityProperties properties) {
        return new GatewayJwkSetUriResolver(discoveryClient, properties.getIamServiceName());
    }

    /**
     * 创建公共安全模块提供的远程 JWKS 获取器。
     *
     * @param resolver IAM JWKS 地址解析器
     * @param gatewayJwkWebClient 用于访问 IAM 的 HTTP 客户端
     * @return 公共远程 JWKS 获取器
     */
    @Bean
    RemoteJwkSetFetcher gatewayJwkSetFetcher(
            GatewayJwkSetUriResolver resolver,
            WebClient gatewayJwkWebClient) {
        return new RemoteJwkSetFetcher(gatewayJwkWebClient, resolver::resolve);
    }

    /**
     * 创建公共安全审计事件工厂。
     *
     * @return 安全审计事件工厂
     */
    @Bean
    SecurityAuditEventFactory securityAuditEventFactory(GatewaySecurityProperties properties) {
        return new SecurityAuditEventFactory(properties.getSecurityAudit().getSystemTenantId());
    }

    /**
     * 创建网关侧审计写入适配器。
     *
     * @param discoveryClient 服务发现客户端
     * @param gatewayJwkWebClient 复用网关 WebClient
     * @param properties 网关安全配置
     * @return 审计写入网关
     */
    @Bean
    GatewayAuditWriteGateway gatewayAuditWriteGateway(
            DiscoveryClient discoveryClient,
            WebClient gatewayJwkWebClient,
            GatewaySecurityProperties properties) {
        return new GatewayAuditWriteGateway(discoveryClient, gatewayJwkWebClient, properties);
    }

    /**
     * 创建安全审计后台线程池。
     *
     * @param properties 网关安全配置
     * @return 审计写入执行器
     */
    @Bean
    ThreadPoolTaskExecutor gatewaySecurityAuditExecutor(GatewaySecurityProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(1, properties.getSecurityAudit().getPoolSize());
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("gateway-security-audit-");
        executor.initialize();
        return executor;
    }

    /**
     * 创建安全审计异步发布器。
     *
     * @param auditWriteGateway 审计写入适配器
     * @param gatewaySecurityAuditExecutor 审计后台线程池
     * @return 安全审计发布器
     */
    @Bean
    SafeAuditWritePublisher securityAuditPublisher(
            GatewayAuditWriteGateway auditWriteGateway,
            Executor gatewaySecurityAuditExecutor) {
        return new SafeAuditWritePublisher(auditWriteGateway, gatewaySecurityAuditExecutor);
    }

    /**
     * 创建网关安全审计报告器。
     *
     * @param securityAuditPublisher 审计发布器
     * @param securityAuditEventFactory 审计事件工厂
     * @param properties 网关安全配置
     * @return 网关安全审计报告器
     */
    @Bean
    GatewaySecurityAuditReporter gatewaySecurityAuditReporter(
            SafeAuditWritePublisher securityAuditPublisher,
            SecurityAuditEventFactory securityAuditEventFactory,
            GatewaySecurityProperties properties) {
        return new GatewaySecurityAuditReporter(securityAuditPublisher, securityAuditEventFactory, properties);
    }

    /**
     * 创建网关安全异常响应写入器。
     *
     * @param objectMapper JSON 序列化器
     * @return 安全异常响应写入器
     */
    @Bean
    GatewaySecurityErrorResponseWriter gatewaySecurityErrorResponseWriter() {
        return new GatewaySecurityErrorResponseWriter();
    }

    /**
     * 创建 Gateway 请求头治理过滤器，负责 TraceId 生成/透传与外部身份 Header 清理。
     *
     * @return Gateway 请求头治理过滤器
     */
    @Bean
    GatewayRequestHeaderWebFilter gatewayRequestHeaderWebFilter() {
        return new GatewayRequestHeaderWebFilter();
    }

    /**
     * 创建 Sentinel Gateway 限流统一响应处理器。
     *
     * @return Sentinel Gateway block 响应处理器
     */
    @Bean
    GatewaySentinelBlockRequestHandler gatewaySentinelBlockRequestHandler() {
        return new GatewaySentinelBlockRequestHandler();
    }

    /**
     * 创建公共安全模块提供的本地缓存 JWKS Provider。
     *
     * @param fetcher JWKS 抓取器
     * @return 带缓存的 JWKS Provider
     */
    @Bean
    CachingJwkKeyProvider cachingJwkKeyProvider(RemoteJwkSetFetcher fetcher, GatewaySecurityProperties properties) {
        GatewaySecurityProperties.JwkCache cache = properties.getJwkCache();
        return new CachingJwkKeyProvider(
                fetcher,
                requiredPositiveCacheTtl(cache),
                requiredStaleCacheTtl(cache),
                requiredNegativeCacheTtl(cache),
                requiredRefreshInterval(cache),
                Clock.systemUTC());
    }

    /**
     * 创建网关 JWKS 定期刷新调度器。
     *
     * @param jwkProvider 带缓存的 JWKS Provider
     * @param properties 网关安全配置
     * @return JWKS 定期刷新调度器
     */
    @Bean
    JwkSetRefreshScheduler gatewayJwkSetRefreshScheduler(
            CachingJwkKeyProvider jwkProvider,
            GatewaySecurityProperties properties) {
        return new JwkSetRefreshScheduler(jwkProvider, requiredRefreshInterval(properties.getJwkCache()));
    }

    /**
     * 创建 Bearer Token 认证过滤器。
     *
     * @param jwkProvider 带缓存的 JWKS Provider
     * @param properties 网关安全配置
     * @return 网关认证过滤器
     */
    @Bean
    GatewayBearerAuthenticationWebFilter gatewayBearerAuthenticationWebFilter(
            CachingJwkKeyProvider jwkProvider,
            GatewaySecurityProperties properties,
            GatewaySecurityAuditReporter securityAuditReporter,
            GatewaySecurityErrorResponseWriter securityErrorResponseWriter) {
        return new GatewayBearerAuthenticationWebFilter(
                jwkProvider,
                properties,
                securityAuditReporter,
                securityErrorResponseWriter);
    }

    /**
     * 创建网关统一 CORS 配置源。
     *
     * @param properties 网关安全配置
     * @return WebFlux Security 使用的 CORS 配置源
     */
    @Bean
    CorsConfigurationSource gatewayCorsConfigurationSource(GatewaySecurityProperties properties) {
        GatewaySecurityProperties.Cors corsProperties = properties.getCors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 定义网关的 WebFlux Security 规则。
     *
     * <p>健康检查和信息端点允许匿名访问，其余请求默认要求先通过
     * Bearer Token 认证；如果关闭网关认证，则只保留最小安全链路。</p>
     *
     * @param http Spring Security WebFlux 配置入口
     * @param bearerAuthenticationWebFilter Bearer Token 认证过滤器
     * @param properties 网关安全配置
     * @return 网关安全过滤链
     */
    @Bean
    SecurityWebFilterChain gatewaySecurityWebFilterChain(
            ServerHttpSecurity http,
            GatewayBearerAuthenticationWebFilter bearerAuthenticationWebFilter,
            GatewayRequestHeaderWebFilter requestHeaderWebFilter,
            CorsConfigurationSource gatewayCorsConfigurationSource,
            GatewaySecurityAuditReporter securityAuditReporter,
            GatewaySecurityErrorResponseWriter securityErrorResponseWriter,
            GatewaySecurityProperties properties) {
        if (!properties.isEnabled()) {
            return http
                    .cors(cors -> cors.configurationSource(gatewayCorsConfigurationSource))
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .addFilterAt(requestHeaderWebFilter, SecurityWebFiltersOrder.FIRST)
                    .build();
        }
        return http
                .cors(cors -> cors.configurationSource(gatewayCorsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(spec -> spec
                        .authenticationEntryPoint((exchange, ex) -> securityAuditReporter
                                .publish(exchange, SecurityAuditFailure.AUTHENTICATION_MISSING, null, null, ex.getMessage())
                                .then(securityErrorResponseWriter.write(
                                        exchange,
                                        SecurityAuditFailure.AUTHENTICATION_MISSING)))
                        .accessDeniedHandler((exchange, ex) -> publishAccessDenied(
                                exchange,
                                securityAuditReporter,
                                securityErrorResponseWriter,
                                ex.getMessage())))
                .authorizeExchange(spec -> {
                    if (!properties.getPublicPaths().isEmpty()) {
                        spec.pathMatchers(properties.getPublicPaths().toArray(String[]::new)).permitAll();
                    }
                    for (GatewaySecurityProperties.PermissionRule rule : orderedPermissionRules(properties)) {
                        List<String> authorities = rule.getAuthorities();
                        if (!authorities.isEmpty() && !rule.getPaths().isEmpty()) {
                            Set<String> allowedAuthorities = new LinkedHashSet<>(authorities);
                            allowedAuthorities.add("*");
                            spec.pathMatchers(rule.getPaths().toArray(String[]::new))
                                    .hasAnyAuthority(allowedAuthorities.toArray(String[]::new));
                        }
                    }
                    spec.anyExchange().authenticated();
                })
                .addFilterAt(requestHeaderWebFilter, SecurityWebFiltersOrder.FIRST)
                .addFilterAt(bearerAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    private Mono<Void> publishAccessDenied(
            ServerWebExchange exchange,
            GatewaySecurityAuditReporter securityAuditReporter,
            GatewaySecurityErrorResponseWriter securityErrorResponseWriter,
            String message) {
        return exchange.getPrincipal()
                .ofType(Authentication.class)
                .map(Authentication::getPrincipal)
                .ofType(CurrentUser.class)
                .flatMap(currentUser -> securityAuditReporter
                        .publish(exchange, SecurityAuditFailure.PERMISSION_DENIED, currentUser, null, message)
                        .then(securityErrorResponseWriter.write(
                                exchange,
                                SecurityAuditFailure.PERMISSION_DENIED)))
                .switchIfEmpty(securityAuditReporter
                        .publish(exchange, SecurityAuditFailure.PERMISSION_DENIED, null, null, message)
                        .then(securityErrorResponseWriter.write(
                                exchange,
                SecurityAuditFailure.PERMISSION_DENIED)));
    }

    private List<GatewaySecurityProperties.PermissionRule> orderedPermissionRules(GatewaySecurityProperties properties) {
        return properties.getPermissionRules().stream()
                .sorted((left, right) -> Integer.compare(ruleSpecificity(right), ruleSpecificity(left)))
                .toList();
    }

    private int ruleSpecificity(GatewaySecurityProperties.PermissionRule rule) {
        return rule.getPaths().stream()
                .mapToInt(this::pathSpecificity)
                .max()
                .orElse(0);
    }

    private int pathSpecificity(String path) {
        if (path == null || path.isBlank()) {
            return 0;
        }
        int score = path.length();
        if (!path.contains("*")) {
            score += 10_000;
        }
        score -= wildcardCount(path) * 1_000;
        return score;
    }

    private int wildcardCount(String path) {
        int count = 0;
        for (int index = 0; index < path.length(); index++) {
            if (path.charAt(index) == '*') {
                count++;
            }
        }
        return count;
    }

    private static Duration requiredPositiveCacheTtl(GatewaySecurityProperties.JwkCache cache) {
        Duration value = cache.getPositiveCacheTtl();
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("xuan.gateway.security.jwk-cache.positive-cache-ttl 必须大于 0");
        }
        return value;
    }

    private static Duration requiredStaleCacheTtl(GatewaySecurityProperties.JwkCache cache) {
        Duration value = cache.getStaleCacheTtl();
        if (value == null || value.isNegative()) {
            throw new IllegalStateException("xuan.gateway.security.jwk-cache.stale-cache-ttl 不能小于 0");
        }
        return value;
    }

    private static Duration requiredNegativeCacheTtl(GatewaySecurityProperties.JwkCache cache) {
        Duration value = cache.getNegativeCacheTtl();
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("xuan.gateway.security.jwk-cache.negative-cache-ttl 必须大于 0");
        }
        return value;
    }

    private static Duration requiredRefreshInterval(GatewaySecurityProperties.JwkCache cache) {
        Duration value = cache.getRefreshInterval();
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("xuan.gateway.security.jwk-cache.refresh-interval 必须大于 0");
        }
        return value;
    }
}
