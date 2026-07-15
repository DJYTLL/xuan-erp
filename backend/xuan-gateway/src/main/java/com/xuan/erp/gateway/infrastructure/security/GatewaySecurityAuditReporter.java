package com.xuan.erp.gateway.infrastructure.security;

import com.xuan.erp.common.audit.SafeAuditWritePublisher;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.audit.SecurityAuditEventFactory;
import com.xuan.erp.common.security.audit.SecurityAuditFailure;
import com.xuan.erp.common.security.audit.SecurityAuditRequest;
import com.xuan.erp.gateway.infrastructure.config.GatewaySecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 网关安全审计发布器。
 *
 * <p>该类只从 WebFlux 请求中提取审计上下文，并把标准事件交给公共异步发布器。
 * 审计写入失败由 {@link SafeAuditWritePublisher} 降级处理，不影响认证和授权响应。</p>
 */
public class GatewaySecurityAuditReporter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String REPORTED_ATTRIBUTE_PREFIX =
            GatewaySecurityAuditReporter.class.getName() + ".reported.";

    private final SafeAuditWritePublisher publisher;
    private final SecurityAuditEventFactory eventFactory;
    private final GatewaySecurityProperties properties;

    public GatewaySecurityAuditReporter(
            SafeAuditWritePublisher publisher,
            SecurityAuditEventFactory eventFactory,
            GatewaySecurityProperties properties) {
        this.publisher = publisher;
        this.eventFactory = eventFactory;
        this.properties = properties;
    }

    /**
     * 发布安全失败审计事件。
     */
    public Mono<Void> publish(
            ServerWebExchange exchange,
            SecurityAuditFailure failure,
            CurrentUser currentUser,
            Long requestedTenantId,
            String errorMessage) {
        if (!properties.getSecurityAudit().isEnabled()) {
            return Mono.empty();
        }
        return Mono.defer(() -> {
            String reportedAttribute = REPORTED_ATTRIBUTE_PREFIX + failure.name();
            if (Boolean.TRUE.equals(exchange.getAttribute(reportedAttribute))) {
                return Mono.empty();
            }
            exchange.getAttributes().put(reportedAttribute, Boolean.TRUE);
            return Mono.fromRunnable(() -> publisher.publish(eventFactory.failure(
                    failure,
                    new SecurityAuditRequest(
                            currentUser,
                            requestedTenantId,
                            requestId(exchange),
                            clientIp(exchange),
                            userAgent(exchange),
                            exchange.getRequest().getMethod().name(),
                            exchange.getRequest().getPath().pathWithinApplication().value(),
                            errorMessage
                    ))));
        });
    }

    private String requestId(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst(HEADER_REQUEST_ID);
    }

    private String userAgent(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return null;
        }
        return remoteAddress.getAddress().getHostAddress();
    }
}
