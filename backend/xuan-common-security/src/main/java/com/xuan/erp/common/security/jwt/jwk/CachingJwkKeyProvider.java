package com.xuan.erp.common.security.jwt.jwk;

import com.nimbusds.jose.jwk.JWKSet;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带本地缓存的 JWK 公钥 Provider。
 *
 * <p>该 Provider 是 Gateway 和业务服务复用的公共实现：优先使用本地缓存，缓存未命中时刷新远程
 * JWKS，刷新后仍找不到目标 {@code kid} 时短期负缓存，避免恶意未知 kid 请求把 IAM 打满。</p>
 */
public class CachingJwkKeyProvider {

    private static final Duration DEFAULT_NEGATIVE_CACHE_TTL = Duration.ofSeconds(30);

    private final JwkSetFetcher jwkSetFetcher;
    private final Duration negativeCacheTtl;
    private final Clock clock;
    private final Map<String, Instant> negativeKidCache = new ConcurrentHashMap<>();

    private volatile JWKSet cachedJwkSet;

    public CachingJwkKeyProvider(JwkSetFetcher jwkSetFetcher) {
        this(jwkSetFetcher, DEFAULT_NEGATIVE_CACHE_TTL, Clock.systemUTC());
    }

    public CachingJwkKeyProvider(JwkSetFetcher jwkSetFetcher, Duration negativeCacheTtl, Clock clock) {
        this.jwkSetFetcher = Objects.requireNonNull(jwkSetFetcher, "jwkSetFetcher must not be null");
        this.negativeCacheTtl = requirePositive(negativeCacheTtl);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 按 JWT header 中的 kid 返回包含目标公钥的 JWK Set。
     *
     * @param kid JWT header 中携带的 key id
     * @return 包含目标 kid 公钥的 JWK Set
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
        if (isNegativeCached(requiredKid)) {
            return Mono.error(signatureInvalid("No JWK found for kid: " + requiredKid));
        }
        return current == null
                ? loadThenRefreshIfNeeded(requiredKid)
                : refreshAndRequireKid(requiredKid);
    }

    private Mono<JWKSet> loadThenRefreshIfNeeded(String kid) {
        return fetchAndCache()
                .flatMap(jwkSet -> containsKid(jwkSet, kid)
                        ? Mono.just(jwkSet)
                        : refreshAndRequireKid(kid))
                .onErrorMap(this::shouldWrapAsUnavailable, this::unavailable);
    }

    private Mono<JWKSet> refreshAndRequireKid(String kid) {
        return fetchAndCache()
                .flatMap(jwkSet -> {
                    if (containsKid(jwkSet, kid)) {
                        negativeKidCache.remove(kid);
                        return Mono.just(jwkSet);
                    }
                    negativeKidCache.put(kid, Instant.now(clock).plus(negativeCacheTtl));
                    return Mono.error(signatureInvalid("No JWK found for kid: " + kid));
                })
                .onErrorMap(this::shouldWrapAsUnavailable, this::unavailable);
    }

    private Mono<JWKSet> fetchAndCache() {
        return jwkSetFetcher.fetch()
                .map(JWKSet::toPublicJWKSet)
                .doOnNext(jwkSet -> cachedJwkSet = jwkSet);
    }

    private boolean isNegativeCached(String kid) {
        Instant expiresAt = negativeKidCache.get(kid);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isAfter(Instant.now(clock))) {
            return true;
        }
        negativeKidCache.remove(kid);
        return false;
    }

    private boolean shouldWrapAsUnavailable(Throwable throwable) {
        return !(throwable instanceof JwtValidationException)
                && !(throwable instanceof JwkSetUnavailableException);
    }

    private JwkSetUnavailableException unavailable(Throwable throwable) {
        return new JwkSetUnavailableException("JWKS Endpoint is unavailable", throwable);
    }

    private static boolean containsKid(JWKSet jwkSet, String kid) {
        return jwkSet != null && jwkSet.getKeyByKeyId(kid) != null;
    }

    private static JwtValidationException signatureInvalid(String message) {
        return new JwtValidationException(JwtValidationException.Reason.SIGNATURE_INVALID, message);
    }

    private static Duration requirePositive(Duration duration) {
        Objects.requireNonNull(duration, "negativeCacheTtl must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("negativeCacheTtl must be positive");
        }
        return duration;
    }
}
