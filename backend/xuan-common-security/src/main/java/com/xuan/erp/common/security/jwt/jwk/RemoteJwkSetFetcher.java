package com.xuan.erp.common.security.jwt.jwk;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.text.ParseException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 基于远程 JWKS Endpoint 的 JWK Set 获取器。
 *
 * <p>该组件只负责访问一个明确的 JWKS URI、读取 JSON 并解析成 public-only 的 {@link JWKSet}。
 * 服务发现、环境配置和调用方的安全链路编排放在外层完成，避免公共安全模块绑定 Nacos 或 Gateway。</p>
 */
public class RemoteJwkSetFetcher implements JwkSetFetcher {

    private final WebClient webClient;
    private final Supplier<Mono<URI>> jwkSetUriSupplier;
    private final Function<URI, Mono<String>> jwksJsonFetcher;

    /**
     * 使用固定 JWKS URI 创建远程获取器。
     *
     * @param webClient HTTP 客户端
     * @param jwkSetUri JWKS Endpoint 地址
     */
    public RemoteJwkSetFetcher(WebClient webClient, URI jwkSetUri) {
        this(webClient, () -> Mono.just(Objects.requireNonNull(jwkSetUri, "jwkSetUri must not be null")));
    }

    /**
     * 使用动态 URI 提供器创建远程获取器。
     *
     * <p>Gateway 可以在这里传入基于服务发现解析出来的 IAM JWKS URI；普通业务服务也可以传入固定
     * 配置 URI。无论 URI 如何得到，HTTP 拉取和 JWK 解析都由公共安全模块完成。</p>
     *
     * @param webClient HTTP 客户端
     * @param jwkSetUriSupplier JWKS URI 提供器
     */
    public RemoteJwkSetFetcher(WebClient webClient, Supplier<Mono<URI>> jwkSetUriSupplier) {
        this.webClient = Objects.requireNonNull(webClient, "webClient must not be null");
        this.jwkSetUriSupplier = Objects.requireNonNull(jwkSetUriSupplier, "jwkSetUriSupplier must not be null");
        this.jwksJsonFetcher = uri -> this.webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * 使用自定义 JSON 获取函数创建远程获取器。
     *
     * <p>该构造器主要用于单元测试或未来替换 HTTP 客户端时复用解析逻辑；生产默认路径仍使用
     * {@link WebClient} 构造器。</p>
     *
     * @param jwksJsonFetcher JWKS JSON 获取函数
     * @param jwkSetUri JWKS Endpoint 地址
     */
    public RemoteJwkSetFetcher(Function<URI, Mono<String>> jwksJsonFetcher, URI jwkSetUri) {
        this.webClient = null;
        this.jwkSetUriSupplier = () -> Mono.just(Objects.requireNonNull(jwkSetUri, "jwkSetUri must not be null"));
        this.jwksJsonFetcher = Objects.requireNonNull(jwksJsonFetcher, "jwksJsonFetcher must not be null");
    }

    @Override
    public Mono<JWKSet> fetch() {
        return Mono.defer(() -> {
            Mono<URI> uriMono = jwkSetUriSupplier.get();
            if (uriMono == null) {
                return Mono.error(new JwkSetUnavailableException("JWKS URI supplier returned null"));
            }
            return uriMono;
        }).flatMap(uri -> jwksJsonFetcher.apply(requireUri(uri))
                .map(this::parseJwkSet));
    }

    private static URI requireUri(URI uri) {
        if (uri == null || uri.toString().isBlank()) {
            throw new JwkSetUnavailableException("JWKS URI must not be blank");
        }
        return uri;
    }

    private JWKSet parseJwkSet(String jwksJson) {
        try {
            return JWKSet.parse(jwksJson).toPublicJWKSet();
        } catch (ParseException ex) {
            throw new JwkSetUnavailableException("JWKS response cannot be parsed", ex);
        }
    }
}
