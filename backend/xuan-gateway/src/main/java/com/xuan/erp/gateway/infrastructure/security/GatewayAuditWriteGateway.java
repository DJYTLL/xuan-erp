package com.xuan.erp.gateway.infrastructure.security;

import com.xuan.erp.common.audit.AuditWriteEvent;
import com.xuan.erp.common.audit.AuditWriteGateway;
import com.xuan.erp.common.audit.AuditWriteReceipt;
import com.xuan.erp.gateway.infrastructure.config.GatewaySecurityProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * 网关侧审计写入适配器。
 *
 * <p>网关是 WebFlux 应用，不复用 IAM 的 Feign 适配器；这里在异步审计线程中通过
 * 服务发现直连 xuan-audit 写入接口，避免把审计请求再次绕回网关自身路由。</p>
 */
public class GatewayAuditWriteGateway implements AuditWriteGateway {

    private static final String AUDIT_LOG_PATH = "/api/audit/logs";

    private final DiscoveryClient discoveryClient;
    private final WebClient webClient;
    private final GatewaySecurityProperties properties;

    public GatewayAuditWriteGateway(
            DiscoveryClient discoveryClient,
            WebClient webClient,
            GatewaySecurityProperties properties) {
        this.discoveryClient = discoveryClient;
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public AuditWriteReceipt write(AuditWriteEvent event) {
        webClient.post()
                .uri(auditWriteUri())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(properties.getSecurityAudit().getWriteTimeoutSeconds()));
        return new AuditWriteReceipt(null);
    }

    private URI auditWriteUri() {
        String serviceName = properties.getSecurityAudit().getAuditServiceName();
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException("未发现审计服务实例: " + serviceName);
        }
        return UriComponentsBuilder.fromUri(instances.getFirst().getUri())
                .path(AUDIT_LOG_PATH)
                .build(true)
                .toUri();
    }
}
