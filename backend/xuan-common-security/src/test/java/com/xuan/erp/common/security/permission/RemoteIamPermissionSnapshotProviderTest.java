package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * IAM 远程权限快照加载测试，验证业务服务会携带当前 Bearer Token 查询 IAM。
 */
class RemoteIamPermissionSnapshotProviderTest {

    @AfterEach
    void stopServer() {
    }

    // 测试远程 IAM 快照会合并路由权限和按钮权限，并转成业务服务授权快照。
    @Test
    void loadsSnapshotFromIamWithBearerToken() throws Exception {
        WebClient webClient = Mockito.mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(URI.create("http://iam.local/api/iam/permissions/current"))).thenReturn(headersSpec);
        when(headersSpec.header(eq("Authorization"), eq("Bearer access-token"))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono((ParameterizedTypeReference) any())).thenReturn(Mono.just(ApiResponse.success(snapshotResponse(
                Set.of("tenant:view"),
                Set.of("tenant:create"),
                "5"))));
        RemoteIamPermissionSnapshotProvider provider = new RemoteIamPermissionSnapshotProvider(
                webClient,
                URI.create("http://iam.local/api/iam/permissions/current"));
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of("tenant_admin"), 5L, Set.of());

        PermissionSnapshot snapshot = provider.load(currentUser, "access-token");

        Mockito.verify(headersSpec).header("Authorization", "Bearer access-token");
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder("tenant:view", "tenant:create");
        assertThat(snapshot.roles()).containsExactly("tenant_admin");
        assertThat(snapshot.authVersion()).isEqualTo(5L);
        assertThat(snapshot.columnAccess("tenant", "code")).isEqualTo(ColumnAccess.VISIBLE);
        assertThat(snapshot.columnAccess("tenant", "contactPhone")).isEqualTo(ColumnAccess.MASKED);
        assertThat(snapshot.columnAccess("tenant", "remark")).isEqualTo(ColumnAccess.HIDDEN);
        assertThat(snapshot.dataScopes("tenant")).containsExactlyInAnyOrder("SELF", "DEPARTMENT");
        assertThat(snapshot.hasDataScope("tenant", "SELF")).isTrue();
        assertThat(snapshot.isStateActionAllowed("tenant", "ENABLED", "disable")).isTrue();
        assertThat(snapshot.isStateActionAllowed("tenant", "DISABLED", "delete")).isFalse();
    }

    // 测试 IAM 返回的权限版本高于当前 token 时，业务服务拒绝继续使用旧 token 授权。
    @Test
    void rejectsSnapshotWhenAuthVersionIsStale() throws Exception {
        WebClient webClient = Mockito.mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(URI.create("http://iam.local/api/iam/permissions/current"))).thenReturn(headersSpec);
        when(headersSpec.header(eq("Authorization"), eq("Bearer old-token"))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono((ParameterizedTypeReference) any())).thenReturn(Mono.just(ApiResponse.success(snapshotResponse(
                Set.of("tenant:create"),
                Set.of(),
                "6"))));
        RemoteIamPermissionSnapshotProvider provider = new RemoteIamPermissionSnapshotProvider(
                webClient,
                URI.create("http://iam.local/api/iam/permissions/current"));
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of(), 5L, Set.of());

        assertThatThrownBy(() -> provider.load(currentUser, "old-token"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("权限版本已过期");
    }

    // 测试权限快照加载器支持按次解析 IAM URI，便于业务服务通过服务发现定位 IAM。
    @Test
    void resolvesIamSnapshotUriFromSupplierOnEachLoad() throws Exception {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        Supplier<Mono<URI>> uriSupplier = () -> Mono.just(URI.create("http://127.0.0.1:18080/api/iam/permissions/current"));
        WebClient webClient = Mockito.mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenAnswer(invocation -> {
            requestedUri.set(invocation.getArgument(0));
            return headersSpec;
        });
        when(headersSpec.header(eq("Authorization"), eq("Bearer access-token"))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono((ParameterizedTypeReference) any())).thenReturn(Mono.just(ApiResponse.success(snapshotResponse(
                Set.of("tenant:view"),
                Set.of("tenant:update"),
                "5"))));
        RemoteIamPermissionSnapshotProvider provider = new RemoteIamPermissionSnapshotProvider(webClient, uriSupplier);
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of("tenant_admin"), 5L, Set.of());

        PermissionSnapshot snapshot = provider.load(currentUser, "access-token");

        assertThat(requestedUri.get()).isEqualTo(URI.create("http://127.0.0.1:18080/api/iam/permissions/current"));
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder("tenant:view", "tenant:update");
    }

    private Object snapshotResponse(Set<String> routePermissions, Set<String> buttonPermissions, String authVersion) throws Exception {
        Class<?> responseType = Class.forName(
                "com.xuan.erp.common.security.permission.RemoteIamPermissionSnapshotProvider$IamPermissionSnapshotResponse");
        Constructor<?> constructor = responseType.getDeclaredConstructor(
                java.util.List.class,
                java.util.List.class,
                java.util.Map.class,
                java.util.Map.class,
                java.util.List.class,
                java.util.Map.class,
                Long.class);
        constructor.setAccessible(true);
        return constructor.newInstance(
                routePermissions.stream().toList(),
                buttonPermissions.stream().toList(),
                java.util.Map.of("tenant", java.util.Map.of("code", "VISIBLE", "contactPhone", "MASKED", "remark", "HIDDEN")),
                java.util.Map.of(),
                java.util.List.of("tenant:SELF", "tenant:DEPARTMENT"),
                java.util.Map.of("tenant:ENABLED", java.util.List.of("disable", "update")),
                Long.valueOf(authVersion));
    }
}
