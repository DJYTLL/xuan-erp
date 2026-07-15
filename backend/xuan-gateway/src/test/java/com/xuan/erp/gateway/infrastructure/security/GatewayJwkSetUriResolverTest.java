package com.xuan.erp.gateway.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayJwkSetUriResolver} 的单元测试。
 *
 * <p>Gateway 只负责通过服务发现定位 IAM 的 JWKS 地址，真正的 HTTP 拉取、解析和缓存由
 * `xuan-common-security` 的公共组件完成。</p>
 */
class GatewayJwkSetUriResolverTest {

    private static final String IAM_SERVICE_NAME = "xuan-iam";

    // 测试可以从服务发现拿到 IAM 实例，并拼出标准 JWKS Endpoint 地址。
    @Test
    void resolvesJwkSetUriFromFirstIamInstance() {
        DiscoveryClient discoveryClient = discoveryClient(List.of(
                new DefaultServiceInstance("iam-1", IAM_SERVICE_NAME, "127.0.0.1", 18080, false)));
        GatewayJwkSetUriResolver resolver = new GatewayJwkSetUriResolver(discoveryClient, IAM_SERVICE_NAME);

        StepVerifier.create(resolver.resolve())
                .assertNext(uri -> assertThat(uri.toString()).isEqualTo("http://127.0.0.1:18080/.well-known/jwks.json"))
                .verifyComplete();
    }

    // 测试服务发现找不到 IAM 实例时，返回包含服务名的清晰异常。
    @Test
    void emitsIllegalStateExceptionWhenIamInstanceMissing() {
        GatewayJwkSetUriResolver resolver = new GatewayJwkSetUriResolver(discoveryClient(List.of()), IAM_SERVICE_NAME);

        StepVerifier.create(resolver.resolve())
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(IllegalStateException.class);
                    assertThat(throwable).hasMessageContaining(IAM_SERVICE_NAME);
                })
                .verify();
    }

    private static DiscoveryClient discoveryClient(List<ServiceInstance> instances) {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "gateway-jwk-set-uri-resolver-test";
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
}
