package com.xuan.erp.tenant.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.security.servlet.BusinessJwtAuthenticationFilter;
import com.xuan.erp.common.security.servlet.GatewayIdentityAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * xuan-tenant 安全配置，额外开放供 IAM 登录前查询的内部租户状态接口。
 */
@Configuration
@EnableMethodSecurity
public class TenantSecurityConfiguration {

    @Bean
    SecurityFilterChain tenantSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<BusinessJwtAuthenticationFilter> businessJwtAuthenticationFilter,
            GatewayIdentityAuthenticationFilter gatewayIdentityAuthenticationFilter,
            ObjectMapper objectMapper) throws Exception {
        HttpSecurity security = http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeSecurityResponse(
                                response,
                                objectMapper,
                                HttpStatus.UNAUTHORIZED,
                                ApiResponse.failure("SECURITY_AUTHENTICATION_MISSING", "未登录或登录已过期")))
                        .accessDeniedHandler((request, response, exception) -> writeSecurityResponse(
                                response,
                                objectMapper,
                                HttpStatus.FORBIDDEN,
                                ApiResponse.failure("SECURITY_PERMISSION_DENIED", "没有访问权限"))))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/internal/tenants/*/status",
                                "/internal/tenants/by-code/*/status",
                                "/internal/tenant-plan-usages/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        BusinessJwtAuthenticationFilter jwtFilter = businessJwtAuthenticationFilter.getIfAvailable();
        if (jwtFilter == null) {
            security.addFilterAfter(gatewayIdentityAuthenticationFilter, SecurityContextHolderFilter.class);
        } else {
            security.addFilterAfter(jwtFilter, SecurityContextHolderFilter.class)
                    .addFilterAfter(gatewayIdentityAuthenticationFilter, BusinessJwtAuthenticationFilter.class);
        }
        return security.build();
    }

    private void writeSecurityResponse(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            ApiResponse<Void> body) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
