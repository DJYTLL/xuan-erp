package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantAssembler;
import com.xuan.erp.tenant.interfaces.dto.ChangeTenantStatusRequest;
import com.xuan.erp.tenant.interfaces.dto.CreateTenantRequest;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantResponse;
import com.xuan.erp.tenant.interfaces.dto.UpdateTenantRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户管理接口控制器，负责租户的查询、创建、修改、启用、停用和软删除。
 */
@Tag(name = "租户管理", description = "租户主档查询、创建、修改、启用、停用和软删除接口")
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantApplicationService tenantApplicationService;

    public TenantController(TenantApplicationService tenantApplicationService) {
        this.tenantApplicationService = tenantApplicationService;
    }

    /**
     * 查询所有未删除的租户列表。
     */
    @Operation(summary = "查询租户列表", description = "查询所有未逻辑删除的租户主档")
    @GetMapping
    public ApiResponse<List<TenantResponse>> listTenants() {
        return ApiResponse.success(tenantApplicationService.listTenants().stream()
                .map(TenantAssembler::toResponse)
                .toList());
    }

    /**
     * 根据租户 ID 查询单个租户详情。
     */
    @Operation(summary = "查询租户详情", description = "根据租户 ID 查询单个租户主档详情")
    @GetMapping("/{tenantId}")
    public ApiResponse<TenantResponse> getTenant(@PathVariable("tenantId") Long tenantId) {
        return ApiResponse.success(TenantAssembler.toResponse(tenantApplicationService.getTenant(tenantId)));
    }

    /**
     * 创建新租户，并初始化为开通中的租户状态。
     */
    @Operation(summary = "创建租户", description = "创建新租户，并初始化为开通中的租户状态")
    @PostMapping
    public ApiResponse<TenantResponse> createTenant(@RequestBody CreateTenantRequest request) {
        return ApiResponse.success(TenantAssembler.toResponse(tenantApplicationService.createTenant(TenantAssembler.toCommand(request))));
    }

    /**
     * 修改租户基础信息，例如租户名称、联系人和备注。
     */
    @Operation(summary = "修改租户", description = "修改租户名称、联系人、联系电话和备注")
    @PutMapping("/{tenantId}")
    public ApiResponse<TenantResponse> updateTenant(@PathVariable("tenantId") Long tenantId, @RequestBody UpdateTenantRequest request) {
        return ApiResponse.success(TenantAssembler.toResponse(tenantApplicationService.updateTenant(tenantId, TenantAssembler.toCommand(request))));
    }

    /**
     * 启用指定租户，并记录状态变更原因和操作人。
     */
    @Operation(summary = "启用租户", description = "启用指定租户，并记录状态变更原因和操作人")
    @PostMapping("/{tenantId}/enable")
    public ApiResponse<TenantResponse> enableTenant(@PathVariable("tenantId") Long tenantId, @RequestBody ChangeTenantStatusRequest request) {
        return ApiResponse.success(TenantAssembler.toResponse(tenantApplicationService.enableTenant(tenantId, TenantAssembler.toCommand(request))));
    }

    /**
     * 停用指定租户，并要求记录停用原因。
     */
    @Operation(summary = "停用租户", description = "停用指定租户，并要求记录停用原因")
    @PostMapping("/{tenantId}/disable")
    public ApiResponse<TenantResponse> disableTenant(@PathVariable("tenantId") Long tenantId, @RequestBody ChangeTenantStatusRequest request) {
        return ApiResponse.success(TenantAssembler.toResponse(tenantApplicationService.disableTenant(tenantId, TenantAssembler.toCommand(request))));
    }

    /**
     * 软删除指定租户，并要求记录删除原因。
     */
    @Operation(summary = "删除租户", description = "软删除指定租户，并要求记录删除原因")
    @DeleteMapping("/{tenantId}")
    public ApiResponse<Void> deleteTenant(@PathVariable("tenantId") Long tenantId, @RequestBody DeleteRequest request) {
        tenantApplicationService.deleteTenant(tenantId, TenantAssembler.toCommand(request));
        return ApiResponse.success(null);
    }
}
