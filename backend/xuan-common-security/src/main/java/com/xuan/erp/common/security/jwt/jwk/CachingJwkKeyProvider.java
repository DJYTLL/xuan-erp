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

    private static final Duration DEFAULT_POSITIVE_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration DEFAULT_STALE_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration DEFAULT_NEGATIVE_CACHE_TTL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofMinutes(5);

    private final JwkSetFetcher jwkSetFetcher;
    private final Duration positiveCacheTtl;
    private final Duration staleCacheTtl;
    private final Duration negativeCacheTtl;
    private final Duration refreshInterval;
    private final Clock clock;
    private final Map<String, Instant> negativeKidCache = new ConcurrentHashMap<>();

    private volatile CacheEntry cachedJwkSet;
    private volatile Instant lastRefreshAttemptAt;

    public CachingJwkKeyProvider(JwkSetFetcher jwkSetFetcher) {
        this(
                jwkSetFetcher,
                DEFAULT_POSITIVE_CACHE_TTL,
                DEFAULT_STALE_CACHE_TTL,
                DEFAULT_NEGATIVE_CACHE_TTL,
                DEFAULT_REFRESH_INTERVAL,
                Clock.systemUTC());
    }

    public CachingJwkKeyProvider(JwkSetFetcher jwkSetFetcher, Duration negativeCacheTtl, Clock clock) {
        this(
                jwkSetFetcher,
                DEFAULT_POSITIVE_CACHE_TTL,
                DEFAULT_STALE_CACHE_TTL,
                negativeCacheTtl,
                DEFAULT_REFRESH_INTERVAL,
                clock);
    }

    public CachingJwkKeyProvider(
            JwkSetFetcher jwkSetFetcher,
            Duration positiveCacheTtl,
            Duration staleCacheTtl,
            Duration negativeCacheTtl,
            Duration refreshInterval,
            Clock clock) {
        this.jwkSetFetcher = Objects.requireNonNull(jwkSetFetcher, "jwkSetFetcher must not be null");
        this.positiveCacheTtl = requirePositive(positiveCacheTtl, "positiveCacheTtl");
        this.staleCacheTtl = requireNotNegative(staleCacheTtl, "staleCacheTtl");
        this.negativeCacheTtl = requirePositive(negativeCacheTtl, "negativeCacheTtl");
        this.refreshInterval = requirePositive(refreshInterval, "refreshInterval");
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
        CacheEntry current = cachedJwkSet;
        Instant now = Instant.now(clock);
        if (containsKid(current, requiredKid) && current.isFresh(now, positiveCacheTtl)) {
            return Mono.just(current.jwkSet());
        }
        if (isNegativeCached(requiredKid)) {
            return Mono.error(signatureInvalid("No JWK found for kid: " + requiredKid));
        }
        return current == null
                ? loadThenRefreshIfNeeded(requiredKid)
                : refreshAndRequireKid(requiredKid, current);
    }

    /**
     * 达到刷新间隔后预热一次 JWKS 缓存。
     *
     * <p>该方法供后台调度器调用。刷新失败不会清空旧缓存，避免 IAM 短暂不可用时扩大影响。</p>
     *
     * @return 当前有效或刚刷新的 JWK Set；未到刷新时间时直接返回当前缓存
     */
    public Mono<JWKSet> refreshIfDue() {
        CacheEntry current = cachedJwkSet;
        Instant now = Instant.now(clock);
        if (current != null && !isRefreshDue(now)) {
            return Mono.just(current.jwkSet());
        }
        lastRefreshAttemptAt = now;
        return fetchAndCache();
    }

    private Mono<JWKSet> loadThenRefreshIfNeeded(String kid) {
        return refresh()
                .flatMap(jwkSet -> containsKid(jwkSet, kid)
                        ? Mono.just(jwkSet)
                        : missingKid(kid))
                .onErrorMap(this::shouldWrapAsUnavailable, this::unavailable);
    }

    private Mono<JWKSet> refreshAndRequireKid(String kid, CacheEntry staleCandidate) {
        return refresh()
                .flatMap(jwkSet -> {
                    if (containsKid(jwkSet, kid)) {
                        negativeKidCache.remove(kid);
                        return Mono.just(jwkSet);
                    }
                    return missingKid(kid);
                })
                .onErrorResume(throwable -> staleOrError(kid, staleCandidate, throwable))
                .onErrorMap(this::shouldWrapAsUnavailable, this::unavailable);
    }

    private Mono<JWKSet> refresh() {
        lastRefreshAttemptAt = Instant.now(clock);
        return fetchAndCache();
    }

    private Mono<JWKSet> fetchAndCache() {
        return jwkSetFetcher.fetch()
                .map(JWKSet::toPublicJWKSet)
                .doOnNext(jwkSet -> cachedJwkSet = new CacheEntry(jwkSet, Instant.now(clock)));
    }

    private Mono<JWKSet> staleOrError(String kid, CacheEntry staleCandidate, Throwable throwable) {
        if (throwable instanceof JwtValidationException || throwable instanceof JwkSetUnavailableException) {
            return Mono.error(throwable);
        }
        if (shouldUseStale(staleCandidate, kid)) {
            return Mono.just(staleCandidate.jwkSet());
        }
        return Mono.error(throwable);
    }

    private Mono<JWKSet> missingKid(String kid) {
        negativeKidCache.put(kid, Instant.now(clock).plus(negativeCacheTtl));
        return Mono.error(signatureInvalid("No JWK found for kid: " + kid));
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

    private boolean isRefreshDue(Instant now) {
        Instant lastAttempt = lastRefreshAttemptAt;
        return lastAttempt == null || !lastAttempt.plus(refreshInterval).isAfter(now);
    }

    private boolean shouldUseStale(CacheEntry entry, String kid) {
        return containsKid(entry, kid) && entry.isStaleUsable(Instant.now(clock), positiveCacheTtl, staleCacheTtl);
    }

    private boolean shouldWrapAsUnavailable(Throwable throwable) {
        return !(throwable instanceof JwtValidationException)
                && !(throwable instanceof JwkSetUnavailableException);
    }

    private JwkSetUnavailableException unavailable(Throwable throwable) {
        return new JwkSetUnavailableException("JWKS Endpoint is unavailable", throwable);
    }

    private static boolean containsKid(CacheEntry entry, String kid) {
        return entry != null && containsKid(entry.jwkSet(), kid);
    }

    private static boolean containsKid(JWKSet jwkSet, String kid) {
        return jwkSet != null && jwkSet.getKeyByKeyId(kid) != null;
    }

    private static JwtValidationException signatureInvalid(String message) {
        return new JwtValidationException(JwtValidationException.Reason.SIGNATURE_INVALID, message);
    }

    private static Duration requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }

    private static Duration requireNotNegative(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return duration;
    }

    private record CacheEntry(JWKSet jwkSet, Instant fetchedAt) {

        private boolean isFresh(Instant now, Duration positiveCacheTtl) {
            return fetchedAt.plus(positiveCacheTtl).isAfter(now);
        }

        private boolean isStaleUsable(Instant now, Duration positiveCacheTtl, Duration staleCacheTtl) {
            Instant staleExpiresAt = fetchedAt.plus(positiveCacheTtl).plus(staleCacheTtl);
            return staleExpiresAt.isAfter(now);
        }
    }
}
