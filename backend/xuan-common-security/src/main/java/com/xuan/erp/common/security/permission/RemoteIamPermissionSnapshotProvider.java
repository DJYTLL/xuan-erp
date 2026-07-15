package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.security.CurrentUser;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 通过 IAM 当前权限快照接口加载业务服务授权快照。
 */
public class RemoteIamPermissionSnapshotProvider implements PermissionSnapshotLoader {

    private static final ParameterizedTypeReference<ApiResponse<IamPermissionSnapshotResponse>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final URI iamSnapshotUri;

    public RemoteIamPermissionSnapshotProvider(WebClient webClient, URI iamSnapshotUri) {
        this.webClient = Objects.requireNonNull(webClient, "webClient must not be null");
        this.iamSnapshotUri = Objects.requireNonNull(iamSnapshotUri, "iamSnapshotUri must not be null");
    }

    @Override
    public PermissionSnapshot load(CurrentUser currentUser, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AccessDeniedException("当前请求缺少访问令牌，无法加载权限快照");
        }
        ApiResponse<IamPermissionSnapshotResponse> response;
        try {
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
                data.authVersion() == null ? currentUser.authVersion() : data.authVersion());
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

    private record IamPermissionSnapshotResponse(
            List<String> routePermissions,
            List<String> buttonPermissions,
            Map<String, List<String>> columnPermissions,
            Map<String, List<String>> fieldPermissions,
            List<String> dataScopes,
            Map<String, List<String>> stateActionRules,
            Long authVersion) {
    }
}
