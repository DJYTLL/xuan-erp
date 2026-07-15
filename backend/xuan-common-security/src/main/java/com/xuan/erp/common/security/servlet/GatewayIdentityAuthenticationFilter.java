package com.xuan.erp.common.security.servlet;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.GatewayIdentityHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Restores Spring Security authentication from identity headers set by xuan-gateway.
 */
public class GatewayIdentityAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuthentication == null || currentAuthentication instanceof AnonymousAuthenticationToken) {
            CurrentUser currentUser = currentUser(request);
            if (currentUser != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication(currentUser, bearerToken(request)));
            }
        }
        filterChain.doFilter(request, response);
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        String userId = request.getHeader(GatewayIdentityHeaders.USER_ID);
        String tenantId = request.getHeader(GatewayIdentityHeaders.TENANT_ID);
        String username = request.getHeader(GatewayIdentityHeaders.USERNAME);
        if (!hasText(userId) || !hasText(tenantId) || !hasText(username)) {
            return null;
        }
        return new CurrentUser(
                parseLong(userId),
                parseLong(tenantId),
                username.trim(),
                split(request.getHeader(GatewayIdentityHeaders.ROLES)),
                parseLongOrDefault(request.getHeader(GatewayIdentityHeaders.AUTH_VERSION), 0L),
                split(request.getHeader(GatewayIdentityHeaders.PERMISSIONS)));
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

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!hasText(authorization) || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return hasText(token) ? token : null;
    }

    private Set<String> split(String value) {
        if (!hasText(value)) {
            return Set.of();
        }
        Set<String> items = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .forEach(items::add);
        return Set.copyOf(items);
    }

    private Long parseLong(String value) {
        return Long.valueOf(value.trim());
    }

    private Long parseLongOrDefault(String value, Long defaultValue) {
        return hasText(value) ? parseLong(value) : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
