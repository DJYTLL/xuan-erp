package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 带本地缓存的网关 JWKS Provider。
 *
 * <p>该 Provider 只负责三件事：首次加载 JWKS、本地缓存命中时直接复用、当目标 kid 不在当前缓存中时
 * 刷新一次。如果刷新后仍然找不到目标 kid，则统一按签名无效处理，交给上层认证流程返回未认证响应。</p>
 */
public class CachingGatewayJwkProvider {

    private final GatewayJwkSetFetcher jwkSetFetcher;

    private volatile JWKSet cachedJwkSet;

    public CachingGatewayJwkProvider(GatewayJwkSetFetcher jwkSetFetcher) {
        this.jwkSetFetcher = Objects.requireNonNull(jwkSetFetcher, "jwkSetFetcher must not be null");
    }

    /**
     * 按 token header 中的 kid 返回可用于验签的公钥集合。
     *
     * <p>如果本地缓存已经包含目标 kid，则直接返回缓存；否则会触发一次
     * 拉取或刷新。刷新后仍然找不到目标 kid 时，统一视为签名无效。</p>
     *
     * @param kid JWT header 中携带的 key id
     * @return 包含目标 kid 的公钥集合
     */
    public Mono<JWKSet> jwkSetForKid(String kid) {
        if (kid == null || kid.isBlank()) {
            return Mono.error(signatureInvalid("kid must not be blank"));
        }
        String requiredKid = kid;
        JWKSet current = cachedJwkSet;
        if (containsKid(current, requiredKid)) {
            return Mono.just(current);
        }
        return current == null
                ? loadThenRefreshIfNeeded(requiredKid)
                : refreshAndRequireKid(requiredKid);
    }

    /**
     * 冷启动场景下首次加载 JWKS；如果首次结果不含目标 kid，则再刷新一次。
     *
     * @param kid 当前请求需要匹配的 key id
     * @return 包含目标 kid 的公钥集合
     */
    private Mono<JWKSet> loadThenRefreshIfNeeded(String kid) {
        return fetchAndCache()
                .flatMap(jwkSet -> containsKid(jwkSet, kid)
                        ? Mono.just(jwkSet)
                        : refreshAndRequireKid(kid));
    }

    /**
     * 强制刷新一次 JWKS，并要求刷新结果中必须包含目标 kid。
     *
     * @param kid 当前请求需要匹配的 key id
     * @return 包含目标 kid 的公钥集合
     */
    private Mono<JWKSet> refreshAndRequireKid(String kid) {
        return fetchAndCache()
                .flatMap(jwkSet -> containsKid(jwkSet, kid)
                        ? Mono.just(jwkSet)
                        : Mono.error(signatureInvalid("No JWK found for kid: " + kid)));
    }

    /**
     * 从 IAM 重新抓取 JWKS，并只保留公钥部分写入本地缓存。
     *
     * @return 刷新后的公钥集合
     */
    private Mono<JWKSet> fetchAndCache() {
        return jwkSetFetcher.fetch()
                .map(JWKSet::toPublicJWKSet)
                .doOnNext(jwkSet -> cachedJwkSet = jwkSet);
    }

    /**
     * 判断当前公钥集合中是否存在指定 kid。
     *
     * @param jwkSet 待检查的公钥集合
     * @param kid 目标 key id
     * @return true 表示命中；false 表示未命中
     */
    private static boolean containsKid(JWKSet jwkSet, String kid) {
        return jwkSet != null && jwkSet.getKeyByKeyId(kid) != null;
    }

    /**
     * 构造统一的签名无效异常，避免向上层暴露底层实现细节。
     *
     * @param message 异常说明
     * @return 统一的 JWT 验签异常
     */
    private static JwtValidationException signatureInvalid(String message) {
        return new JwtValidationException(
                JwtValidationException.Reason.SIGNATURE_INVALID,
                message);
    }
}
