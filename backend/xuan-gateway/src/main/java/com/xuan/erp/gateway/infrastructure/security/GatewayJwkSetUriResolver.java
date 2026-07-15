package com.xuan.erp.gateway.infrastructure.security;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Gateway 侧 IAM JWKS 地址解析器。
 *
 * <p>Gateway 只负责通过服务发现定位 IAM 实例并拼出标准 JWKS 地址；HTTP 拉取、JSON 解析、本地缓存、
 * 未知 kid 刷新等通用认证能力全部交给 `xuan-common-security`。</p>
 */
public class GatewayJwkSetUriResolver {

    private static final String JWKS_PATH = "/.well-known/jwks.json";

    private final DiscoveryClient discoveryClient;
    private final String iamServiceName;

    public GatewayJwkSetUriResolver(DiscoveryClient discoveryClient, String iamServiceName) {
        this.discoveryClient = Objects.requireNonNull(discoveryClient, "discoveryClient must not be null");
        this.iamServiceName = Objects.requireNonNull(iamServiceName, "iamServiceName must not be null");
    }

    /**
     * 解析当前可用 IAM 实例的 JWKS Endpoint。
     *
     * @return IAM JWKS Endpoint URI
     */
    public Mono<URI> resolve() {
        List<ServiceInstance> instances = discoveryClient.getInstances(iamServiceName);
        if (instances == null || instances.isEmpty()) {
            return Mono.error(new IllegalStateException("未找到可用的 IAM 服务实例: " + iamServiceName));
        }
        URI jwksUri = UriComponentsBuilder.fromUri(instances.get(0).getUri())
                .path(JWKS_PATH)
                .build(true)
                .toUri();
        return Mono.just(jwksUri);
    }
}
