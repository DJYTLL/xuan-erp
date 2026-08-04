package com.xuan.erp.tenant.infrastructure.rpc;

import com.xuan.erp.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * xuan-tenant 调用 xuan-iam 的租户 IAM 初始化 Feign 客户端。
 */
@FeignClient(name = "xuan-iam", contextId = "tenantIamBootstrapClient")
public interface IamTenantBootstrapClient {

    @PostMapping("/internal/iam/tenant-bootstrap/{tenantId}")
    ApiResponse<Integer> bootstrapTenant(
            @PathVariable("tenantId") Long tenantId,
            @RequestBody IamTenantBootstrapSyncRequest request,
            @RequestParam("requestedBy") String requestedBy);

    @GetMapping("/internal/iam/tenant-bootstrap/{tenantId}/permission-sync-state")
    ApiResponse<IamTenantPermissionSyncStateResponse> getPermissionSyncState(@PathVariable("tenantId") Long tenantId);
}
