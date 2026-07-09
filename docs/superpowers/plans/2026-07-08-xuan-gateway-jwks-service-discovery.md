# Xuan Gateway JWKS Service Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `xuan-gateway` 补齐“通过服务发现发现 `xuan-iam`、拉取 `/.well-known/jwks.json`、缓存公钥并完成入口 Bearer 验签”的最小认证闭环。

**Architecture:** 保持实现边界只在 `xuan-gateway` 内：`GatewayJwkSetFetcher` 负责通过 `DiscoveryClient + WebClient` 拉取 IAM 的 JWKS，`CachingGatewayJwkProvider` 负责内存缓存与按 `kid` 刷新，`GatewayBearerAuthenticationWebFilter` 负责提取 Bearer Token、预读 JWT header 中的 `kid`、决定是否刷新缓存并调用 `JwkJwtTokenParser` 完成验签，`GatewaySecurityConfiguration` 负责把该过滤器接入 WebFlux Security 并放行 `actuator/health` / `actuator/info`。

**Tech Stack:** Spring Boot, Spring Cloud Gateway, Spring Security WebFlux, WebClient, Nacos Discovery, Nimbus JOSE JWT, JUnit 5, Mockito, Reactor Test

---

## File Map

### Create

- `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityProperties.java`
  - 网关安全配置属性，承载 `enabled`、`issuer`、`audience`、`iamServiceName`。
- `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityConfiguration.java`
  - 装配 `WebClient`、`GatewayJwkSetFetcher`、`CachingGatewayJwkProvider`、`SecurityWebFilterChain`。
- `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/GatewayJwkSetFetcher.java`
  - 通过服务发现定位 `xuan-iam` 并拉取 `/.well-known/jwks.json`。
- `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/CachingGatewayJwkProvider.java`
  - 维护最近一次成功拉取的 `JWKSet`，并提供“首次加载 / 按 `kid` 刷新”的能力。
- `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/GatewayBearerAuthenticationWebFilter.java`
  - 提取 Bearer Token、预读 `kid`、调用 Provider 与 `JwkJwtTokenParser`、把 `CurrentUser` 写入 Reactor 安全上下文。
- `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/infrastructure/security/GatewayJwkSetFetcherTest.java`
  - 验证服务发现 + JWKS 拉取行为。
- `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/infrastructure/security/CachingGatewayJwkProviderTest.java`
  - 验证首次加载、命中缓存、`kid` 不存在时刷新一次、刷新后仍缺失时报错。
- `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/GatewaySecurityIntegrationTest.java`
  - 验证 WebFlux 安全链路：无 token、合法 token、未知 `kid`、过期 token、`actuator` 放行。

### Modify

- `backend/xuan-gateway/pom.xml`
  - 增加测试依赖：`spring-boot-starter-test`、`spring-security-test`。
- `backend/xuan-gateway/src/main/resources/application.yml`
  - 增加 `xuan.gateway.security.*` 默认配置，默认与 IAM 当前最小契约一致：`issuer=xuan-iam`、`audience=xuan-gateway`、`iam-service-name=xuan-iam`。

## Task 1: 先把 JWKS 拉取测试写红

**Files:**
- Modify: `backend/xuan-gateway/pom.xml`
- Create: `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/infrastructure/security/GatewayJwkSetFetcherTest.java`

- [ ] **Step 1: 增加网关模块测试依赖**

在 `backend/xuan-gateway/pom.xml` 追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 写 `GatewayJwkSetFetcherTest` 的失败用例**

创建 `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/infrastructure/security/GatewayJwkSetFetcherTest.java`：

```java
package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayJwkSetFetcherTest {

    @Test
    void loadsJwkSetFromDiscoveredIamInstance() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("kid-1").algorithm(JWSAlgorithm.RS256).generate();
        DiscoveryClient discoveryClient = new StubDiscoveryClient(List.of(
                new DefaultServiceInstance("iam-1", "xuan-iam", "127.0.0.1", 8101, false)));
        WebClient webClient = WebClient.builder()
                .exchangeFunction(okJson("{\"keys\":[" + rsaKey.toPublicJWK().toJSONString() + "]}"))
                .build();

        GatewayJwkSetFetcher fetcher = new GatewayJwkSetFetcher(discoveryClient, webClient, "xuan-iam");

        StepVerifier.create(fetcher.fetch())
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull())
                .verifyComplete();
    }

    @Test
    void failsWhenDiscoveryReturnsNoIamInstance() {
        GatewayJwkSetFetcher fetcher = new GatewayJwkSetFetcher(
                new StubDiscoveryClient(List.of()),
                WebClient.builder().build(),
                "xuan-iam");

        StepVerifier.create(fetcher.fetch())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("xuan-iam"))
                .verify();
    }

    private static ExchangeFunction okJson(String body) {
        return request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private record StubDiscoveryClient(List<org.springframework.cloud.client.ServiceInstance> instances)
            implements DiscoveryClient {
        @Override
        public String description() {
            return "stub";
        }
        @Override
        public List<org.springframework.cloud.client.ServiceInstance> getInstances(String serviceId) {
            return instances;
        }
        @Override
        public List<String> getServices() {
            return List.of("xuan-iam");
        }
    }
}
```

- [ ] **Step 3: 运行定向测试，确认先失败**

运行（工作目录 `D:\xuan-erp\backend`）：

```bash
mvn -pl xuan-gateway -Dtest=GatewayJwkSetFetcherTest test
```

Expected: FAIL，报 `GatewayJwkSetFetcher` 不存在，或 `spring-boot-starter-test` 相关类型尚未解析。

- [ ] **Step 4: 实现最小 `GatewayJwkSetFetcher`**

创建 `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/GatewayJwkSetFetcher.java`：

```java
package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.List;

public class GatewayJwkSetFetcher {

    private final DiscoveryClient discoveryClient;
    private final WebClient webClient;
    private final String iamServiceName;

    public GatewayJwkSetFetcher(DiscoveryClient discoveryClient, WebClient webClient, String iamServiceName) {
        this.discoveryClient = discoveryClient;
        this.webClient = webClient;
        this.iamServiceName = iamServiceName;
    }

    public Mono<JWKSet> fetch() {
        List<ServiceInstance> instances = discoveryClient.getInstances(iamServiceName);
        if (instances == null || instances.isEmpty()) {
            return Mono.error(new IllegalStateException("No instance available for " + iamServiceName));
        }
        ServiceInstance instance = instances.get(0);
        String jwksUri = instance.getUri().toString() + "/.well-known/jwks.json";
        return webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseJwkSet);
    }

    private JWKSet parseJwkSet(String body) {
        try {
            return JWKSet.parse(body);
        } catch (ParseException ex) {
            throw new IllegalStateException("Failed to parse IAM JWK set", ex);
        }
    }
}
```

- [ ] **Step 5: 重新运行定向测试，确认转绿**

运行：

```bash
mvn -pl xuan-gateway -Dtest=GatewayJwkSetFetcherTest test
```

Expected: PASS，2 个测试通过。

- [ ] **Step 6: 提交这一小步**

```bash
git add backend/xuan-gateway/pom.xml backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/GatewayJwkSetFetcher.java backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/infrastructure/security/GatewayJwkSetFetcherTest.java
git commit -m "test: cover gateway jwks fetching"
```

## Task 2: 做出按 `kid` 刷新的缓存 Provider

**Files:**
- Create: `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/CachingGatewayJwkProvider.java`
- Create: `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/infrastructure/security/CachingGatewayJwkProviderTest.java`

- [ ] **Step 1: 写 Provider 的失败测试**

创建 `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/infrastructure/security/CachingGatewayJwkProviderTest.java`：

```java
package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CachingGatewayJwkProviderTest {

    @Test
    void loadsJwkSetOnlyOnceWhenKidAlreadyPresent() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("kid-1").algorithm(JWSAlgorithm.RS256).generate();
        AtomicInteger counter = new AtomicInteger();
        GatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(() -> {
            counter.incrementAndGet();
            return Mono.just(new JWKSet(rsaKey.toPublicJWK()));
        });

        CachingGatewayJwkProvider provider = new CachingGatewayJwkProvider(fetcher);

        StepVerifier.create(provider.jwkSetForKid("kid-1"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull())
                .verifyComplete();
        StepVerifier.create(provider.jwkSetForKid("kid-1"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull())
                .verifyComplete();

        assertThat(counter).hasValue(1);
    }

    @Test
    void refreshesOnceWhenKidMissingInCache() throws Exception {
        RSAKey oldKey = new RSAKeyGenerator(2048).keyID("kid-old").algorithm(JWSAlgorithm.RS256).generate();
        RSAKey newKey = new RSAKeyGenerator(2048).keyID("kid-new").algorithm(JWSAlgorithm.RS256).generate();
        AtomicInteger counter = new AtomicInteger();
        GatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(() -> {
            if (counter.getAndIncrement() == 0) {
                return Mono.just(new JWKSet(oldKey.toPublicJWK()));
            }
            return Mono.just(new JWKSet(newKey.toPublicJWK()));
        });

        CachingGatewayJwkProvider provider = new CachingGatewayJwkProvider(fetcher);

        StepVerifier.create(provider.jwkSetForKid("kid-old"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-old")).isNotNull())
                .verifyComplete();
        StepVerifier.create(provider.jwkSetForKid("kid-new"))
                .assertNext(jwkSet -> assertThat(jwkSet.getKeyByKeyId("kid-new")).isNotNull())
                .verifyComplete();
    }

    @Test
    void failsWhenKidStillMissingAfterRefresh() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("kid-1").algorithm(JWSAlgorithm.RS256).generate();
        GatewayJwkSetFetcher fetcher = new StubGatewayJwkSetFetcher(() -> Mono.just(new JWKSet(rsaKey.toPublicJWK())));
        CachingGatewayJwkProvider provider = new CachingGatewayJwkProvider(fetcher);

        StepVerifier.create(provider.jwkSetForKid("kid-missing"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(JwtValidationException.class);
                    JwtValidationException exception = (JwtValidationException) error;
                    assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.SIGNATURE_INVALID);
                })
                .verify();
    }

    private static final class StubGatewayJwkSetFetcher extends GatewayJwkSetFetcher {
        private final java.util.function.Supplier<Mono<JWKSet>> loader;
        private StubGatewayJwkSetFetcher(java.util.function.Supplier<Mono<JWKSet>> loader) {
            super(null, null, "xuan-iam");
            this.loader = loader;
        }
        @Override
        public Mono<JWKSet> fetch() {
            return loader.get();
        }
    }
}
```

- [ ] **Step 2: 运行 Provider 测试，确认先失败**

```bash
mvn -pl xuan-gateway -Dtest=CachingGatewayJwkProviderTest test
```

Expected: FAIL，报 `CachingGatewayJwkProvider` 不存在，或 `GatewayJwkSetFetcher#fetch()` 无法被覆写。

- [ ] **Step 3: 实现 `CachingGatewayJwkProvider`，并允许 fetcher 被测试覆写**

先把 `GatewayJwkSetFetcher` 的 `fetch()` 改成 `public Mono<JWKSet> fetch()` 保持可覆写；然后创建 `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/CachingGatewayJwkProvider.java`：

```java
package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import reactor.core.publisher.Mono;

public class CachingGatewayJwkProvider {

    private final GatewayJwkSetFetcher fetcher;
    private volatile JWKSet cached;

    public CachingGatewayJwkProvider(GatewayJwkSetFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public Mono<JWKSet> jwkSetForKid(String kid) {
        return currentOrLoad()
                .flatMap(current -> current.getKeyByKeyId(kid) != null
                        ? Mono.just(current)
                        : refreshAndRequireKid(kid));
    }

    private Mono<JWKSet> currentOrLoad() {
        if (cached != null) {
            return Mono.just(cached);
        }
        return refresh();
    }

    public Mono<JWKSet> refresh() {
        return fetcher.fetch()
                .doOnNext(jwkSet -> this.cached = jwkSet);
    }

    private Mono<JWKSet> refreshAndRequireKid(String kid) {
        return refresh().flatMap(refreshed -> {
            if (refreshed.getKeyByKeyId(kid) != null) {
                return Mono.just(refreshed);
            }
            return Mono.error(new JwtValidationException(
                    JwtValidationException.Reason.SIGNATURE_INVALID,
                    "No RSA public key found for JWT kid " + kid));
        });
    }
}
```

- [ ] **Step 4: 重跑 Provider 测试，确认缓存和刷新逻辑通过**

```bash
mvn -pl xuan-gateway -Dtest=CachingGatewayJwkProviderTest test
```

Expected: PASS，3 个测试通过。

- [ ] **Step 5: 提交 Provider 小步**

```bash
git add backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/CachingGatewayJwkProvider.java backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/GatewayJwkSetFetcher.java backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/infrastructure/security/CachingGatewayJwkProviderTest.java
git commit -m "feat: cache gateway jwk set by kid"
```

## Task 3: 接上 WebFlux 安全链路并写集成测试

**Files:**
- Create: `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityProperties.java`
- Create: `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityConfiguration.java`
- Create: `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/GatewayBearerAuthenticationWebFilter.java`
- Create: `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/GatewaySecurityIntegrationTest.java`
- Modify: `backend/xuan-gateway/src/main/resources/application.yml`

- [ ] **Step 1: 先写安全集成测试**

创建 `backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/GatewaySecurityIntegrationTest.java`：

```java
package com.xuan.erp.gateway;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.jwt.TokenType;
import com.xuan.erp.gateway.infrastructure.security.CachingGatewayJwkProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "xuan.gateway.security.enabled=true",
                "xuan.gateway.security.issuer=xuan-iam",
                "xuan.gateway.security.audience=xuan-gateway",
                "xuan.gateway.security.iam-service-name=xuan-iam"
        })
@AutoConfigureWebTestClient
@Import(GatewaySecurityIntegrationTest.TestEndpoints.class)
class GatewaySecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CachingGatewayJwkProvider jwkProvider;

    private RSAKey rsaKey;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("kid-1").algorithm(JWSAlgorithm.RS256).generate();
    }

    @Test
    void returnsUnauthorizedWhenTokenMissing() {
        webTestClient.get()
                .uri("/api/test/secured")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void allowsRequestWithValidIamAccessToken() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.now().plusSeconds(300)))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("secured");
    }

    @Test
    void returnsUnauthorizedWhenKidStillMissingAfterRefresh() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-missing"))
                .thenReturn(Mono.error(new com.xuan.erp.common.security.jwt.JwtValidationException(
                        com.xuan.erp.common.security.jwt.JwtValidationException.Reason.SIGNATURE_INVALID,
                        "missing kid")));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-missing", Instant.now().plusSeconds(300)))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void returnsUnauthorizedWhenTokenExpired() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.now().minusSeconds(10)))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void keepsActuatorEndpointsAnonymous() {
        webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
        webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
    }

    private static String signedToken(RSAKey rsaKey, String kid, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("xuan-iam")
                .audience("xuan-gateway")
                .issueTime(Date.from(expiresAt.minusSeconds(60)))
                .expirationTime(Date.from(expiresAt))
                .claim("tokenType", TokenType.ACCESS.name())
                .claim("userId", 1L)
                .claim("tenantId", 1001L)
                .claim("username", "gateway-user")
                .claim("roles", List.of("tenant_admin"))
                .claim("authVersion", 7L)
                .claim("permissions", List.of("iam:view"))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), claims);
        signedJWT.sign(new RSASSASigner(rsaKey));
        return signedJWT.serialize();
    }

    @RestController
    static class TestEndpoints {
        @GetMapping("/api/test/secured")
        String secured() {
            return "secured";
        }
    }
}
```

- [ ] **Step 2: 跑集成测试，确认先失败**

```bash
mvn -pl xuan-gateway -Dtest=GatewaySecurityIntegrationTest test
```

Expected: FAIL，报 `GatewaySecurityConfiguration`、`GatewayBearerAuthenticationWebFilter` 或配置属性类不存在。

- [ ] **Step 3: 实现配置属性与安全配置**

创建 `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityProperties.java`：

```java
package com.xuan.erp.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xuan.gateway.security")
public class GatewaySecurityProperties {

    private boolean enabled = true;
    private String issuer = "xuan-iam";
    private String audience = "xuan-gateway";
    private String iamServiceName = "xuan-iam";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getIamServiceName() { return iamServiceName; }
    public void setIamServiceName(String iamServiceName) { this.iamServiceName = iamServiceName; }
}
```

创建 `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityConfiguration.java`：

```java
package com.xuan.erp.gateway.infrastructure.config;

import com.xuan.erp.gateway.infrastructure.security.CachingGatewayJwkProvider;
import com.xuan.erp.gateway.infrastructure.security.GatewayBearerAuthenticationWebFilter;
import com.xuan.erp.gateway.infrastructure.security.GatewayJwkSetFetcher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfiguration {

    @Bean
    WebClient gatewayJwkWebClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    GatewayJwkSetFetcher gatewayJwkSetFetcher(
            org.springframework.cloud.client.discovery.DiscoveryClient discoveryClient,
            WebClient gatewayJwkWebClient,
            GatewaySecurityProperties properties) {
        return new GatewayJwkSetFetcher(discoveryClient, gatewayJwkWebClient, properties.getIamServiceName());
    }

    @Bean
    CachingGatewayJwkProvider cachingGatewayJwkProvider(GatewayJwkSetFetcher fetcher) {
        return new CachingGatewayJwkProvider(fetcher);
    }

    @Bean
    SecurityWebFilterChain gatewaySecurityWebFilterChain(
            ServerHttpSecurity http,
            GatewayBearerAuthenticationWebFilter bearerFilter,
            GatewaySecurityProperties properties) {
        if (!properties.isEnabled()) {
            return http.csrf(ServerHttpSecurity.CsrfSpec::disable).build();
        }
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .exceptionHandling(spec -> spec.authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }))
                .authorizeExchange(spec -> spec
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(bearerFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
```

- [ ] **Step 4: 实现 Bearer 过滤器，先读 `kid` 再决定是否刷新**

创建 `backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/GatewayBearerAuthenticationWebFilter.java`：

```java
package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.gateway.infrastructure.config.GatewaySecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.stream.Collectors;

@Component
public class GatewayBearerAuthenticationWebFilter implements WebFilter {

    private final BearerTokenResolver bearerTokenResolver = new BearerTokenResolver();
    private final CachingGatewayJwkProvider jwkProvider;
    private final GatewaySecurityProperties properties;

    public GatewayBearerAuthenticationWebFilter(
            CachingGatewayJwkProvider jwkProvider,
            GatewaySecurityProperties properties) {
        this.jwkProvider = jwkProvider;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return Mono.justOrEmpty(bearerTokenResolver.resolve(authorization))
                .flatMap(token -> authenticate(token)
                        .flatMap(authentication -> chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))))
                .switchIfEmpty(chain.filter(exchange))
                .onErrorResume(ex -> unauthorized(exchange));
    }

    private Mono<UsernamePasswordAuthenticationToken> authenticate(String token) {
        String kid = readKid(token);
        return jwkProvider.jwkSetForKid(kid)
                .map(jwkSet -> new JwkJwtTokenParser(jwkSet, properties.getIssuer(), properties.getAudience()))
                .map(parser -> parser.parseAccessToken(token))
                .map(this::toAuthentication);
    }

    private String readKid(String token) {
        try {
            return SignedJWT.parse(token).getHeader().getKeyID();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("JWT token format is invalid", ex);
        }
    }

    private UsernamePasswordAuthenticationToken toAuthentication(CurrentUser currentUser) {
        return new UsernamePasswordAuthenticationToken(
                currentUser,
                "N/A",
                currentUser.permissions().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
```

- [ ] **Step 5: 在 `application.yml` 增加默认配置**

把以下片段加入 `backend/xuan-gateway/src/main/resources/application.yml`：

```yaml
xuan:
  gateway:
    security:
      enabled: true
      issuer: xuan-iam
      audience: xuan-gateway
      iam-service-name: xuan-iam
```

- [ ] **Step 6: 重跑安全集成测试**

```bash
mvn -pl xuan-gateway -Dtest=GatewaySecurityIntegrationTest test
```

Expected: PASS，覆盖无 token、合法 token、未知 `kid`、过期 token、`actuator` 放行。

- [ ] **Step 7: 提交安全链路**

```bash
git add backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityProperties.java backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityConfiguration.java backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/security/GatewayBearerAuthenticationWebFilter.java backend/xuan-gateway/src/main/resources/application.yml backend/xuan-gateway/src/test/java/com/xuan/erp/gateway/GatewaySecurityIntegrationTest.java
git commit -m "feat: verify gateway jwt via iam jwks"
```

## Task 4: 做整体验证和回归

**Files:**
- Verify: `backend/xuan-gateway`

- [ ] **Step 1: 跑三组定向测试，确认红绿闭环都还在**

```bash
mvn -pl xuan-gateway -Dtest=GatewayJwkSetFetcherTest,CachingGatewayJwkProviderTest,GatewaySecurityIntegrationTest test
```

Expected: PASS。

- [ ] **Step 2: 跑网关模块完整测试**

```bash
mvn -pl xuan-gateway -am test
```

Expected: PASS，`xuan-gateway` 和所需上游模块测试全部通过。

- [ ] **Step 3: 做配置与日志人工复查**

检查点：

```text
1. application.yml 中 issuer / audience / iam-service-name 与 IAM 当前最小契约一致。
2. SecurityWebFilterChain 只放行 /actuator/health 和 /actuator/info。
3. 网关代码中没有任何私钥配置项或私钥解析逻辑。
4. 新增类和测试保留中文注释，便于后续排查。
```

- [ ] **Step 4: 如需联调，再跑网关模块启动验证**

运行（工作目录 `D:\xuan-erp\backend`）：

```bash
mvn -pl xuan-gateway spring-boot:run
```

Expected: 本地能启动；若本机 Nacos / IAM 可达，网关首次收到 Bearer 请求时可通过服务发现拉取 JWKS。

- [ ] **Step 5: 最后提交验证收尾**

```bash
git status --short
```

Expected: 工作区干净，或只剩用户明确保留的未跟踪文件。

## Self-Review

### 1. Spec coverage

- 服务发现定位 `xuan-iam`：Task 1。
- 拉取 `/.well-known/jwks.json`：Task 1。
- 本地缓存与未知 `kid` 刷新一次：Task 2。
- Bearer 验签与 `CurrentUser` 上下文：Task 3。
- 放行 `/actuator/health`、`/actuator/info`：Task 3。
- 无 token / 合法 token / 未知 `kid` / 过期 token 验收：Task 3。
- 全量模块验证：Task 4。

### 2. Placeholder scan

- 没有 `TBD`、`TODO`、`implement later`。
- 每个新增测试 / 代码步骤都给了实际文件路径、代码片段和命令。

### 3. Type consistency

- `GatewayJwkSetFetcher#fetch()` 在 Task 1 和 Task 2 中保持一致，返回 `Mono<JWKSet>`。
- `CachingGatewayJwkProvider#jwkSetForKid(String kid)` 在 Task 2 和 Task 3 中保持一致。
- 配置前缀统一为 `xuan.gateway.security`。

## Notes Before Execution

- `unknown kid` 刷新逻辑不要依赖 `JwkJwtTokenParser` 的异常分类来猜测；先从 JWT header 读 `kid`，再决定是否刷新缓存。
- 过滤器里不要调用 `.block()`；整个 JWKS 拉取与认证链路保持 Reactor 风格。
- 如果集成测试里 `@MockBean CachingGatewayJwkProvider` 与配置装配冲突，优先通过 `@Primary` 测试 Bean 替换 Provider，不要回退到真实网络访问。

Plan complete and saved to `docs/superpowers/plans/2026-07-08-xuan-gateway-jwks-service-discovery.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
