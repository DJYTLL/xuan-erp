package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.service.IamTenantBootstrapApplicationService;
import com.xuan.erp.iam.interfaces.dto.IamTenantBootstrapRequest;
import com.xuan.erp.iam.interfaces.dto.IamTenantPermissionSyncStateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 租户初始化接口控制器，负责租户开通后的 IAM 基础授权初始化入口。
 */
@Tag(name = "IAM 租户初始化", description = "租户开通后的菜单授权初始化接口")
@RestController
@RequestMapping("/internal/iam/tenant-bootstrap")
public class IamTenantBootstrapController {

    private final IamTenantBootstrapApplicationService tenantBootstrapApplicationService;

    public IamTenantBootstrapController(IamTenantBootstrapApplicationService tenantBootstrapApplicationService) {
        this.tenantBootstrapApplicationService = tenantBootstrapApplicationService;
    }

    @Operation(summary = "初始化租户 IAM", description = "为指定租户触发 IAM 菜单授权初始化")
    @PreAuthorize("@xuanPermission.has('iam:create')")
    @PostMapping("/{tenantId}")
    public ApiResponse<Integer> bootstrapTenant(
            @Parameter(description = "租户 ID")
            @PathVariable("tenantId") Long tenantId,
            @RequestBody(required = false) IamTenantBootstrapRequest request,
            @Parameter(description = "触发 IAM 初始化的服务或操作人")
            @RequestParam(value = "requestedBy", required = false) String requestedBy) {
        return ApiResponse.success(tenantBootstrapApplicationService.bootstrapTenant(
                tenantId,
                request == null ? null : request.toCommand(),
                request == null ? null : request.iamInitTemplateCode(),
                request == null ? null : request.columnPermissionTemplateCodes(),
                request == null ? null : request.defaultColumnPermissionTemplateCode(),
                request == null ? null : request.permissionHash(),
                requestedBy));
    }

    @Operation(summary = "查询租户 IAM 权限同步状态", description = "供 Tenant 对比当前套餐权限指纹和 IAM 最后同步指纹")
    @PreAuthorize("@xuanPermission.hasAny('tenant:view', 'tenant-plan:assign')")
    @GetMapping("/{tenantId}/permission-sync-state")
    public ApiResponse<IamTenantPermissionSyncStateResponse> getPermissionSyncState(
            @Parameter(description = "租户 ID")
            @PathVariable("tenantId") Long tenantId) {
        return ApiResponse.success(new IamTenantPermissionSyncStateResponse(
                tenantId,
                tenantBootstrapApplicationService.lastSyncedPermissionHash(tenantId)));
    }
}
