package com.xuan.erp.gateway.infrastructure.security;
import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.GatewayIdentityHeaders;
import com.xuan.erp.common.security.audit.SecurityAuditFailure;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import com.xuan.erp.common.security.jwt.jwk.JwkSetUnavailableException;
import com.xuan.erp.gateway.infrastructure.config.GatewaySecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 网关 Bearer Token 认证过滤器。
 *
 * <p>该过滤器负责在请求进入业务路由前完成三步：
 * 解析 Authorization 头、按 kid 获取 IAM 公钥、基于共享的
 * {@link JwkJwtTokenParser} 完成 access token 验签与 claim 校验。</p>
 */
public class GatewayBearerAuthenticationWebFilter implements WebFilter {

    private final BearerTokenResolver bearerTokenResolver = new BearerTokenResolver();
    private final CachingJwkKeyProvider jwkProvider;
    private final GatewaySecurityProperties properties;
    private final GatewaySecurityAuditReporter securityAuditReporter;
    private final GatewaySecurityErrorResponseWriter securityErrorResponseWriter;

    public GatewayBearerAuthenticationWebFilter(
            CachingJwkKeyProvider jwkProvider,
            GatewaySecurityProperties properties) {
        this(jwkProvider, properties, null, new GatewaySecurityErrorResponseWriter());
    }

    public GatewayBearerAuthenticationWebFilter(
            CachingJwkKeyProvider jwkProvider,
            GatewaySecurityProperties properties,
            GatewaySecurityAuditReporter securityAuditReporter) {
        this(jwkProvider, properties, securityAuditReporter, new GatewaySecurityErrorResponseWriter());
    }

    public GatewayBearerAuthenticationWebFilter(
            CachingJwkKeyProvider jwkProvider,
            GatewaySecurityProperties properties,
            GatewaySecurityAuditReporter securityAuditReporter,
            GatewaySecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwkProvider = jwkProvider;
        this.properties = properties;
        this.securityAuditReporter = securityAuditReporter;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    /**
     * 在 WebFlux 链路中执行 Bearer Token 认证。
     *
     * <p>当请求未携带 Bearer Token 时直接放行，交给后续 Spring Security
     * 规则决定是否拦截；当 token 验签失败或格式非法时，统一返回 401。</p>
     *
     * @param exchange 当前请求交换对象
     * @param chain 后续过滤器链
     * @return 过滤执行结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = bearerTokenResolver.resolve(authorization).orElse(null);
        if (token == null) {
            return chain.filter(exchange);
        }
        return authenticate(token)
                .flatMap(authentication -> filterAuthenticated(exchange, chain, authentication))
                .onErrorResume(JwkSetUnavailableException.class, ex -> serviceUnavailable(exchange, ex))
                .onErrorResume(JwtValidationException.class, ex -> unauthorized(exchange, ex))
                .onErrorResume(IllegalArgumentException.class, ex -> unauthorized(exchange, ex));
    }

    /**
     * 完成单个 Bearer Token 的认证。
     *
     * @param token 待验证的 JWT
     * @return 认证成功后的 Spring Security 身份对象
     */
    private Mono<UsernamePasswordAuthenticationToken> authenticate(String token) {
        String kid = readKid(token);
        return jwkProvider.jwkSetForKid(kid)
                .map(jwkSet -> new JwkJwtTokenParser(jwkSet, properties.getIssuer(), properties.getAudience()))
                .map(parser -> parser.parseAccessToken(token))
                .map(this::authentication);
    }

    /**
     * 从 JWT header 中读取 kid。
     *
     * @param token 待解析的 JWT
     * @return header 中的 key id
     */
    private String readKid(String token) {
        try {
            String kid = SignedJWT.parse(token).getHeader().getKeyID();
            if (kid == null || kid.isBlank()) {
                throw new IllegalArgumentException("JWT header kid must not be blank");
            }
            return kid;
        } catch (ParseException ex) {
            throw new IllegalArgumentException("JWT token format is invalid", ex);
        }
    }

    /**
     * 将共享安全模块的 {@link CurrentUser} 转换为 Spring Security 认证对象。
     *
     * @param currentUser 已通过 JWT 验签得到的当前用户
     * @return 携带角色与权限信息的认证对象
     */
    private UsernamePasswordAuthenticationToken authentication(CurrentUser currentUser) {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        for (String permission : effectivePermissions(currentUser)) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        for (String role : currentUser.roles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return new UsernamePasswordAuthenticationToken(currentUser, "N/A", authorities);
    }

    private Mono<Void> filterAuthenticated(
            ServerWebExchange exchange,
            WebFilterChain chain,
            UsernamePasswordAuthenticationToken authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        Long requestedTenantId;
        try {
            requestedTenantId = requestedTenantId(exchange);
        } catch (IllegalArgumentException ex) {
            return forbiddenTenantContext(exchange, currentUser, null, ex.getMessage());
        }
        if (requestedTenantId != null
                && !requestedTenantId.equals(currentUser.tenantId())
                && !isPlatformSuperAdmin(currentUser)) {
            return forbiddenTenantContext(exchange, currentUser, requestedTenantId, "请求租户与 token 租户不一致");
        }
        ServerWebExchange authenticatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.set(GatewayIdentityHeaders.USER_ID, String.valueOf(currentUser.userId()));
                    headers.set(GatewayIdentityHeaders.TENANT_ID, String.valueOf(currentUser.tenantId()));
                    headers.set(GatewayIdentityHeaders.USERNAME, currentUser.username());
                    headers.set(GatewayIdentityHeaders.ROLES, String.join(",", currentUser.roles()));
                    headers.set(GatewayIdentityHeaders.AUTH_VERSION, String.valueOf(currentUser.authVersion()));
                    headers.set(GatewayIdentityHeaders.PERMISSIONS, String.join(",", effectivePermissions(currentUser)));
                }))
                .build();
        return chain.filter(authenticatedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Set<String> effectivePermissions(CurrentUser currentUser) {
        if (!isPlatformSuperAdmin(currentUser) || currentUser.permissions().contains("*")) {
            return currentUser.permissions();
        }
        Set<String> permissions = new LinkedHashSet<>(currentUser.permissions());
        permissions.add("*");
        return Set.copyOf(permissions);
    }

    private boolean isPlatformSuperAdmin(CurrentUser currentUser) {
        return currentUser.roles().contains("super_admin")
                || "super_admin".equals(currentUser.username())
                || "superadmin".equals(currentUser.username());
    }

    private Long requestedTenantId(ServerWebExchange exchange) {
        String tenantId = exchange.getRequest().getHeaders().getFirst(GatewayIdentityHeaders.TENANT_ID);
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("X-Tenant-Id must be a number", ex);
        }
    }

    /**
     * 将当前请求标记为未认证。
     *
     * @param exchange 当前请求交换对象
     * @return 401 响应完成信号
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, RuntimeException failure) {
        return publish(exchange, SecurityAuditFailure.TOKEN_INVALID, null, null, failure.getMessage())
                .then(securityErrorResponseWriter.write(exchange, SecurityAuditFailure.TOKEN_INVALID));
    }

    /**
     * 将 IAM JWKS Endpoint 不可用转换为认证基础设施不可用响应。
     *
     * @param exchange 当前请求交换对象
     * @return 503 响应完成信号
     */
    private Mono<Void> serviceUnavailable(ServerWebExchange exchange, RuntimeException failure) {
        return publish(exchange, SecurityAuditFailure.JWKS_UNAVAILABLE, null, null, failure.getMessage())
                .then(securityErrorResponseWriter.write(exchange, SecurityAuditFailure.JWKS_UNAVAILABLE));
    }

    private Mono<Void> forbiddenTenantContext(
            ServerWebExchange exchange,
            CurrentUser currentUser,
            Long requestedTenantId,
            String message) {
        return publish(
                exchange,
                SecurityAuditFailure.TENANT_CONTEXT_INVALID,
                currentUser,
                requestedTenantId,
                message)
                .then(securityErrorResponseWriter.write(exchange, SecurityAuditFailure.TENANT_CONTEXT_INVALID));
    }

    private Mono<Void> publish(
            ServerWebExchange exchange,
            SecurityAuditFailure failure,
            CurrentUser currentUser,
            Long requestedTenantId,
            String message) {
        if (securityAuditReporter == null) {
            return Mono.empty();
        }
        return securityAuditReporter.publish(exchange, failure, currentUser, requestedTenantId, message);
    }

}
