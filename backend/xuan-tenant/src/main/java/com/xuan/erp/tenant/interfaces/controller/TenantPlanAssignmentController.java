package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantPlanAssignmentApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantPlanAssignmentAssembler;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantPlanAssignmentRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantPlanAssignmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户套餐分配接口控制器，负责租户绑定套餐、套餐变更记录和分配记录软删除。
 */
@Tag(name = "租户套餐分配", description = "租户绑定套餐、套餐变更记录和分配记录软删除接口")
@RestController
@RequestMapping("/api/tenant-plan-assignments")
public class TenantPlanAssignmentController {

    private final TenantPlanAssignmentApplicationService service;

    public TenantPlanAssignmentController(TenantPlanAssignmentApplicationService service) {
        this.service = service;
    }

    /**
     * 查询所有未删除的租户套餐分配记录。
     */
    @Operation(summary = "查询租户套餐分配列表", description = "查询所有未逻辑删除的租户套餐分配记录")
    @GetMapping
    @PreAuthorize("@xuanPermission.has('tenant-plan:view')")
    public ApiResponse<List<TenantPlanAssignmentResponse>> listAssignments() {
        return ApiResponse.success(service.listAssignments().stream()
                .map(TenantPlanAssignmentAssembler::toResponse)
                .toList());
    }

    /**
     * 根据分配记录 ID 查询租户套餐分配详情。
     */
    @Operation(summary = "查询租户套餐分配详情", description = "根据分配记录 ID 查询租户套餐分配详情")
    @GetMapping("/{assignmentId}")
    @PreAuthorize("@xuanPermission.has('tenant-plan:view')")
    public ApiResponse<TenantPlanAssignmentResponse> getAssignment(@PathVariable("assignmentId") Long assignmentId) {
        return ApiResponse.success(TenantPlanAssignmentAssembler.toResponse(service.getAssignment(assignmentId)));
    }

    /**
     * 创建租户套餐分配记录。
     */
    @Operation(summary = "创建租户套餐分配", description = "为租户创建套餐分配记录，记录套餐、生效时间和到期时间")
    @PostMapping
    @PreAuthorize("@xuanPermission.has('tenant-plan:assign')")
    public ApiResponse<TenantPlanAssignmentResponse> createAssignment(@RequestBody TenantPlanAssignmentRequest request) {
        return ApiResponse.success(TenantPlanAssignmentAssembler.toResponse(service.createAssignment(TenantPlanAssignmentAssembler.toValues(request))));
    }

    /**
     * 修改租户套餐分配记录，例如状态、生效时间、到期时间和变更原因。
     */
    @Operation(summary = "修改租户套餐分配", description = "修改租户套餐分配状态、生效时间、到期时间和变更原因")
    @PutMapping("/{assignmentId}")
    @PreAuthorize("@xuanPermission.has('tenant-plan:assign')")
    public ApiResponse<TenantPlanAssignmentResponse> updateAssignment(@PathVariable("assignmentId") Long assignmentId, @RequestBody TenantPlanAssignmentRequest request) {
        return ApiResponse.success(TenantPlanAssignmentAssembler.toResponse(service.updateAssignment(assignmentId, TenantPlanAssignmentAssembler.toValues(request))));
    }

    /**
     * 软删除租户套餐分配记录，并要求记录删除原因。
     */
    @Operation(summary = "删除租户套餐分配", description = "软删除租户套餐分配记录，并要求记录删除原因")
    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("@xuanPermission.has('tenant-plan:assign')")
    public ApiResponse<Void> deleteAssignment(@PathVariable("assignmentId") Long assignmentId, @RequestBody DeleteRequest request) {
        service.deleteAssignment(assignmentId, request.reason(), request.operator());
        return ApiResponse.success(null);
    }
}
