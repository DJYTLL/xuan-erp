package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantAssembler;
import com.xuan.erp.tenant.interfaces.dto.TenantInternalStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户内部状态查询接口，供 IAM 登录前校验租户生命周期和套餐有效性。
 */
@Tag(name = "租户内部状态", description = "服务内部使用的租户状态只读查询接口")
@RestController
@RequestMapping("/internal/tenants")
public class TenantInternalStatusController {

    private final TenantApplicationService tenantApplicationService;

    public TenantInternalStatusController(TenantApplicationService tenantApplicationService) {
        this.tenantApplicationService = tenantApplicationService;
    }

    @Operation(summary = "查询租户登录状态", description = "供 IAM 登录前查询租户是否允许登录")
    @GetMapping("/{tenantId}/status")
    public ApiResponse<TenantInternalStatusResponse> getTenantStatus(@PathVariable("tenantId") Long tenantId) {
        return ApiResponse.success(TenantAssembler.toInternalStatusResponse(
                tenantApplicationService.getTenantInternalStatus(tenantId)));
    }

    @Operation(summary = "按租户编码查询租户登录状态", description = "供 IAM 登录前用业务编码解析租户并校验是否允许登录")
    @GetMapping("/by-code/{tenantCode}/status")
    public ApiResponse<TenantInternalStatusResponse> getTenantStatusByCode(@PathVariable("tenantCode") String tenantCode) {
        return ApiResponse.success(TenantAssembler.toInternalStatusResponse(
                tenantApplicationService.getTenantInternalStatusByCode(tenantCode)));
    }
}
