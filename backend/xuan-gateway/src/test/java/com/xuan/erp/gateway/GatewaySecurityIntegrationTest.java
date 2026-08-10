package com.xuan.erp.gateway;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
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
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
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
                "xuan.gateway.security.permission-rules[0].authority=admin:access",
                "xuan.gateway.security.permission-rules[1].paths[0]=/api/tenants/**",
                "xuan.gateway.security.permission-rules[1].authority=tenant:view",
                "xuan.gateway.security.permission-rules[2].paths[0]=/api/tenants/column-permission-options",
                "xuan.gateway.security.permission-rules[2].authorities[0]=tenant:view",
                "xuan.gateway.security.permission-rules[2].authorities[1]=iam-column-permission:view",
                "xuan.gateway.security.permission-rules[2].authorities[2]=iam-role-column-permission:view"
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

    @Autowired
    private Environment environment;

    @Autowired
    private BlockRequestHandler sentinelGatewayBlockRequestHandler;

    @Autowired
    private ServerCodecConfigurer serverCodecConfigurer;

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

    // 测试未显式传入请求 ID 时，安全审计会使用 Gateway 生成的 TraceId 作为可追踪请求 ID。
    @Test
    void usesGeneratedTraceIdAsSecurityAuditRequestIdWhenRequestIdMissing() {
        webTestClient.get()
                .uri("/api/test/secured")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches("X-Trace-Id", "xuan-[A-Za-z0-9]{32}");

        ArgumentCaptor<AuditWriteEvent> eventCaptor = ArgumentCaptor.forClass(AuditWriteEvent.class);
        verify(securityAuditPublisher).publish(eventCaptor.capture());
        AuditWriteEvent event = eventCaptor.getValue();
        assertNotNull(event.requestId());
        assertTrue(event.requestId().startsWith("xuan-"));
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

    // 测试外部伪造的身份 Header 即使打到匿名放行路径，也不会被网关转发给下游。
    @Test
    void stripsSpoofedIdentityHeadersOnPublicPaths() {
        webTestClient.get()
                .uri("/v3/api-docs/header-probe")
                .header("X-User-Id", "999")
                .header("X-Tenant-Id", "888")
                .header("X-Username", "fake-admin")
                .header("X-Roles", "super_admin")
                .header("X-Auth-Version", "999")
                .header("X-Permissions", "*")
                .header("X-Trace-Id", "client-trace-001")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("identity=<none>|permissions=<none>|trace=client-trace-001");
    }

    // 测试外部没有 TraceId 时，由 Gateway 生成并向下游透传。
    @Test
    void generatesTraceIdWhenMissing() {
        webTestClient.get()
                .uri("/v3/api-docs/header-probe")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.startsWith("identity=<none>|permissions=<none>|trace=xuan-"));
                    assertTrue(body.length() > "identity=<none>|permissions=<none>|trace=xuan-".length());
                });
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

    // 测试业务租户里的 super_admin 用户名和角色不能冒充平台超管跨租户访问。
    @Test
    void rejectsTenantScopedSuperAdminAcrossTenantContext() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/test/secured")
                .header("X-Request-Id", "req-tenant-scoped-super-admin")
                .header("X-Tenant-Id", "2002")
                .header("Authorization", "Bearer " + signedToken(
                        rsaKey,
                        "kid-1",
                        Instant.parse("2030-01-01T00:05:00Z"),
                        1001L,
                        "super_admin",
                        List.of("super_admin"),
                        List.of("*")))
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
        assertEquals("req-tenant-scoped-super-admin", event.requestId());
    }

    // 测试角色列权限页面可通过专用权限读取租户下拉选项，不被租户管理 tenant:view 粗粒度规则误拦。
    @Test
    void allowsTenantColumnPermissionOptionsWithRoleColumnPermission() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/tenants/column-permission-options?pageNum=1&pageSize=200")
                .header("Authorization", "Bearer " + signedToken(
                        rsaKey,
                        "kid-1",
                        Instant.parse("2030-01-01T00:05:00Z"),
                        List.of("iam-role-column-permission:view")))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("tenant-column-options");
    }

    // 测试租户下拉选项接口在网关层仍需列权限或租户查看权限，不能退化成任意登录用户可访问。
    @Test
    void rejectsTenantColumnPermissionOptionsWithoutExpectedPermission() throws Exception {
        when(jwkProvider.jwkSetForKid("kid-1")).thenReturn(Mono.just(new JWKSet(rsaKey.toPublicJWK())));

        webTestClient.get()
                .uri("/api/tenants/column-permission-options?pageNum=1&pageSize=200")
                .header("Authorization", "Bearer " + signedToken(
                        rsaKey,
                        "kid-1",
                        Instant.parse("2030-01-01T00:05:00Z"),
                        List.of("iam:view")))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SECURITY_PERMISSION_DENIED")
                .jsonPath("$.message").isEqualTo("没有访问权限");
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

    // 测试 Gateway 已接入 Sentinel 网关规则数据源，规则仍由 Nacos 独立 dataId 管理。
    @Test
    void configuresSentinelGatewayRuleDataSources() {
        assertEquals("xuan-gateway-sentinel-gw-flow-rules.json",
                environment.getProperty("spring.cloud.sentinel.datasource.gw-flow.nacos.data-id"));
        assertEquals("gw-flow",
                environment.getProperty("spring.cloud.sentinel.datasource.gw-flow.nacos.rule-type"));
        assertEquals("xuan-gateway-sentinel-gw-api-group-rules.json",
                environment.getProperty("spring.cloud.sentinel.datasource.gw-api-group.nacos.data-id"));
        assertEquals("gw-api-group",
                environment.getProperty("spring.cloud.sentinel.datasource.gw-api-group.nacos.rule-type"));
    }

    // 测试 Sentinel Gateway block 回调返回统一 429 JSON，而不是 Sentinel 默认结构或空响应。
    @Test
    void returnsUnifiedJsonWhenSentinelGatewayBlocksRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test/secured").build());
        ServerResponse response = sentinelGatewayBlockRequestHandler
                .handleRequest(exchange, new RuntimeException("blocked by test"))
                .block();

        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode());

        response.writeTo(exchange, new ServerResponse.Context() {
            @Override
            public List<org.springframework.http.codec.HttpMessageWriter<?>> messageWriters() {
                return serverCodecConfigurer.getWriters();
            }

            @Override
            public List<ViewResolver> viewResolvers() {
                return List.copyOf(applicationContext.getBeansOfType(ViewResolver.class).values());
            }
        }).block();

        assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
        assertEquals(
                "{\"code\":\"GATEWAY_RATE_LIMITED\",\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}",
                exchange.getResponse().getBodyAsString().block());
    }

    // 测试 Sentinel 运行时回调管理器实际注册了 Gateway 的统一 429 响应处理器。
    @Test
    void registersUnifiedSentinelGatewayBlockHandlerWithCallbackManager() {
        BlockRequestHandler registeredHandler = GatewayCallbackManager.getBlockHandler();
        assertEquals(sentinelGatewayBlockRequestHandler.getClass(), registeredHandler.getClass());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test/secured").build());
        ServerResponse response = registeredHandler
                .handleRequest(exchange, new RuntimeException("blocked by callback manager"))
                .block();

        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode());
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
        assertTrue(pathPredicateArgs.contains("/api/tenant-configs"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-configs/**"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-domains"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-domains/**"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-contacts"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-contacts/**"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-resources"));
        assertTrue(pathPredicateArgs.contains("/api/tenant-resources/**"));
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

        @GetMapping("/v3/api-docs/header-probe")
        String publicHeaderProbe(
                @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String userId,
                @org.springframework.web.bind.annotation.RequestHeader(value = "X-Permissions", required = false) String permissions,
                @org.springframework.web.bind.annotation.RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
            return "identity=" + valueOrNone(userId)
                    + "|permissions=" + valueOrNone(permissions)
                    + "|trace=" + valueOrNone(traceId);
        }

        @GetMapping("/api/tenants/column-permission-options")
        String tenantColumnPermissionOptions() {
            return "tenant-column-options";
        }

        private String valueOrNone(String value) {
            return value == null || value.isBlank() ? "<none>" : value;
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
