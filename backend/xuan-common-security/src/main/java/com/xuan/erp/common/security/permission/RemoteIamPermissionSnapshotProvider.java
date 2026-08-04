package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.security.CurrentUser;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 通过 IAM 当前权限快照接口加载业务服务授权快照。
 */
public class RemoteIamPermissionSnapshotProvider implements PermissionSnapshotLoader {

    private static final ParameterizedTypeReference<ApiResponse<IamPermissionSnapshotResponse>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final Supplier<Mono<URI>> iamSnapshotUriSupplier;

    public RemoteIamPermissionSnapshotProvider(WebClient webClient, URI iamSnapshotUri) {
        this(webClient, () -> Mono.just(Objects.requireNonNull(iamSnapshotUri, "iamSnapshotUri must not be null")));
    }

    public RemoteIamPermissionSnapshotProvider(WebClient webClient, Supplier<Mono<URI>> iamSnapshotUriSupplier) {
        this.webClient = Objects.requireNonNull(webClient, "webClient must not be null");
        this.iamSnapshotUriSupplier = Objects.requireNonNull(iamSnapshotUriSupplier, "iamSnapshotUriSupplier must not be null");
    }

    @Override
    public PermissionSnapshot load(CurrentUser currentUser, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AccessDeniedException("当前请求缺少访问令牌，无法加载权限快照");
        }
        ApiResponse<IamPermissionSnapshotResponse> response;
        try {
            URI iamSnapshotUri = resolveIamSnapshotUri();
            response = webClient.get()
                    .uri(iamSnapshotUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(RESPONSE_TYPE)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new AccessDeniedException("加载 IAM 权限快照失败：" + ex.getStatusCode(), ex);
        }
        if (response == null || !"SUCCESS".equals(response.code()) || response.data() == null) {
            throw new AccessDeniedException("加载 IAM 权限快照失败");
        }
        IamPermissionSnapshotResponse data = response.data();
        if (data.authVersion() != null && currentUser.authVersion() != null
                && !Objects.equals(data.authVersion(), currentUser.authVersion())) {
            throw new AccessDeniedException("权限版本已过期，请重新登录");
        }
        return new PermissionSnapshot(
                currentUser.tenantId(),
                currentUser.userId(),
                currentUser.username(),
                currentUser.roles(),
                mergedPermissions(data),
                columnPermissions(data.columnPermissions()),
                data.authVersion() == null ? currentUser.authVersion() : data.authVersion());
    }

    private URI resolveIamSnapshotUri() {
        Mono<URI> uriMono = iamSnapshotUriSupplier.get();
        if (uriMono == null) {
            throw new AccessDeniedException("加载 IAM 权限快照失败：未提供可用的 IAM 地址");
        }
        URI uri = uriMono.block();
        if (uri == null || uri.toString().isBlank()) {
            throw new AccessDeniedException("加载 IAM 权限快照失败：IAM 地址为空");
        }
        return uri;
    }

    private Set<String> mergedPermissions(IamPermissionSnapshotResponse data) {
        Set<String> permissions = new LinkedHashSet<>();
        addAll(permissions, data.routePermissions());
        addAll(permissions, data.buttonPermissions());
        return Set.copyOf(permissions);
    }

    private void addAll(Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(target::add);
    }

    private Map<String, Map<String, ColumnAccess>> columnPermissions(Map<String, Map<String, String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, ColumnAccess>> result = new LinkedHashMap<>();
        values.forEach((resourceKey, columns) -> {
            if (resourceKey == null || resourceKey.isBlank() || columns == null || columns.isEmpty()) {
                return;
            }
            Map<String, ColumnAccess> resourceRules = new LinkedHashMap<>();
            columns.forEach((columnKey, access) -> {
                if (columnKey != null && !columnKey.isBlank()) {
                    resourceRules.put(columnKey.trim(), ColumnAccess.parse(access));
                }
            });
            if (!resourceRules.isEmpty()) {
                result.put(resourceKey.trim(), Map.copyOf(resourceRules));
            }
        });
        return Map.copyOf(result);
    }

    private record IamPermissionSnapshotResponse(
            List<String> routePermissions,
            List<String> buttonPermissions,
            Map<String, Map<String, String>> columnPermissions,
            Map<String, List<String>> fieldPermissions,
            List<String> dataScopes,
            Map<String, List<String>> stateActionRules,
            Long authVersion) {
    }
}
