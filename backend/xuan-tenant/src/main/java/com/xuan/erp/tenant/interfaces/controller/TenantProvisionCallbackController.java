package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantProvisionApplicationService;
import com.xuan.erp.tenant.interfaces.dto.TenantProvisionCallbackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "租户初始化回调", description = "接收 IAM 和业务服务的租户初始化步骤回执")
@RestController
@RequestMapping("/internal")
public class TenantProvisionCallbackController {

    private final TenantProvisionApplicationService service;

    public TenantProvisionCallbackController(TenantProvisionApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "回写租户初始化结果", description = "接收 IAM_BOOTSTRAP 等初始化步骤的成功或失败回执")
    @PreAuthorize("@xuanPermission.has('tenant-provision:callback')")
    @PostMapping("/tenants/{tenantId}/provision-callbacks")
    public ApiResponse<Void> handleCallback(
            @Parameter(description = "租户 ID")
            @PathVariable("tenantId") @Min(value = 1, message = "租户 ID 必须大于 0") Long tenantId,
            @Valid @RequestBody TenantProvisionCallbackRequest request) {
        service.handleProvisionCallback(tenantId, request.toCommand());
        return ApiResponse.success(null);
    }
}

