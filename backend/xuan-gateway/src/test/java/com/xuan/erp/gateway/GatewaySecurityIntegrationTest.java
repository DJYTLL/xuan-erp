package com.xuan.erp.gateway;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.audit.AuditWriteEvent;
import com.xuan.erp.common.audit.AuditWriteSubmission;
import com.xuan.erp.common.audit.SafeAuditWritePublisher;
import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import com.xuan.erp.common.security.jwt.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import org.mockito.ArgumentCaptor;

/**
 * 网关安全链路集成测试。
 *
 * <p>该测试直接启动 `xuan-gateway` 的 Spring Boot 上下文，验证
 * Bearer Token 认证过滤器、Spring Security 规则以及 actuator
 * 放行策略能否按预期协同工作。</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "xuan.gateway.security.enabled=true",
                "xuan.gateway.security.issuer=xuan-iam",
                "xuan.gateway.security.audience=xuan-gateway",
                "xuan.gateway.security.iam-service-name=xuan-iam",
                "xuan.gateway.security.permission-rules[0].paths[0]=/api/test/admin",
                "xuan.gateway.security.permission-rules[0].authority=admin:access"
        })
@Import(GatewaySecurityIntegrationTest.TestEndpoints.class)
class GatewaySecurityIntegrationTest {

    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private CachingJwkKeyProvider jwkProvider;

    @MockitoBean
    private org.springframework.cloud.client.discovery.DiscoveryClient discoveryClient;

    @MockitoBean
    private SafeAuditWritePublisher securityAuditPublisher;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private CorsConfigurationSource gatewayCorsConfigurationSource;

    @Autowired
    private com.xuan.erp.gateway.infrastructure.config.GatewaySecurityProperties gatewaySecurityProperties;

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
        when(securityAuditPublisher.publish(any())).thenReturn(AuditWriteSubmission.accepted());
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    // 测试受保护路由在未携带 Bearer Token 时返回 401。
    @Test
    void returnsUnauthorizedWhenTokenMissing() {
        webTestClient.get()
                .uri("/api/test/secured")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SECURITY_AUTHENTICATION_MISSING")
                .jsonPath("$.message").isEqualTo("未登录或登录已过期");
    }

    // 测试未登录请求除了返回 401，还会写入网关侧安全审计。
    @Test
    void writesSecurityAuditWhenTokenMissing() {
        webTestClient.get()
                .uri("/api/test/secured")
                .header("X-Request-Id", "req-missing-token")
                .exchange()
                .expectStatus().isUnauthorized();

        ArgumentCaptor<AuditWriteEvent> eventCaptor = ArgumentCaptor.forClass(AuditWriteEvent.class);
        verify(securityAuditPublisher).publish(eventCaptor.capture());
        AuditWriteEvent event = eventCaptor.getValue();
        assertEquals("security:authentication:missing", event.action());
        assertEquals("GatewaySecurity", event.entityType());
        assertEquals(0L, event.tenantId());
        assertEquals("/api/test/secured", event.path());
        assertEquals(401, event.httpStatus());
        assertEquals("SECURITY_AUTHENTICATION_MISSING", event.errorCode());
        assertEquals("req-missing-token", event.requestId());
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

    // 测试网关认证成功后会把最小用户上下文转发给下游 Servlet 服务。
    @Test
    void forwardsGatewayIdentityHeadersAfterAuthentication() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/identity-headers")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.parse("2030-01-01T00:05:00Z")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("1|1001|gateway-user|iam:view");
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
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SECURITY_TOKEN_INVALID")
                .jsonPath("$.message").isEqualTo("访问令牌无效或已过期");
    }

    // 测试过期 token 会在网关入口被拒绝。
    @Test
    void returnsUnauthorizedWhenTokenExpired() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.parse("2020-01-01T00:05:00Z")))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SECURITY_TOKEN_INVALID")
                .jsonPath("$.message").isEqualTo("访问令牌无效或已过期");
    }

    // 测试 token 过期等 token 异常会写入网关侧安全审计。
    @Test
    void writesSecurityAuditWhenTokenInvalid() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("X-Request-Id", "req-token-invalid")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.parse("2020-01-01T00:05:00Z")))
                .exchange()
                .expectStatus().isUnauthorized();

        ArgumentCaptor<AuditWriteEvent> eventCaptor = ArgumentCaptor.forClass(AuditWriteEvent.class);
        verify(securityAuditPublisher).publish(eventCaptor.capture());
        AuditWriteEvent event = eventCaptor.getValue();
        assertEquals("security:token:invalid", event.action());
        assertEquals(0L, event.tenantId());
        assertEquals(401, event.httpStatus());
        assertEquals("SECURITY_TOKEN_INVALID", event.errorCode());
        assertEquals("req-token-invalid", event.requestId());
    }

    // 测试网关侧权限规则拒绝访问时，会进入真实 AccessDeniedHandler 并记录权限拒绝审计。
    @Test
    void writesSecurityAuditWhenGatewayPermissionDenied() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/admin")
                .header("X-Request-Id", "req-permission-denied")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.parse("2030-01-01T00:05:00Z")))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SECURITY_PERMISSION_DENIED")
                .jsonPath("$.message").isEqualTo("没有访问权限");

        ArgumentCaptor<AuditWriteEvent> eventCaptor = ArgumentCaptor.forClass(AuditWriteEvent.class);
        verify(securityAuditPublisher).publish(eventCaptor.capture());
        AuditWriteEvent event = eventCaptor.getValue();
        assertEquals("security:permission:denied", event.action());
        assertEquals(1001L, event.tenantId());
        assertEquals(1L, event.actorUserId());
        assertEquals("gateway-user", event.actorUsername());
        assertEquals(1001L, event.authTenantId());
        assertEquals(403, event.httpStatus());
        assertEquals("SECURITY_PERMISSION_DENIED", event.errorCode());
        assertEquals("req-permission-denied", event.requestId());
    }

    // 测试 superadmin 的通配权限能穿过网关配置化权限规则，避免新增权限码后旧 token 被网关误拦。
    @Test
    void allowsGatewayPermissionRuleWhenTokenHasWildcardPermission() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/admin")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.parse("2030-01-01T00:05:00Z"), List.of("*")))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("admin");
    }

    // 测试 super_admin 用户名不依赖权限快照里的 *，也能穿过网关配置化权限规则。
    @Test
    void allowsGatewayPermissionRuleForPlatformSuperAdminUsernameWithoutWildcardPermission() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/admin")
                .header("Authorization", "Bearer " + signedToken(
                        rsaKey,
                        "kid-1",
                        Instant.parse("2030-01-01T00:05:00Z"),
                        0L,
                        "super_admin",
                        List.of(),
                        List.of()))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("admin");
    }

    // 测试平台 super_admin 用户名即使角色快照缺失，也能跨租户操作目标租户资源。
    @Test
    void allowsPlatformSuperAdminUsernameAcrossTenantContextWithoutRoleSnapshot() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("X-Tenant-Id", "2002")
                .header("Authorization", "Bearer " + signedToken(
                        rsaKey,
                        "kid-1",
                        Instant.parse("2030-01-01T00:05:00Z"),
                        0L,
                        "super_admin",
                        List.of(),
                        List.of()))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("secured");
    }

    // 测试请求租户头与 token 租户不一致时，网关返回 403 并记录跨租户安全审计。
    @Test
    void writesSecurityAuditWhenTenantContextInvalid() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("X-Request-Id", "req-tenant-mismatch")
                .header("X-Tenant-Id", "2002")
                .header("Authorization", "Bearer " + signedToken(rsaKey, "kid-1", Instant.parse("2030-01-01T00:05:00Z")))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SECURITY_TENANT_CONTEXT_INVALID")
                .jsonPath("$.message").isEqualTo("租户上下文无效");

        ArgumentCaptor<AuditWriteEvent> eventCaptor = ArgumentCaptor.forClass(AuditWriteEvent.class);
        verify(securityAuditPublisher).publish(eventCaptor.capture());
        AuditWriteEvent event = eventCaptor.getValue();
        assertEquals("security:tenant-context:invalid", event.action());
        assertEquals(2002L, event.tenantId());
        assertEquals(1001L, event.authTenantId());
        assertTrue(event.crossTenant());
        assertEquals(403, event.httpStatus());
        assertEquals("SECURITY_TENANT_CONTEXT_INVALID", event.errorCode());
    }

    // 测试健康检查端点保持匿名可访问。
    @Test
    void keepsActuatorEndpointsAnonymous() {
        webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
        webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
    }

    // 测试 IAM 登录、刷新与 JWKS 入口允许匿名访问，避免认证入口请求在网关层被 401 拦截。
    @Test
    void keepsIamAuthEntryAndJwksEndpointsAnonymous() {
        webTestClient.post()
                .uri("/api/iam/auth/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("login-entry");

        webTestClient.post()
                .uri("/api/iam/auth/refresh")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("refresh-entry");

        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("jwks-entry");
    }

    // 测试 Gateway 聚合 Swagger UI 和 OpenAPI 文档入口允许匿名访问，避免文档页在网关层被 401 拦截。
    @Test
    void keepsSwaggerAndOpenApiAggregationEndpointsAnonymous() {
        webTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("swagger-ui-entry");

        webTestClient.get()
                .uri("/swagger-ui/index.css")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("swagger-ui-static-entry");

        webTestClient.get()
                .uri("/webjars/swagger-ui/swagger-ui.css")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("swagger-webjar-entry");

        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("gateway-openapi-entry");

        webTestClient.get()
                .uri("/v3/api-docs/swagger-config")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("swagger-config-entry");

        webTestClient.get()
                .uri("/v3/api-docs/iam")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("iam-openapi-entry");

        webTestClient.get()
                .uri("/v3/api-docs/tenant")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("tenant-openapi-entry");
    }

    // 测试匿名放行路径来自 Gateway YAML 配置，避免把安全策略散落硬编码在 Security 配置类里。
    @Test
    void bindsPublicPathsFromGatewayYamlConfiguration() {
        assertTrue(gatewaySecurityProperties.getPublicPaths().contains("/swagger-ui.html"));
        assertTrue(gatewaySecurityProperties.getPublicPaths().contains("/swagger-ui/**"));
        assertTrue(gatewaySecurityProperties.getPublicPaths().contains("/v3/api-docs"));
        assertTrue(gatewaySecurityProperties.getPublicPaths().contains("/v3/api-docs/**"));
        assertTrue(gatewaySecurityProperties.getPublicPaths().contains("/api/iam/auth/refresh"));
    }

    // 测试前端登录预检请求所需的 CORS 配置已注册到 Gateway Security 链路。
    @Test
    void configuresFrontendCorsPreflightForIamLogin() {
        CorsConfiguration configuration = corsConfigurationFor(
                MockServerHttpRequest.options("/api/iam/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,authorization")
                        .build());

        assertNotNull(configuration);
        assertTrue(configuration.getAllowedOrigins().contains("http://127.0.0.1:5173"));
        assertTrue(configuration.getAllowedMethods().contains(HttpMethod.POST.name()));
        assertTrue(configuration.getAllowedHeaders().contains("*"));
    }

    // 测试真实跨域访问需要暴露的响应头已配置，避免浏览器丢弃认证成功后的响应头。
    @Test
    void configuresCorsForAuthenticatedFrontendRequest() {
        CorsConfiguration configuration = corsConfigurationFor(
                MockServerHttpRequest.get("/api/test/secured")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .build());

        assertNotNull(configuration);
        assertTrue(configuration.getAllowedOrigins().contains("http://127.0.0.1:5173"));
        assertTrue(configuration.getExposedHeaders().contains(HttpHeaders.AUTHORIZATION));
        assertTrue(configuration.getExposedHeaders().contains(HttpHeaders.CONTENT_DISPOSITION));
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

    // 测试 Gateway 聚合 OpenAPI 文档路由，聚合路径必须能转发到对应服务的 /v3/api-docs。
    @Test
    void routesOpenApiAggregationToBackendServices() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertNotNull(routes);

        RouteDefinition iamRoute = routes.stream()
                .filter(route -> "xuan-iam".equals(route.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 xuan-iam Gateway 路由"));
        String iamPathPredicateArgs = pathPredicateArgs(iamRoute);
        assertTrue(iamPathPredicateArgs.contains("/v3/api-docs/iam"));
        assertTrue(rewritePathFilterArgs(iamRoute).contains("/v3/api-docs/iam"));
        assertTrue(rewritePathFilterArgs(iamRoute).contains("/v3/api-docs"));

        RouteDefinition tenantRoute = routes.stream()
                .filter(route -> "xuan-tenant".equals(route.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 xuan-tenant Gateway 路由"));
        String tenantPathPredicateArgs = pathPredicateArgs(tenantRoute);
        assertTrue(tenantPathPredicateArgs.contains("/v3/api-docs/tenant"));
        assertTrue(rewritePathFilterArgs(tenantRoute).contains("/v3/api-docs/tenant"));
        assertTrue(rewritePathFilterArgs(tenantRoute).contains("/v3/api-docs"));
    }

    // 测试 Gateway 明确配置 Audit 路由，保证系统设置下的监控查询能转发到 xuan-audit。
    @Test
    void routesAuditApiToAuditService() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertNotNull(routes);

        RouteDefinition auditRoute = routes.stream()
                .filter(route -> "xuan-audit".equals(route.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 xuan-audit Gateway 路由"));

        assertEquals("lb://xuan-audit", auditRoute.getUri().toString());
        String pathPredicateArgs = auditRoute.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .flatMap(predicate -> predicate.getArgs().values().stream())
                .collect(Collectors.joining(","));
        assertTrue(pathPredicateArgs.contains("/api/audit/**"));
    }

    // 测试 Gateway 明确配置 Tenant 路由，保证租户管理页面请求能转发到 xuan-tenant。
    @Test
    void routesTenantApiToTenantService() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertNotNull(routes);

        RouteDefinition tenantRoute = routes.stream()
                .filter(route -> "xuan-tenant".equals(route.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 xuan-tenant Gateway 路由"));

        assertEquals("lb://xuan-tenant", tenantRoute.getUri().toString());
        List<String> pathPredicateArgs = tenantRoute.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .flatMap(predicate -> predicate.getArgs().values().stream())
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .toList();
        assertTrue(pathPredicateArgs.contains("/api/tenants"));
        assertTrue(pathPredicateArgs.contains("/api/tenants/**"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-plans"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-plans/**"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-plan-assignments"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-plan-assignments/**"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-provision-tasks/**"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-outbox-events/**"));
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
        return signedToken(signingKey, kid, expiresAt, List.of("iam:view"));
    }

    private static String signedToken(RSAKey signingKey, String kid, Instant expiresAt, List<String> permissions) throws Exception {
        return signedToken(signingKey, kid, expiresAt, 1001L, "gateway-user", List.of("tenant_admin"), permissions);
    }

    private static String signedToken(
            RSAKey signingKey,
            String kid,
            Instant expiresAt,
            Long tenantId,
            String username,
            List<String> roles,
            List<String> permissions) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("xuan-iam")
                .audience("xuan-gateway")
                .issueTime(Date.from(expiresAt.minusSeconds(300)))
                .expirationTime(Date.from(expiresAt))
                .claim("tokenType", TokenType.ACCESS.name())
                .claim("userId", 1L)
                .claim("tenantId", tenantId)
                .claim("username", username)
                .claim("roles", roles)
                .claim("authVersion", 7L)
                .claim("permissions", permissions)
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), claims);
        signedJWT.sign(new RSASSASigner(signingKey));
        return signedJWT.serialize();
    }

    private String pathPredicateArgs(RouteDefinition route) {
        return route.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .flatMap(predicate -> predicate.getArgs().values().stream())
                .collect(Collectors.joining(","));
    }

    private String rewritePathFilterArgs(RouteDefinition route) {
        return route.getFilters().stream()
                .filter(filter -> "RewritePath".equals(filter.getName()))
                .flatMap(filter -> filter.getArgs().values().stream())
                .collect(Collectors.joining(","));
    }

    private CorsConfiguration corsConfigurationFor(MockServerHttpRequest request) {
        return gatewayCorsConfigurationSource.getCorsConfiguration(MockServerWebExchange.from(request));
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

        @GetMapping("/api/test/admin")
        String admin() {
            return "admin";
        }

        @GetMapping("/api/test/identity-headers")
        String identityHeaders(
                @org.springframework.web.bind.annotation.RequestHeader("X-User-Id") String userId,
                @org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Id") String tenantId,
                @org.springframework.web.bind.annotation.RequestHeader("X-Username") String username,
                @org.springframework.web.bind.annotation.RequestHeader("X-Permissions") String permissions) {
            return String.join("|", userId, tenantId, username, permissions);
        }

        @PostMapping("/api/iam/auth/login")
        String loginEntry() {
            return "login-entry";
        }

        @PostMapping("/api/iam/auth/refresh")
        String refreshEntry() {
            return "refresh-entry";
        }

        @GetMapping("/swagger-ui.html")
        String swaggerUiEntry() {
            return "swagger-ui-entry";
        }

        @GetMapping("/swagger-ui/index.css")
        String swaggerUiStaticEntry() {
            return "swagger-ui-static-entry";
        }

        @GetMapping("/webjars/swagger-ui/swagger-ui.css")
        String swaggerWebjarEntry() {
            return "swagger-webjar-entry";
        }

        @GetMapping("/v3/api-docs")
        String gatewayOpenApiEntry() {
            return "gateway-openapi-entry";
        }

        @GetMapping("/v3/api-docs/swagger-config")
        String swaggerConfigEntry() {
            return "swagger-config-entry";
        }

        @GetMapping("/v3/api-docs/iam")
        String iamOpenApiEntry() {
            return "iam-openapi-entry";
        }

        @GetMapping("/v3/api-docs/tenant")
        String tenantOpenApiEntry() {
            return "tenant-openapi-entry";
        }

        @GetMapping("/.well-known/jwks.json")
        String jwksEntry() {
            return "jwks-entry";
        }
    }
}
