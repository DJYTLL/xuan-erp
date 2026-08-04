package com.xuan.erp.common.security.autoconfigure;

import com.xuan.erp.common.security.permission.CachedPermissionSnapshotProvider;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.permission.PermissionSnapshot;
import com.xuan.erp.common.security.permission.PermissionSnapshotProvider;
import com.xuan.erp.common.security.permission.RemoteIamPermissionSnapshotProvider;
import com.xuan.erp.common.security.permission.XuanPermissionExpression;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 权限快照自动配置测试，验证业务服务启用配置后可获得远程加载、缓存和表达式 Bean。
 */
class XuanPermissionSecurityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(XuanPermissionSecurityAutoConfiguration.class))
            .withBean("xuanPermissionWebClientBuilder", WebClient.Builder.class, () -> {
                WebClient.Builder builder = Mockito.mock(WebClient.Builder.class);
                Mockito.when(builder.build()).thenReturn(Mockito.mock(WebClient.class));
                return builder;
            });

    // 测试配置完整时自动创建 IAM 远程快照 Provider、缓存 Provider 和 @xuanPermission 表达式。
    @Test
    void createsPermissionSnapshotBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "xuan.security.permission.enabled=true",
                        "xuan.security.permission.iam-snapshot-uri=http://127.0.0.1:18080/api/iam/permissions/current",
                        "xuan.security.permission.cache.ttl=PT30S")
                .run(context -> {
                    assertThat(context).hasSingleBean(RemoteIamPermissionSnapshotProvider.class);
                    assertThat(context).hasSingleBean(CachedPermissionSnapshotProvider.class);
                    assertThat(context).hasSingleBean(PermissionSnapshotProvider.class);
                    assertThat(context).hasBean("xuanPermission");
                    assertThat(context).hasSingleBean(XuanPermissionExpression.class);
                });
    }

    // 测试业务服务已接入注册发现时，即使没有固定 iam-snapshot-uri，也能通过服务名和路径自动创建权限快照链路。
    @Test
    void createsPermissionSnapshotBeansWhenDiscoveryClientIsAvailable() {
        contextRunner
                .withUserConfiguration(DiscoveryClientConfiguration.class)
                .withPropertyValues(
                        "xuan.security.permission.enabled=true",
                        "xuan.security.permission.iam-service-name=xuan-iam",
                        "xuan.security.permission.iam-snapshot-path=/api/iam/permissions/current",
                        "xuan.security.permission.cache.ttl=PT30S")
                .run(context -> {
                    assertThat(context).hasSingleBean(RemoteIamPermissionSnapshotProvider.class);
                    assertThat(context).hasSingleBean(CachedPermissionSnapshotProvider.class);
                    assertThat(context).hasSingleBean(PermissionSnapshotProvider.class);
                });
    }

    // 测试启用权限配置后，@PreAuthorize 会真正拦截缺少权限的业务方法。
    @Test
    void enablesPreAuthorizeWithXuanPermissionExpression() {
        contextRunner
                .withUserConfiguration(MethodSecurityTestConfiguration.class)
                .withPropertyValues(
                        "xuan.security.permission.enabled=true",
                        "xuan.security.permission.iam-snapshot-uri=http://127.0.0.1:18080/api/iam/permissions/current",
                        "xuan.security.permission.cache.ttl=PT30S")
                .run(context -> {
                    CurrentUser currentUser = new CurrentUser(
                            7L,
                            1L,
                            "tenant-viewer",
                            Set.of("tenant_viewer"),
                            2L,
                            Set.of("tenant:view"));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(currentUser, "access-token", Set.of()));
                    try {
                        assertThatThrownBy(() -> context.getBean(SecuredSampleService.class).enableTenant())
                                .isInstanceOf(AccessDeniedException.class);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
    }

    // 测试未配置 IAM 快照地址时启动失败，避免业务服务误以为已启用远程授权。
    @Test
    void failsWhenIamSnapshotUriIsMissing() {
        contextRunner
                .withPropertyValues("xuan.security.permission.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    static class MethodSecurityTestConfiguration {

        @Bean
        PermissionSnapshotProvider permissionSnapshotProvider() {
            return (currentUser, accessToken) -> new PermissionSnapshot(
                    currentUser.tenantId(),
                    currentUser.userId(),
                    currentUser.username(),
                    currentUser.roles(),
                    Set.of("tenant:view"),
                    currentUser.authVersion());
        }

        @Bean
        SecuredSampleService securedSampleService() {
            return new SecuredSampleService();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DiscoveryClientConfiguration {

        @Bean
        DiscoveryClient discoveryClient() {
            return serviceId -> List.of(new TestServiceInstance(URI.create("http://127.0.0.1:18080")));
        }
    }

    static class SecuredSampleService {

        @PreAuthorize("@xuanPermission.has('tenant:enable')")
        void enableTenant() {
        }
    }

    private record TestServiceInstance(URI uri) implements ServiceInstance {

        @Override
        public URI getUri() {
            return uri;
        }
    }
}
