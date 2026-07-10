package com.xuan.erp.gateway.infrastructure.config;

import com.xuan.erp.gateway.infrastructure.security.CachingGatewayJwkProvider;
import com.xuan.erp.gateway.infrastructure.security.GatewayBearerAuthenticationWebFilter;
import com.xuan.erp.gateway.infrastructure.security.GatewayJwkSetFetcher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;

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
     * 创建通过服务发现拉取 IAM JWKS 的抓取器。
     *
     * @param discoveryClient Spring Cloud 服务发现客户端
     * @param gatewayJwkWebClient 用于访问 IAM 的 HTTP 客户端
     * @param properties 网关安全配置
     * @return JWKS 抓取器
     */
    @Bean
    GatewayJwkSetFetcher gatewayJwkSetFetcher(
            org.springframework.cloud.client.discovery.DiscoveryClient discoveryClient,
            WebClient gatewayJwkWebClient,
            GatewaySecurityProperties properties) {
        return new GatewayJwkSetFetcher(discoveryClient, gatewayJwkWebClient, properties.getIamServiceName());
    }

    /**
     * 创建带本地缓存的网关 JWKS Provider。
     *
     * @param fetcher JWKS 抓取器
     * @return 带缓存的 JWKS Provider
     */
    @Bean
    CachingGatewayJwkProvider cachingGatewayJwkProvider(GatewayJwkSetFetcher fetcher) {
        return new CachingGatewayJwkProvider(fetcher);
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
            CachingGatewayJwkProvider jwkProvider,
            GatewaySecurityProperties properties) {
        return new GatewayBearerAuthenticationWebFilter(jwkProvider, properties);
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
            CorsConfigurationSource gatewayCorsConfigurationSource,
            GatewaySecurityProperties properties) {
        if (!properties.isEnabled()) {
            return http
                    .cors(cors -> cors.configurationSource(gatewayCorsConfigurationSource))
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .build();
        }
        return http
                .cors(cors -> cors.configurationSource(gatewayCorsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(spec -> spec.authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }))
                .authorizeExchange(spec -> spec
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/api/iam/auth/login",
                                "/.well-known/jwks.json")
                        .permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(bearerAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
