package com.xuan.erp.iam.infrastructure.rpc;

import com.xuan.erp.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * IAM 调用 xuan-tenant 的租户状态查询 Feign 客户端。
 */
@FeignClient(name = "xuan-tenant", contextId = "iamTenantStatusClient")
public interface TenantStatusClient {

    @GetMapping("/internal/tenants/{tenantId}/status")
    ApiResponse<TenantStatusClientResponse> getTenantStatus(@PathVariable("tenantId") Long tenantId);
}
