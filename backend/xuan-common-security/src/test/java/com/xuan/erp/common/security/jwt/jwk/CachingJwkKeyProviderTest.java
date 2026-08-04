package com.xuan.erp.common.security.jwt.jwk;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachingJwkKeyProviderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC);

    // 测试首次访问会从远程 JWKS Endpoint 拉取一次公钥集合。
    @Test
    void firstAccessFetchesJwkSet() throws Exception {
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of(jwkSet("kid-1")));
        CachingJwkKeyProvider provider = provider(fetcher);

        JWKSet jwkSet = provider.jwkSetForKid("kid-1").block();

        assertThat(jwkSet).isNotNull();
        assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull();
        assertThat(fetcher.fetchCount()).isEqualTo(1);
    }

    // 测试缓存命中时不再访问 IAM，避免每个请求都远程拉取公钥。
    @Test
    void usesCachedJwkSetWhenKidAlreadyPresent() throws Exception {
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of(jwkSet("kid-1")));
        CachingJwkKeyProvider provider = provider(fetcher);

        provider.jwkSetForKid("kid-1").block();
        provider.jwkSetForKid("kid-1").block();

        assertThat(fetcher.fetchCount()).isEqualTo(1);
    }

    // 测试正向缓存 TTL 到期后，即使目标 kid 仍存在，也会重新拉取 JWKS，避免长期使用过旧公钥。
    @Test
    void refreshesCachedJwkSetWhenPositiveCacheTtlExpires() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"));
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of(
                jwkSet("kid-1"),
                jwkSet("kid-1")));
        CachingJwkKeyProvider provider = new CachingJwkKeyProvider(
                fetcher,
                Duration.ofSeconds(5),
                Duration.ofMinutes(1),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                clock);

        provider.jwkSetForKid("kid-1").block();
        clock.advance(Duration.ofSeconds(6));
        provider.jwkSetForKid("kid-1").block();

        assertThat(fetcher.fetchCount()).isEqualTo(2);
    }

    // 测试缓存中没有目标 kid 时会刷新一次，支持 IAM 密钥轮换后新 kid 生效。
    @Test
    void refreshesOnceWhenKidMissingFromCurrentCache() throws Exception {
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of(
                jwkSet("kid-1"),
                jwkSet("kid-1", "kid-2")));
        CachingJwkKeyProvider provider = provider(fetcher);

        provider.jwkSetForKid("kid-1").block();
        JWKSet refreshed = provider.jwkSetForKid("kid-2").block();

        assertThat(refreshed).isNotNull();
        assertThat(refreshed.getKeyByKeyId("kid-2")).isNotNull();
        assertThat(fetcher.fetchCount()).isEqualTo(2);
    }

    // 测试正向缓存过期后 IAM JWKS 短暂不可用时，兜底期内可继续使用旧公钥验签。
    @Test
    void usesStaleJwkSetWhenEndpointFailsInsideStaleTtl() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"));
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of(jwkSet("kid-1")));
        fetcher.enqueueFailure(new IllegalStateException("iam down"));
        CachingJwkKeyProvider provider = new CachingJwkKeyProvider(
                fetcher,
                Duration.ofSeconds(5),
                Duration.ofMinutes(1),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                clock);

        provider.jwkSetForKid("kid-1").block();
        clock.advance(Duration.ofSeconds(6));
        JWKSet staleJwkSet = provider.jwkSetForKid("kid-1").block();

        assertThat(staleJwkSet).isNotNull();
        assertThat(staleJwkSet.getKeyByKeyId("kid-1")).isNotNull();
        assertThat(fetcher.fetchCount()).isEqualTo(2);
    }

    // 测试陈旧缓存兜底期也过期后，IAM JWKS 仍不可用时应暴露基础设施不可用。
    @Test
    void rejectsStaleJwkSetAfterStaleTtlExpires() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"));
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of(jwkSet("kid-1")));
        fetcher.enqueueFailure(new IllegalStateException("iam down"));
        CachingJwkKeyProvider provider = new CachingJwkKeyProvider(
                fetcher,
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                clock);

        provider.jwkSetForKid("kid-1").block();
        clock.advance(Duration.ofSeconds(16));

        assertThatThrownBy(() -> provider.jwkSetForKid("kid-1").block())
                .isInstanceOf(JwkSetUnavailableException.class);
    }

    // 测试达到定期刷新间隔后，Provider 可以预热最新 JWKS，后续新 kid 命中时不再额外拉取。
    @Test
    void refreshIfDueWarmsLatestJwkSetAfterRefreshInterval() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"));
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of(
                jwkSet("kid-1"),
                jwkSet("kid-1", "kid-2")));
        CachingJwkKeyProvider provider = new CachingJwkKeyProvider(
                fetcher,
                Duration.ofMinutes(10),
                Duration.ofMinutes(30),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                clock);

        provider.jwkSetForKid("kid-1").block();
        provider.refreshIfDue().block();
        clock.advance(Duration.ofSeconds(11));
        provider.refreshIfDue().block();
        provider.jwkSetForKid("kid-2").block();

        assertThat(fetcher.fetchCount()).isEqualTo(2);
    }

    // 测试刷新后仍找不到目标 kid 时会短期负缓存，避免恶意未知 kid 反复打 IAM。
    @Test
    void negativeCachesUnknownKidAfterRefreshStillMisses() throws Exception {
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of(
                jwkSet("kid-1"),
                jwkSet("kid-1")));
        CachingJwkKeyProvider provider = provider(fetcher);

        provider.jwkSetForKid("kid-1").block();
        assertThatThrownBy(() -> provider.jwkSetForKid("kid-2").block())
                .isInstanceOf(JwtValidationException.class)
                .extracting("reason")
                .isEqualTo(JwtValidationException.Reason.SIGNATURE_INVALID);
        assertThatThrownBy(() -> provider.jwkSetForKid("kid-2").block())
                .isInstanceOf(JwtValidationException.class)
                .extracting("reason")
                .isEqualTo(JwtValidationException.Reason.SIGNATURE_INVALID);

        assertThat(fetcher.fetchCount()).isEqualTo(2);
    }

    // 测试没有任何缓存且 JWKS Endpoint 不可用时，明确暴露认证基础设施不可用。
    @Test
    void throwsUnavailableWhenNoCacheAndEndpointFails() {
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(new IllegalStateException("iam down"));
        CachingJwkKeyProvider provider = provider(fetcher);

        assertThatThrownBy(() -> provider.jwkSetForKid("kid-1").block())
                .isInstanceOf(JwkSetUnavailableException.class)
                .hasMessageContaining("JWKS");
    }

    // 测试空 kid 不访问 IAM，并统一按签名无效处理。
    @Test
    void rejectsBlankKidWithoutFetchingEndpoint() {
        StubJwkSetFetcher fetcher = new StubJwkSetFetcher(List.of());
        CachingJwkKeyProvider provider = provider(fetcher);

        assertThatThrownBy(() -> provider.jwkSetForKid(" ").block())
                .isInstanceOf(JwtValidationException.class)
                .extracting("reason")
                .isEqualTo(JwtValidationException.Reason.SIGNATURE_INVALID);
        assertThat(fetcher.fetchCount()).isZero();
    }

    private static CachingJwkKeyProvider provider(StubJwkSetFetcher fetcher) {
        return new CachingJwkKeyProvider(fetcher, Duration.ofSeconds(30), CLOCK);
    }

    private static JWKSet jwkSet(String... kids) throws Exception {
        java.util.List<com.nimbusds.jose.jwk.JWK> keys = new java.util.ArrayList<>();
        for (String kid : kids) {
            keys.add(rsaKey(kid).toPublicJWK());
        }
        return new JWKSet(keys);
    }

    private static RSAKey rsaKey(String kid) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .algorithm(JWSAlgorithm.RS256)
                .keyID(kid)
                .build();
    }

    private static final class StubJwkSetFetcher implements JwkSetFetcher {

        private final AtomicInteger fetchCount = new AtomicInteger();
        private final ConcurrentLinkedDeque<Object> responses;

        private StubJwkSetFetcher(List<JWKSet> responses) {
            this.responses = new ConcurrentLinkedDeque<>(responses);
        }

        private StubJwkSetFetcher(RuntimeException failure) {
            this.responses = new ConcurrentLinkedDeque<>();
            this.responses.add(failure);
        }

        private void enqueueFailure(RuntimeException failure) {
            responses.add(failure);
        }

        @Override
        public Mono<JWKSet> fetch() {
            fetchCount.incrementAndGet();
            Object response = responses.pollFirst();
            if (response == null) {
                return Mono.error(new AssertionError("测试未准备足够的 JWKS 响应"));
            }
            if (response instanceof RuntimeException exception) {
                return Mono.error(exception);
            }
            return Mono.just((JWKSet) response);
        }

        private int fetchCount() {
            return fetchCount.get();
        }
    }

    private static final class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        private void advance(Duration duration) {
            instant.updateAndGet(current -> current.plus(duration));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
