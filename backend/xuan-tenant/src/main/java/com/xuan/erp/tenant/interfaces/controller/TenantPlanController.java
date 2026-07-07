package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantPlanApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantAssembler;
import com.xuan.erp.tenant.interfaces.assembler.TenantPlanAssembler;
import com.xuan.erp.tenant.interfaces.dto.ChangeTenantPlanStatusRequest;
import com.xuan.erp.tenant.interfaces.dto.CreateTenantPlanRequest;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantPlanResponse;
import com.xuan.erp.tenant.interfaces.dto.UpdateTenantPlanRequest;
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
 * 租户套餐接口控制器，负责租户套餐的查询、创建、修改、启用、停用和软删除。
 */
@Tag(name = "租户套餐", description = "租户套餐查询、创建、修改、启用、停用和软删除接口")
@RestController
@RequestMapping("/api/tenant-plans")
public class TenantPlanController {

    private final TenantPlanApplicationService tenantPlanApplicationService;

    public TenantPlanController(TenantPlanApplicationService tenantPlanApplicationService) {
        this.tenantPlanApplicationService = tenantPlanApplicationService;
    }

    /**
     * 查询所有未删除的租户套餐列表。
     */
    @Operation(summary = "查询租户套餐列表", description = "查询所有未逻辑删除的租户套餐")
    @GetMapping
    public ApiResponse<List<TenantPlanResponse>> listPlans() {
        return ApiResponse.success(tenantPlanApplicationService.listPlans().stream()
                .map(TenantPlanAssembler::toResponse)
                .toList());
    }

    /**
     * 根据套餐 ID 查询单个租户套餐详情。
     */
    @Operation(summary = "查询租户套餐详情", description = "根据套餐 ID 查询单个租户套餐详情")
    @GetMapping("/{planId}")
    public ApiResponse<TenantPlanResponse> getPlan(@PathVariable("planId") Long planId) {
        return ApiResponse.success(TenantPlanAssembler.toResponse(tenantPlanApplicationService.getPlan(planId)));
    }

    /**
     * 创建新租户套餐，默认创建为启用状态。
     */
    @Operation(summary = "创建租户套餐", description = "创建新租户套餐，默认创建为启用状态")
    @PostMapping
    public ApiResponse<TenantPlanResponse> createPlan(@RequestBody CreateTenantPlanRequest request) {
        return ApiResponse.success(TenantPlanAssembler.toResponse(tenantPlanApplicationService.createPlan(TenantPlanAssembler.toCommand(request))));
    }

    /**
     * 修改租户套餐基础信息，例如名称、计费周期、价格、额度和功能开关。
     */
    @Operation(summary = "修改租户套餐", description = "修改租户套餐名称、计费周期、价格、额度和功能开关")
    @PutMapping("/{planId}")
    public ApiResponse<TenantPlanResponse> updatePlan(@PathVariable("planId") Long planId, @RequestBody UpdateTenantPlanRequest request) {
        return ApiResponse.success(TenantPlanAssembler.toResponse(tenantPlanApplicationService.updatePlan(planId, TenantPlanAssembler.toCommand(request))));
    }

    /**
     * 启用指定租户套餐，并记录操作原因和操作人。
     */
    @Operation(summary = "启用租户套餐", description = "启用指定租户套餐，并记录操作原因和操作人")
    @PostMapping("/{planId}/enable")
    public ApiResponse<TenantPlanResponse> enablePlan(@PathVariable("planId") Long planId, @RequestBody ChangeTenantPlanStatusRequest request) {
        return ApiResponse.success(TenantPlanAssembler.toResponse(tenantPlanApplicationService.enablePlan(planId, TenantPlanAssembler.toCommand(request))));
    }

    /**
     * 停用指定租户套餐，并要求记录停用原因。
     */
    @Operation(summary = "停用租户套餐", description = "停用指定租户套餐，并要求记录停用原因")
    @PostMapping("/{planId}/disable")
    public ApiResponse<TenantPlanResponse> disablePlan(@PathVariable("planId") Long planId, @RequestBody ChangeTenantPlanStatusRequest request) {
        return ApiResponse.success(TenantPlanAssembler.toResponse(tenantPlanApplicationService.disablePlan(planId, TenantPlanAssembler.toCommand(request))));
    }

    /**
     * 软删除指定租户套餐，并要求记录删除原因。
     */
    @Operation(summary = "删除租户套餐", description = "软删除指定租户套餐，并要求记录删除原因")
    @DeleteMapping("/{planId}")
    public ApiResponse<Void> deletePlan(@PathVariable("planId") Long planId, @RequestBody DeleteRequest request) {
        tenantPlanApplicationService.deletePlan(planId, TenantAssembler.toCommand(request));
        return ApiResponse.success(null);
    }
}
