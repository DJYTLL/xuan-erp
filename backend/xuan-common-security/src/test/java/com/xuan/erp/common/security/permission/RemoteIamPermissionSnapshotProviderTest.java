package com.xuan.erp.common.security.permission;

import com.sun.net.httpserver.HttpServer;
import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IAM 远程权限快照加载测试，验证业务服务会携带当前 Bearer Token 查询 IAM。
 */
class RemoteIamPermissionSnapshotProviderTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    // 测试远程 IAM 快照会合并路由权限和按钮权限，并转成业务服务授权快照。
    @Test
    void loadsSnapshotFromIamWithBearerToken() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        URI uri = startServer("""
                {"code":"SUCCESS","message":"OK","data":{
                  "menus":[],
                  "routePermissions":["tenant:view"],
                  "buttonPermissions":["tenant:create"],
                  "columnPermissions":{},
                  "fieldPermissions":{},
                  "dataScopes":[],
                  "stateActionRules":{},
                  "authVersion":5
                }}
                """, authorization);
        RemoteIamPermissionSnapshotProvider provider = new RemoteIamPermissionSnapshotProvider(
                WebClient.builder().build(),
                uri);
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of("tenant_admin"), 5L, Set.of());

        PermissionSnapshot snapshot = provider.load(currentUser, "access-token");

        assertThat(authorization).hasValue("Bearer access-token");
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder("tenant:view", "tenant:create");
        assertThat(snapshot.roles()).containsExactly("tenant_admin");
        assertThat(snapshot.authVersion()).isEqualTo(5L);
    }

    // 测试 IAM 返回的权限版本高于当前 token 时，业务服务拒绝继续使用旧 token 授权。
    @Test
    void rejectsSnapshotWhenAuthVersionIsStale() throws Exception {
        URI uri = startServer("""
                {"code":"SUCCESS","message":"OK","data":{
                  "routePermissions":["tenant:create"],
                  "buttonPermissions":[],
                  "authVersion":6
                }}
                """, new AtomicReference<>());
        RemoteIamPermissionSnapshotProvider provider = new RemoteIamPermissionSnapshotProvider(
                WebClient.builder().build(),
                uri);
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of(), 5L, Set.of());

        assertThatThrownBy(() -> provider.load(currentUser, "old-token"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("权限版本已过期");
    }

    private URI startServer(String body, AtomicReference<String> authorization) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/iam/permissions/current", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/iam/permissions/current");
    }
}
