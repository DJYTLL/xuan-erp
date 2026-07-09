package com.xuan.erp.gateway;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import com.xuan.erp.common.security.jwt.TokenType;
import com.xuan.erp.gateway.infrastructure.security.CachingGatewayJwkProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.test.web.server.LocalServerPort;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 网关安全链路集成测试。
 *
 * <p>该测试直接启动 `xuan-gateway` 的 Spring Boot 上下文，验证
 * Bearer Token 认证过滤器、Spring Security 规则以及 actuator
 * 放行策略能否按预期协同工作。</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "xuan.gateway.security.enabled=true",
                "xuan.gateway.security.issuer=xuan-iam",
                "xuan.gateway.security.audience=xuan-gateway",
                "xuan.gateway.security.iam-service-name=xuan-iam"
        })
@Import(GatewaySecurityIntegrationTest.TestEndpoints.class)
class GatewaySecurityIntegrationTest {

    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @MockitoBean
    private CachingGatewayJwkProvider jwkProvider;

    @MockitoBean
    private org.springframework.cloud.client.discovery.DiscoveryClient discoveryClient;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    private RSAKey rsaKey;

    /**
     * 为每个用例准备独立的测试密钥和 HTTP 客户端。
     */
    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048)
                .keyID("kid-1")
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    // 测试受保护路由在未携带 Bearer Token 时返回 401。
    @Test
    void returnsUnauthorizedWhenTokenMissing() {
        webTestClient.get()
                .uri("/api/test/secured")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // 测试合法的 IAM access token 能通过网关认证并访问受保护路由。
    @Test
    void allowsRequestWithValidIamAccessToken() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.parse("2030-01-01T00:05:00Z")))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("secured");
    }

    // 测试 kid 刷新后仍然不存在时，网关统一返回 401。
    @Test
    void returnsUnauthorizedWhenKidStillMissingAfterRefresh() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-missing")).thenReturn(Mono.error(
                new JwtValidationException(JwtValidationException.Reason.SIGNATURE_INVALID, "missing kid")));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-missing", Instant.parse("2030-01-01T00:05:00Z")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // 测试过期 token 会在网关入口被拒绝。
    @Test
    void returnsUnauthorizedWhenTokenExpired() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.parse("2020-01-01T00:05:00Z")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // 测试健康检查端点保持匿名可访问。
    @Test
    void keepsActuatorEndpointsAnonymous() {
        webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
        webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
    }

    // 测试 IAM 登录与 JWKS 入口允许匿名访问，避免登录请求在网关层被 401 拦截。
    @Test
    void keepsIamLoginAndJwksEndpointsAnonymous() {
        webTestClient.post()
                .uri("/api/iam/auth/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("login-entry");

        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("jwks-entry");
    }

    // 测试 Gateway 明确配置 IAM 路由，保证匿名登录请求放行后可以继续转发到 xuan-iam。
    @Test
    void routesIamApiAndJwksToIamService() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertNotNull(routes);

        RouteDefinition iamRoute = routes.stream()
                .filter(route -> "xuan-iam".equals(route.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 xuan-iam Gateway 路由"));

        assertEquals("lb://xuan-iam", iamRoute.getUri().toString());
        String pathPredicateArgs = iamRoute.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .flatMap(predicate -> predicate.getArgs().values().stream())
                .collect(Collectors.joining(","));
        assertTrue(pathPredicateArgs.contains("/api/iam/**"));
        assertTrue(pathPredicateArgs.contains("/.well-known/jwks.json"));
    }

    /**
     * 生成一个符合 IAM 约定的测试 access token。
     *
     * @param signingKey 用于签名的 RSA 密钥
     * @param kid JWT header 中携带的 key id
     * @param expiresAt token 过期时间
     * @return 已签名的 JWT 字符串
     */
    private static String signedToken(RSAKey signingKey, String kid, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("xuan-iam")
                .audience("xuan-gateway")
                .issueTime(Date.from(expiresAt.minusSeconds(300)))
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
        signedJWT.sign(new RSASSASigner(signingKey));
        return signedJWT.serialize();
    }

    /**
     * 测试专用接口，用于验证受保护路由与匿名 actuator 端点的行为。
     */
    @RestController
    static class TestEndpoints {

        @GetMapping("/api/test/secured")
        String secured() {
            return "secured";
        }

        @PostMapping("/api/iam/auth/login")
        String loginEntry() {
            return "login-entry";
        }

        @GetMapping("/.well-known/jwks.json")
        String jwksEntry() {
            return "jwks-entry";
        }
    }
}
