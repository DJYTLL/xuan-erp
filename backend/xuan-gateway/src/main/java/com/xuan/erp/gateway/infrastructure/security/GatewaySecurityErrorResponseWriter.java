package com.xuan.erp.gateway.infrastructure.security;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.security.audit.SecurityAuditFailure;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关安全异常响应写入器。
 *
 * <p>Spring Security 默认只写状态码。这里统一输出 Xuan ERP 的
 * {@link ApiResponse} JSON 结构，让前端能稳定识别 401/403 的错误码。</p>
 */
public class GatewaySecurityErrorResponseWriter {

    public Mono<Void> write(ServerWebExchange exchange, SecurityAuditFailure failure) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(failure.httpStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = serialize(ApiResponse.failure(failure.errorCode(), message(failure)));
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private byte[] serialize(ApiResponse<Void> response) {
        String body = "{\"code\":\"" + escape(response.code())
                + "\",\"message\":\"" + escape(response.message())
                + "\",\"data\":null}";
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String message(SecurityAuditFailure failure) {
        return switch (failure) {
            case AUTHENTICATION_MISSING -> "未登录或登录已过期";
            case TOKEN_INVALID -> "访问令牌无效或已过期";
            case PERMISSION_DENIED -> "没有访问权限";
            case TENANT_CONTEXT_INVALID -> "租户上下文无效";
            case JWKS_UNAVAILABLE -> "认证服务暂不可用";
        };
    }
}
