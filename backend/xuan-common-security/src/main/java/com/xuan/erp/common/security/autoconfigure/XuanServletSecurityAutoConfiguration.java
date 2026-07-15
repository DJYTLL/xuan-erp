package com.xuan.erp.common.security.autoconfigure;

import com.xuan.erp.common.security.servlet.GatewayIdentityAuthenticationFilter;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.servlet.BusinessJwtAuthenticationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Servlet Web 场景下的默认安全自动配置。
 */
@AutoConfiguration(after = XuanJwtSecurityAutoConfiguration.class, before = UserDetailsServiceAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({HttpSecurity.class, SecurityFilterChain.class})
public class XuanServletSecurityAutoConfiguration {

    /**
     * 注册默认 Servlet 安全过滤链，开放文档和健康检查端点，其余请求需要认证。
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain xuanServletSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<BusinessJwtAuthenticationFilter> businessJwtAuthenticationFilter,
            GatewayIdentityAuthenticationFilter gatewayIdentityAuthenticationFilter) throws Exception {
        HttpSecurity security = http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info")
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

    @Bean
    @ConditionalOnBean({BearerTokenResolver.class, JwkJwtTokenParser.class})
    @ConditionalOnMissingBean(BusinessJwtAuthenticationFilter.class)
    BusinessJwtAuthenticationFilter businessJwtAuthenticationFilter(
            BearerTokenResolver bearerTokenResolver,
            JwkJwtTokenParser jwtTokenParser) {
        return new BusinessJwtAuthenticationFilter(bearerTokenResolver, jwtTokenParser);
    }

    @Bean
    @ConditionalOnMissingBean(GatewayIdentityAuthenticationFilter.class)
    GatewayIdentityAuthenticationFilter gatewayIdentityAuthenticationFilter() {
        return new GatewayIdentityAuthenticationFilter();
    }

    @Bean
    FilterRegistrationBean<GatewayIdentityAuthenticationFilter> gatewayIdentityAuthenticationFilterRegistration(
            GatewayIdentityAuthenticationFilter gatewayIdentityAuthenticationFilter) {
        FilterRegistrationBean<GatewayIdentityAuthenticationFilter> registration =
                new FilterRegistrationBean<>(gatewayIdentityAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnBean(BusinessJwtAuthenticationFilter.class)
    FilterRegistrationBean<BusinessJwtAuthenticationFilter> businessJwtAuthenticationFilterRegistration(
            BusinessJwtAuthenticationFilter businessJwtAuthenticationFilter) {
        FilterRegistrationBean<BusinessJwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(businessJwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 提供默认 Swagger Basic 用户，业务方自定义 UserDetailsService 时自动让位。
     */
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    UserDetailsService xuanSwaggerUserDetailsService() {
        return new InMemoryUserDetailsManager(User.withUsername("swagger")
                .password("{noop}swagger")
                .roles("SWAGGER")
                .build());
    }
}
