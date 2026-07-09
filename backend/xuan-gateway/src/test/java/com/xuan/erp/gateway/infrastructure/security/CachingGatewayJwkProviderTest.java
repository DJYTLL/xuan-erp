package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CachingGatewayJwkProvider} 的单元测试。
 *
 * <p>重点覆盖缓存命中、冷启动刷新、缓存 miss 刷新一次以及
 * 最终失败时统一转成 SIGNATURE_INVALID 的行为。</p>
 */
class CachingGatewayJwkProviderTest {

    // 测试首次访问会拉取一次 JWKS，并把包含目标 kid 的结果返回给调用方。
    @Test
    void firstAccessFetchesJwkSet() throws Exception {
        StubGatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(List.of(jwkSet("kid-1")));
        ProviderHandle provider = provider(fetcher);

        StepVerifier.create(provider.jwkSetForKid("kid-1"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull())
                .verifyComplete();

        assertThat(fetcher.fetchCount()).isEqualTo(1);
    }

    // 测试冷启动第一次拉取不含目标 kid 时，会再刷新一次，并返回第二次命中的 JWKS。
    @Test
    void refreshesAgainOnColdStartWhenFirstFetchMissesKid() throws Exception {
        StubGatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(List.of(
                jwkSet("kid-1"),
                jwkSet("kid-1", "kid-2")));
        ProviderHandle provider = provider(fetcher);

        StepVerifier.create(provider.jwkSetForKid("kid-2"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-2")).isNotNull())
                .verifyComplete();

        assertThat(fetcher.fetchCount()).isEqualTo(2);
    }

    // 测试缓存里已经命中 kid 时直接复用缓存，不会重复向 IAM 拉取 JWKS。
    @Test
    void usesCachedJwkSetWhenKidAlreadyPresent() throws Exception {
        StubGatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(List.of(jwkSet("kid-1")));
        ProviderHandle provider = provider(fetcher);

        StepVerifier.create(provider.jwkSetForKid("kid-1"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull())
                .verifyComplete();
        StepVerifier.create(provider.jwkSetForKid("kid-1"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull())
                .verifyComplete();

        assertThat(fetcher.fetchCount()).isEqualTo(1);
    }

    // 测试当前缓存不含目标 kid 时，会额外刷新一次，并返回刷新后命中的 JWKS。
    @Test
    void refreshesOnceWhenKidMissingFromCurrentCache() throws Exception {
        StubGatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(List.of(
                jwkSet("kid-1"),
                jwkSet("kid-1", "kid-2")));
        ProviderHandle provider = provider(fetcher);

        StepVerifier.create(provider.jwkSetForKid("kid-1"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull())
                .verifyComplete();
        StepVerifier.create(provider.jwkSetForKid("kid-2"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-2")).isNotNull())
                .verifyComplete();

        assertThat(fetcher.fetchCount()).isEqualTo(2);
    }

    // 测试刷新后仍找不到目标 kid 时，统一抛出 SIGNATURE_INVALID，供上层按验签失败处理。
    @Test
    void throwsSignatureInvalidWhenKidStillMissingAfterRefresh() throws Exception {
        StubGatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(List.of(
                jwkSet("kid-1"),
                jwkSet("kid-1")));
        ProviderHandle provider = provider(fetcher);

        StepVerifier.create(provider.jwkSetForKid("kid-1"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull())
                .verifyComplete();
        StepVerifier.create(provider.jwkSetForKid("kid-2"))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable.getClass().getName())
                            .isEqualTo("com.xuan.erp.common.security.jwt.JwtValidationException");
                    assertThat(jwtValidationReason(throwable)).isEqualTo("SIGNATURE_INVALID");
                })
                .verify();

        assertThat(fetcher.fetchCount()).isEqualTo(2);
    }

    // 测试 null、空串和纯空白 kid 都统一抛出 SIGNATURE_INVALID，避免泄漏实现细节异常类型。
    @Test
    void throwsSignatureInvalidWhenKidIsBlankOrInvalid() throws Exception {
        StubGatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(List.of(jwkSet("kid-1")));
        ProviderHandle provider = provider(fetcher);

        for (String invalidKid : Arrays.asList(null, "", "   ")) {
            StepVerifier.create(provider.jwkSetForKid(invalidKid))
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable.getClass().getName())
                                .isEqualTo("com.xuan.erp.common.security.jwt.JwtValidationException");
                        assertThat(jwtValidationReason(throwable)).isEqualTo("SIGNATURE_INVALID");
                    })
                    .verify();
        }

        assertThat(fetcher.fetchCount()).isZero();
    }

    /**
     * 通过反射构造待测 Provider，确保方法签名与计划契约保持一致。
     */
    private static ProviderHandle provider(GatewayJwkSetFetcher fetcher) {
        try {
            Class<?> providerClass = Class.forName(
                    "com.xuan.erp.gateway.infrastructure.security.CachingGatewayJwkProvider");
            Constructor<?> constructor = providerClass.getDeclaredConstructor(GatewayJwkSetFetcher.class);
            Object provider = constructor.newInstance(fetcher);
            Method method = providerClass.getDeclaredMethod("jwkSetForKid", String.class);
            return new ProviderHandle(provider, method);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("CachingGatewayJwkProvider 尚未实现", ex);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("CachingGatewayJwkProvider 构造器或 jwkSetForKid 签名不符合测试预期", ex);
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new AssertionError("CachingGatewayJwkProvider 无法实例化", ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("CachingGatewayJwkProvider 初始化失败", ex.getTargetException());
        }
    }

    /**
     * 构造仅包含公钥的测试 JWKSet。
     */
    private static JWKSet jwkSet(String... kids) throws Exception {
        java.util.List<com.nimbusds.jose.jwk.JWK> keys = new java.util.ArrayList<>();
        for (String kid : kids) {
            keys.add(rsaKey(kid).toPublicJWK());
        }
        return new JWKSet(keys);
    }

    /**
     * 生成一个带指定 kid 的 RSA 测试密钥。
     */
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

    /**
     * 通过反射读取 JwtValidationException 的 reason，避免直接绑定实现细节。
     */
    private static String jwtValidationReason(Throwable throwable) {
        try {
            Method method = throwable.getClass().getMethod("reason");
            Object reason = method.invoke(throwable);
            return String.valueOf(reason);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new AssertionError("JwtValidationException#reason 反射调用失败", ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("JwtValidationException#reason 执行失败", ex.getTargetException());
        }
    }

    /**
     * 反射调用 Provider 的轻量包装，减少测试代码与待测类型的直接耦合。
     */
    private static final class ProviderHandle {

        private final Object provider;
        private final Method jwkSetForKidMethod;

        private ProviderHandle(Object provider, Method jwkSetForKidMethod) {
            this.provider = provider;
            this.jwkSetForKidMethod = jwkSetForKidMethod;
        }

        @SuppressWarnings("unchecked")
        private Mono<JWKSet> jwkSetForKid(String kid) {
            try {
                return (Mono<JWKSet>) jwkSetForKidMethod.invoke(provider, kid);
            } catch (IllegalAccessException ex) {
                return Mono.error(new AssertionError("CachingGatewayJwkProvider#jwkSetForKid 无法访问", ex));
            } catch (InvocationTargetException ex) {
                return Mono.error(ex.getTargetException());
            }
        }
    }

    /**
     * JWKS 抓取器测试替身。
     *
     * <p>该替身不会真的发 HTTP 请求，而是按顺序返回预设的 JWKSet，
     * 便于验证 Provider 的缓存与刷新策略。</p>
     */
    private static final class StubGatewayJwkSetFetcher extends GatewayJwkSetFetcher {

        private final AtomicInteger fetchCount = new AtomicInteger();
        private final Deque<JWKSet> responses;

        private StubGatewayJwkSetFetcher(List<JWKSet> responses) {
            super(discoveryClient(), WebClient.builder().exchangeFunction(failingExchangeFunction()).build(), "xuan-iam");
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public Mono<JWKSet> fetch() {
            fetchCount.incrementAndGet();
            JWKSet response = responses.pollFirst();
            if (response == null) {
                return Mono.error(new AssertionError("测试未准备足够的 JWKS 响应"));
            }
            return Mono.just(response);
        }

        private int fetchCount() {
            return fetchCount.get();
        }

        /**
         * 提供一个不会返回真实实例的最小 DiscoveryClient 占位实现。
         */
        private static DiscoveryClient discoveryClient() {
            return new DiscoveryClient() {
                @Override
                public String description() {
                    return "caching-gateway-jwk-provider-test";
                }

                @Override
                public List<ServiceInstance> getInstances(String serviceId) {
                    return List.of();
                }

                @Override
                public List<String> getServices() {
                    return List.of();
                }
            };
        }

        /**
         * 构造一个失败型 ExchangeFunction，防止测试误发真实 HTTP 请求。
         */
        private static ExchangeFunction failingExchangeFunction() {
            return request -> Mono.error(new AssertionError(
                    "CachingGatewayJwkProviderTest 不应该真的发出 HTTP 请求: " + requestDescription(request)));
        }

        /**
         * 输出误发请求时的请求摘要，便于排查问题。
         */
        private static String requestDescription(ClientRequest request) {
            return request.method() + " " + request.url();
        }
    }
}
