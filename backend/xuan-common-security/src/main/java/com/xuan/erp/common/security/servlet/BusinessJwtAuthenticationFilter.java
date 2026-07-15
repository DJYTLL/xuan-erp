package com.xuan.erp.common.security.servlet;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import com.xuan.erp.common.security.jwt.jwk.JwkSetUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 业务服务侧 Bearer Token 认证过滤器。
 *
 * <p>该过滤器用于普通 Servlet 业务服务直收 {@code Authorization: Bearer xxx} 的场景。它只负责
 * 调用共享 JWT Parser 完成验签并写入 {@link SecurityContextHolder}；权限事实源和权限快照仍由
 * IAM 及后续授权组件负责。</p>
 */
public class BusinessJwtAuthenticationFilter extends OncePerRequestFilter {

    private final BearerTokenResolver bearerTokenResolver;
    private final JwkJwtTokenParser jwtTokenParser;

    public BusinessJwtAuthenticationFilter(
            BearerTokenResolver bearerTokenResolver,
            JwkJwtTokenParser jwtTokenParser) {
        this.bearerTokenResolver = Objects.requireNonNull(bearerTokenResolver, "bearerTokenResolver must not be null");
        this.jwtTokenParser = Objects.requireNonNull(jwtTokenParser, "jwtTokenParser must not be null");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = bearerTokenResolver.resolve(request.getHeader(HttpHeaders.AUTHORIZATION)).orElse(null);
        if (token == null || hasRealAuthentication()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            CurrentUser currentUser = jwtTokenParser.parseAccessToken(token);
            SecurityContextHolder.getContext().setAuthentication(authentication(currentUser, token));
            filterChain.doFilter(request, response);
        } catch (JwkSetUnavailableException ex) {
            complete(response, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JwtValidationException | IllegalArgumentException ex) {
            complete(response, HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean hasRealAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private UsernamePasswordAuthenticationToken authentication(CurrentUser currentUser, String token) {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        for (String permission : currentUser.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        for (String role : currentUser.roles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return new UsernamePasswordAuthenticationToken(currentUser, token, authorities);
    }

    private void complete(HttpServletResponse response, HttpStatus status) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status.value());
        response.flushBuffer();
    }
}
