package com.xuan.erp.common.security.autoconfigure;

import com.nimbusds.jose.jwk.JWKSet;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.servlet.BusinessJwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class XuanServletSecurityAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DispatcherServletAutoConfiguration.class,
                    WebMvcAutoConfiguration.class,
                    SecurityAutoConfiguration.class,
                    UserDetailsServiceAutoConfiguration.class,
                    SecurityFilterAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class,
                    XuanServletSecurityAutoConfiguration.class))
            .withUserConfiguration(TestControllerConfiguration.class);

    // 测试 OpenAPI 和 Swagger 相关端点默认放行，不需要登录也能访问文档资源。
    @Test
    void permitsOpenApiAndSwaggerEndpointsWithoutAuthentication() {
        contextRunner.run(context -> {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
                    .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                    .build();

            assertThat(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus())
                    .isEqualTo(HttpStatus.OK.value());
            assertThat(mockMvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus())
                    .isEqualTo(HttpStatus.OK.value());
            assertThat(mockMvc.perform(get("/swagger-ui/index.css")).andReturn().getResponse().getStatus())
                    .isEqualTo(HttpStatus.OK.value());
        });
    }

    // 测试业务接口默认受保护，未认证请求访问业务端点时应返回 401。
    @Test
    void keepsBusinessEndpointsProtectedByDefault() {
        contextRunner.run(context -> {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
                    .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                    .build();

            var response = mockMvc.perform(get("/tenants")).andReturn().getResponse();
            assertThat(response.getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getHeader("WWW-Authenticate")).isNull();
        });
    }

    // 测试网关转发的身份头可以在 Servlet 业务服务中恢复为当前登录用户。
    @Test
    void authenticatesRequestFromGatewayIdentityHeaders() {
        contextRunner.run(context -> {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
                    .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                    .build();

            var response = mockMvc.perform(get("/current-user")
                            .header("X-User-Id", "7")
                            .header("X-Tenant-Id", "1001")
                            .header("X-Username", "tenant-admin")
                            .header("X-Roles", "tenant_admin")
                            .header("X-Auth-Version", "5")
                            .header("X-Permissions", "tenant:view,tenant:create"))
                    .andReturn()
                    .getResponse();

            assertThat(response.getStatus())
                    .isEqualTo(HttpStatus.OK.value());
            String[] parts = response.getContentAsString().split("\\|");
            assertThat(parts[0]).isEqualTo("tenant-admin");
            assertThat(parts[1].split(","))
                    .containsExactlyInAnyOrder("tenant:view", "tenant:create");
        });
    }

    // 测试网关身份过滤器只在 Spring Security 链中执行，避免被 Servlet 容器提前执行后丢失认证。
    @Test
    void disablesServletContainerRegistrationForGatewayIdentityFilter() {
        contextRunner.run(context -> {
            @SuppressWarnings("unchecked")
            FilterRegistrationBean<com.xuan.erp.common.security.servlet.GatewayIdentityAuthenticationFilter> registration =
                    context.getBean(
                            "gatewayIdentityAuthenticationFilterRegistration",
                            FilterRegistrationBean.class);

            assertThat(registration.isEnabled()).isFalse();
        });
    }

    // 测试启用业务服务 JWT Bean 后，默认 Servlet 安全链可以通过 Bearer Token 建立 CurrentUser。
    @Test
    void authenticatesBearerTokenWhenBusinessJwtBeansAreAvailable() {
        contextRunner
                .withUserConfiguration(BusinessJwtTestConfiguration.class)
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
                            .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                            .build();

                    var response = mockMvc.perform(get("/current-user")
                                    .header("Authorization", "Bearer access-token"))
                            .andReturn()
                            .getResponse();

                    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
                    String[] parts = response.getContentAsString().split("\\|");
                    assertThat(parts[0]).isEqualTo("tenant-admin");
                    assertThat(parts[1].split(","))
                            .containsExactlyInAnyOrder("tenant:view", "tenant:create");
                });
    }

    // 测试业务 JWT 过滤器只在 Spring Security 链中执行，避免被 Servlet 容器提前执行。
    @Test
    void disablesServletContainerRegistrationForBusinessJwtFilter() {
        contextRunner
                .withUserConfiguration(BusinessJwtTestConfiguration.class)
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    FilterRegistrationBean<BusinessJwtAuthenticationFilter> registration =
                            context.getBean(
                                    "businessJwtAuthenticationFilterRegistration",
                                    FilterRegistrationBean.class);

                    assertThat(registration.isEnabled()).isFalse();
                });
    }

    // 测试自动配置会贡献且只贡献一个 Servlet SecurityFilterChain。
    @Test
    void contributesSingleServletSecurityFilterChain() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(SecurityFilterChain.class));
    }

    // 测试自动配置会贡献固定的 swagger UserDetailsService。
    @Test
    void contributesFixedSwaggerBasicUser() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(UserDetailsService.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestControllerConfiguration {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BusinessJwtTestConfiguration {

        @Bean
        BearerTokenResolver bearerTokenResolver() {
            return new BearerTokenResolver();
        }

        @Bean
        JwkJwtTokenParser jwkJwtTokenParser() {
            return new JwkJwtTokenParser(new JWKSet(), "issuer", "audience") {
                @Override
                public CurrentUser parseAccessToken(String token) {
                    return new CurrentUser(
                            7L,
                            1001L,
                            "tenant-admin",
                            Set.of("tenant_admin"),
                            5L,
                            Set.of("tenant:view", "tenant:create"));
                }
            };
        }
    }

    @RestController
    static class TestController {

        @GetMapping({
                "/v3/api-docs",
                "/swagger-ui.html",
                "/swagger-ui/index.css",
                "/tenants"
        })
        String ok() {
            return "ok";
        }

        @GetMapping("/current-user")
        String currentUser(org.springframework.security.core.Authentication authentication) {
            com.xuan.erp.common.security.CurrentUser currentUser =
                    (com.xuan.erp.common.security.CurrentUser) authentication.getPrincipal();
            return currentUser.username() + "|" + String.join(",", currentUser.permissions());
        }
    }
}
