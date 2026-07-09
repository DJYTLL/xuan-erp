package com.xuan.erp.iam.infrastructure.security;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.BearerTokenResolver;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.JwtValidationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IAM Bearer Token 认证过滤器，负责把访问令牌解析进 Spring Security 上下文。
 */
public class IamBearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final BearerTokenResolver bearerTokenResolver = new BearerTokenResolver();
    private final JwkJwtTokenParser jwtTokenParser;

    public IamBearerTokenAuthenticationFilter(JwkJwtTokenParser jwtTokenParser) {
        this.jwtTokenParser = jwtTokenParser;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = bearerTokenResolver.resolve(request.getHeader("Authorization")).orElse(null);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            CurrentUser currentUser = jwtTokenParser.parseAccessToken(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    currentUser,
                    token,
                    authorities(currentUser));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtValidationException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token 无效或已过期");
        }
    }

    private Set<GrantedAuthority> authorities(CurrentUser currentUser) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String permission : currentUser.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        for (String role : currentUser.roles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }
}
