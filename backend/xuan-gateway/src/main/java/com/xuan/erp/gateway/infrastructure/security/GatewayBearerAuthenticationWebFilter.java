package com.xuan.erp.gateway.infrastructure.security;

import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import com.xuan.erp.gateway.infrastructure.config.GatewaySecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private final CachingGatewayJwkProvider jwkProvider;
    private final GatewaySecurityProperties properties;

    public GatewayBearerAuthenticationWebFilter(
            CachingGatewayJwkProvider jwkProvider,
            GatewaySecurityProperties properties) {
        this.jwkProvider = jwkProvider;
        this.properties = properties;
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
                .flatMap(authentication -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .onErrorResume(JwtValidationException.class, ex -> unauthorized(exchange))
                .onErrorResume(IllegalArgumentException.class, ex -> unauthorized(exchange));
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
        for (String permission : currentUser.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        for (String role : currentUser.roles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return new UsernamePasswordAuthenticationToken(currentUser, "N/A", authorities);
    }

    /**
     * 将当前请求标记为未认证。
     *
     * @param exchange 当前请求交换对象
     * @return 401 响应完成信号
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
