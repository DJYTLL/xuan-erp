package com.xuan.erp.iam.infrastructure.rpc;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.audit.AuditWriteEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * IAM 调用审计服务的 Feign 客户端。
 */
@FeignClient(name = "xuan-audit", contextId = "iamAuditWriteClient")
public interface AuditWriteClient {

    @PostMapping("/api/audit/logs")
    ApiResponse<AuditWriteClientResponse> write(@RequestBody AuditWriteEvent event);
}
