package com.xuan.erp.iam.infrastructure.config;

import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.permission.PermissionSnapshot;
import com.xuan.erp.common.security.permission.XuanPermissionExpression;
import com.xuan.erp.iam.infrastructure.security.IamBearerTokenAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * IAM 安全配置，负责方法鉴权、JWT/JWK Bean 与 Bearer 过滤链注册。
 */
@Configuration
@EnableMethodSecurity
public class IamSecurityConfiguration {

    @Bean
    PasswordEncoder iamPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    IamBearerTokenAuthenticationFilter iamBearerTokenAuthenticationFilter(JwkJwtTokenParser jwtTokenParser) {
        return new IamBearerTokenAuthenticationFilter(jwtTokenParser);
    }

    @Bean("xuanPermission")
    @ConditionalOnMissingBean(name = "xuanPermission")
    XuanPermissionExpression iamLocalXuanPermissionExpression() {
        return new XuanPermissionExpression((currentUser, accessToken) -> new PermissionSnapshot(
                currentUser.tenantId(),
                currentUser.userId(),
                currentUser.username(),
                currentUser.roles(),
                currentUser.permissions(),
                currentUser.authVersion()));
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain iamSecurityFilterChain(HttpSecurity http, IamBearerTokenAuthenticationFilter authenticationFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, ex) -> response.sendError(401, "未认证"))
                        .accessDeniedHandler((request, response, ex) -> response.sendError(403, "无权限")))
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
                                "/error",
                                "/api/iam/auth/login",
                                "/api/iam/auth/refresh",
                                "/api/iam/auth/logout",
                                "/.well-known/jwks.json")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .rememberMe(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults())
                .build();
    }
}
