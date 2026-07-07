package com.xuan.erp.common.security.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

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

            assertThat(mockMvc.perform(get("/tenants")).andReturn().getResponse().getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        });
    }

    // 测试内置的固定 swagger 基础认证用户可以访问受保护的业务端点。
    @Test
    void allowsBusinessEndpointsWithFixedSwaggerBasicUser() {
        contextRunner.run(context -> {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
                    .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                    .build();

            assertThat(mockMvc.perform(get("/tenants").with(httpBasic("swagger", "swagger"))).andReturn().getResponse().getStatus())
                    .isEqualTo(HttpStatus.OK.value());
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
    }
}
