package com.xuan.erp.gateway.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayJwkSetFetcher} 的单元测试。
 *
 * <p>这里重点验证两件事：一是网关能否通过服务发现拼出 IAM 的
 * JWKS 地址并成功拉取；二是当服务发现没有可用实例时，是否能
 * 返回清晰可诊断的异常。</p>
 */
class GatewayJwkSetFetcherTest {

    private static final String IAM_SERVICE_NAME = "xuan-iam";
    private static final String JWKS_JSON = """
            {
              "keys": [
                {
                  "kty": "RSA",
                  "kid": "kid-1",
                  "use": "sig",
                  "alg": "RS256",
                  "n": "sXch3kC1X8JD7BqXc9Ejo4kKDAdAm8X1sC3yRyvE4s-lHoPazTA_gkGEXLMaLLq5yRvCNrI6VsgGAaHo9dZYkTfLZNVVRFl1FdYzfAzS-fWznuaCG2l9VmOAXoJ1i8LJIYurx8WcN6i_K3PaY5E9OQj40YfJOLLmObAun1vDLteA94E",
                  "e": "AQAB"
                }
              ]
            }
            """;

    // 测试可以从服务发现拿到 IAM 实例，并成功拉取并解析出 kid-1 的 JWK。
    @Test
    void fetchesJwkSetFromFirstIamInstance() {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction(requestedUrl, JWKS_JSON))
                .build();
        DiscoveryClient discoveryClient = discoveryClient(List.of(
                new DefaultServiceInstance("iam-1", IAM_SERVICE_NAME, "127.0.0.1", 18080, false)));

        StepVerifier.create(fetch(discoveryClient, webClient, IAM_SERVICE_NAME))
                .assertNext(jwkSet -> assertThat(keyByKid(jwkSet, "kid-1")).isNotNull())
                .verifyComplete();

        assertThat(requestedUrl.get()).isEqualTo("http://127.0.0.1:18080/.well-known/jwks.json");
    }

    // 测试当服务发现找不到 IAM 实例时，返回包含服务名的 IllegalStateException。
    @Test
    void emitsIllegalStateExceptionWhenIamInstanceMissing() {
        DiscoveryClient discoveryClient = discoveryClient(List.of());
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction(new AtomicReference<>(), "{\"keys\":[]}"))
                .build();

        StepVerifier.create(fetch(discoveryClient, webClient, IAM_SERVICE_NAME))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(IllegalStateException.class);
                    assertThat(throwable).hasMessageContaining(IAM_SERVICE_NAME);
                })
                .verify();
    }

    /**
     * 通过反射调用待测类，避免测试类在发现阶段直接绑定 Nimbus 类型。
     */
    private static Mono<?> fetch(DiscoveryClient discoveryClient, WebClient webClient, String iamServiceName) {
        try {
            Class<?> fetcherClass = Class.forName(
                    "com.xuan.erp.gateway.infrastructure.security.GatewayJwkSetFetcher");
            Constructor<?> constructor = fetcherClass.getDeclaredConstructor(
                    DiscoveryClient.class, WebClient.class, String.class);
            Object fetcher = constructor.newInstance(discoveryClient, webClient, iamServiceName);
            Method fetchMethod = fetcherClass.getDeclaredMethod("fetch");
            return (Mono<?>) fetchMethod.invoke(fetcher);
        } catch (ClassNotFoundException ex) {
            return Mono.error(new AssertionError("GatewayJwkSetFetcher 尚未实现", ex));
        } catch (NoSuchMethodException ex) {
            return Mono.error(new AssertionError("GatewayJwkSetFetcher 构造器或 fetch 方法签名不符合测试预期", ex));
        } catch (InstantiationException | IllegalAccessException ex) {
            return Mono.error(new AssertionError("GatewayJwkSetFetcher 无法实例化", ex));
        } catch (InvocationTargetException ex) {
            return Mono.error(ex.getTargetException());
        }
    }

    /**
     * 通过反射从返回的 JWKSet 中按 kid 取出公钥，验证解析结果。
     */
    private static Object keyByKid(Object jwkSet, String kid) {
        try {
            Method method = jwkSet.getClass().getMethod("getKeyByKeyId", String.class);
            return method.invoke(jwkSet, kid);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new AssertionError("JWKSet 反射调用失败", ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("JWKSet#getKeyByKeyId 执行失败", ex.getTargetException());
        }
    }

    /**
     * 构造一个最小化的 DiscoveryClient 测试替身。
     */
    private static DiscoveryClient discoveryClient(List<ServiceInstance> instances) {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "gateway-jwk-set-fetcher-test";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                if (IAM_SERVICE_NAME.equals(serviceId)) {
                    return instances;
                }
                return List.of();
            }

            @Override
            public List<String> getServices() {
                if (instances.isEmpty()) {
                    return List.of();
                }
                return List.of(IAM_SERVICE_NAME);
            }
        };
    }

    /**
     * 构造返回固定 JWKS 内容的 HTTP 交换函数，模拟 IAM 的 JWKS 响应。
     */
    private static ExchangeFunction exchangeFunction(AtomicReference<String> requestedUrl, String responseBody) {
        return request -> {
            requestedUrl.set(request.url().toString());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(responseBody)
                    .build());
        };
    }
}
