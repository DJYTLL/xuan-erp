package com.xuan.erp.audit.infrastructure.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * xuan-audit 安全配置。
 *
 * <p>Audit 管理端接口通过 xuan-gateway 暴露，由网关完成 Bearer Token 入口认证；
 * 服务自身禁用默认 Basic challenge，避免浏览器在接口查询失败时弹出原生登录框。</p>
 */
@Configuration
public class AuditSecurityConfiguration {

    private static final String OBSERVABILITY_API_PREFIX = "/api/audit/observability/";

    @Bean
    @Order(1)
    SecurityFilterChain auditObservabilitySecurityFilterChain(HttpSecurity http) throws Exception {
        return statelessApi(http)
                .securityMatcher(observabilityApiMatcher())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain auditSecurityFilterChain(HttpSecurity http) throws Exception {
        return statelessApi(http)
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/error",
                                "/error/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/api/audit/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    private HttpSecurity statelessApi(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, ex) -> response.sendError(401, "未认证"))
                        .accessDeniedHandler((request, response, ex) -> response.sendError(403, "无权限")))
                .rememberMe(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults());
    }

    private RequestMatcher observabilityApiMatcher() {
        return request -> {
            String contextPath = request.getContextPath();
            String uri = request.getRequestURI();
            String path = contextPath == null || contextPath.isBlank() ? uri : uri.substring(contextPath.length());
            return path.startsWith(OBSERVABILITY_API_PREFIX);
        };
    }
}
