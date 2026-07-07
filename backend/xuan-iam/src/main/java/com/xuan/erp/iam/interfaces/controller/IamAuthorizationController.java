package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.service.IamAuthorizationApplicationService;
import com.xuan.erp.iam.interfaces.assembler.IamAuthorizationAssembler;
import com.xuan.erp.iam.interfaces.dto.IamAuthorizationSnapshotResponse;
import com.xuan.erp.iam.interfaces.dto.RebuildAuthorizationSnapshotRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 授权快照接口控制器，负责查询和重建用户授权快照。
 */
@Tag(name = "IAM 授权快照", description = "用户角色、权限、菜单和列权限快照接口")
@RestController
@RequestMapping("/api/iam/authorization-snapshots")
public class IamAuthorizationController {

    private final IamAuthorizationApplicationService authorizationApplicationService;

    public IamAuthorizationController(IamAuthorizationApplicationService authorizationApplicationService) {
        this.authorizationApplicationService = authorizationApplicationService;
    }

    @Operation(summary = "查询授权快照", description = "按租户和用户查询授权快照")
    @PreAuthorize("hasAuthority('iam:view')")
    @GetMapping
    public ApiResponse<IamAuthorizationSnapshotResponse> getSnapshot(
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId,
            @Parameter(description = "用户 ID")
            @RequestParam("userId") Long userId) {
        return ApiResponse.success(IamAuthorizationAssembler.toResponse(authorizationApplicationService.getSnapshot(tenantId, userId)));
    }

    @Operation(summary = "重建授权快照", description = "根据角色、权限和菜单编码重建用户授权快照")
    @PreAuthorize("hasAuthority('iam:update')")
    @PostMapping("/rebuild")
    public ApiResponse<IamAuthorizationSnapshotResponse> rebuildSnapshot(@RequestBody RebuildAuthorizationSnapshotRequest request) {
        return ApiResponse.success(IamAuthorizationAssembler.toResponse(authorizationApplicationService.rebuildSnapshot(IamAuthorizationAssembler.toCommand(request))));
    }
}
