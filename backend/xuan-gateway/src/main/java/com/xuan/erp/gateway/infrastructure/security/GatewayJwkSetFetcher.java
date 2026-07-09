package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * 通过服务发现从 IAM 服务拉取当前可用的 JWKS 文档。
 *
 * <p>网关自身不保存 IAM 私钥，只负责通过服务发现定位 `xuan-iam`
 * 的实例地址，并访问其 `/.well-known/jwks.json` 端点获取公钥集合，
 * 供后续 JWT 验签流程使用。</p>
 */
public class GatewayJwkSetFetcher {

    private static final String JWKS_PATH = "/.well-known/jwks.json";

    private final DiscoveryClient discoveryClient;
    private final WebClient webClient;
    private final String iamServiceName;

    public GatewayJwkSetFetcher(DiscoveryClient discoveryClient, WebClient webClient, String iamServiceName) {
        this.discoveryClient = Objects.requireNonNull(discoveryClient, "discoveryClient must not be null");
        this.webClient = Objects.requireNonNull(webClient, "webClient must not be null");
        this.iamServiceName = Objects.requireNonNull(iamServiceName, "iamServiceName must not be null");
    }

    /**
     * 拉取 IAM 当前对外发布的 JWKS。
     *
     * <p>当前实现会从服务发现结果中取第一个可用实例，并向其标准 JWKS
     * 端点发起请求；如果没有找到任何实例，则直接返回错误，让上层感知
     * 当前认证链路不可用。</p>
     *
     * @return IAM 发布的公钥集合
     */
    public Mono<JWKSet> fetch() {
        List<ServiceInstance> instances = discoveryClient.getInstances(iamServiceName);
        if (instances == null || instances.isEmpty()) {
            return Mono.error(new IllegalStateException("未找到可用的 IAM 服务实例: " + iamServiceName));
        }

        URI jwksUri = UriComponentsBuilder.fromUri(instances.get(0).getUri())
                .path(JWKS_PATH)
                .build(true)
                .toUri();

        return webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseJwkSet);
    }

    /**
     * 将 IAM 返回的 JWKS JSON 解析为 Nimbus 可识别的 {@link JWKSet}。
     *
     * @param jwksJson IAM 返回的 JWKS 原始 JSON
     * @return 解析后的公钥集合
     */
    private JWKSet parseJwkSet(String jwksJson) {
        try {
            return JWKSet.parse(jwksJson);
        } catch (java.text.ParseException ex) {
            throw new IllegalStateException("解析 IAM JWKS 失败: " + iamServiceName, ex);
        }
    }
}
