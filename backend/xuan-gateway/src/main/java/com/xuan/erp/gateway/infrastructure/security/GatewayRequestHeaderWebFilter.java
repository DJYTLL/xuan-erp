package com.xuan.erp.gateway.infrastructure.security;

import com.xuan.erp.common.security.GatewayIdentityHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 统一治理进入 Gateway 的请求头。
 *
 * <p>身份 Header 只允许 Gateway 在认证成功后写入，外部传入的同名 Header
 * 会在进入下游前被清理；TraceId 则由 Gateway 校验、生成并继续透传。</p>
 */
public class GatewayRequestHeaderWebFilter implements WebFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUESTED_TENANT_ID_ATTRIBUTE =
            GatewayRequestHeaderWebFilter.class.getName() + ".requestedTenantId";

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final List<String> IDENTITY_HEADERS = List.of(
            GatewayIdentityHeaders.USER_ID,
            GatewayIdentityHeaders.TENANT_ID,
            GatewayIdentityHeaders.USERNAME,
            GatewayIdentityHeaders.ROLES,
            GatewayIdentityHeaders.AUTH_VERSION,
            GatewayIdentityHeaders.PERMISSIONS);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestedTenantId = exchange.getRequest().getHeaders().getFirst(GatewayIdentityHeaders.TENANT_ID);
        if (hasText(requestedTenantId)) {
            exchange.getAttributes().put(REQUESTED_TENANT_ID_ATTRIBUTE, requestedTenantId.trim());
        }
        String traceId = traceId(exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER));
        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    IDENTITY_HEADERS.forEach(headers::remove);
                    headers.set(TRACE_ID_HEADER, traceId);
                }))
                .build();
        sanitizedExchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        return chain.filter(sanitizedExchange);
    }

    public static String requestedTenantId(ServerWebExchange exchange) {
        return exchange.getAttribute(REQUESTED_TENANT_ID_ATTRIBUTE);
    }

    private String traceId(String value) {
        if (!hasText(value)) {
            return newTraceId();
        }
        String trimmed = value.trim();
        return TRACE_ID_PATTERN.matcher(trimmed).matches() ? trimmed : newTraceId();
    }

    private String newTraceId() {
        return "xuan-" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
