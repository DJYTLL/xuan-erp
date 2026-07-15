package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.command.CreateIamPermissionCommand;
import com.xuan.erp.iam.application.command.UpdateIamPermissionCommand;
import com.xuan.erp.iam.application.service.IamPermissionApplicationService;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.interfaces.dto.CreateIamPermissionRequest;
import com.xuan.erp.iam.interfaces.dto.UpdateIamPermissionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 权限目录接口控制器，负责全局权限清单查询入口。
 */
@Tag(name = "IAM 权限目录", description = "全局权限清单查询接口")
@RestController
@RequestMapping("/api/iam/permissions")
public class IamPermissionController {

    private final IamPermissionApplicationService permissionApplicationService;

    public IamPermissionController(IamPermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    @Operation(summary = "查询权限清单", description = "查询 IAM 中维护的全局权限定义")
    @PreAuthorize("hasAuthority('iam:view')")
    @GetMapping
    public ApiResponse<List<IamPermission>> listPermissions() {
        return ApiResponse.success(permissionApplicationService.listPermissions());
    }

    @Operation(summary = "新增权限", description = "新增 IAM 全局权限定义")
    @PreAuthorize("hasAuthority('iam:create')")
    @PostMapping
    public ApiResponse<IamPermission> createPermission(@RequestBody CreateIamPermissionRequest request) {
        return ApiResponse.success(permissionApplicationService.createPermission(new CreateIamPermissionCommand(
                request.code(),
                request.name(),
                request.serviceName(),
                request.menuCode(),
                request.description())));
    }

    @Operation(summary = "修改权限", description = "修改 IAM 全局权限定义")
    @PreAuthorize("hasAuthority('iam:update')")
    @PutMapping("/{permissionId}")
    public ApiResponse<IamPermission> updatePermission(
            @Parameter(description = "权限 ID")
            @PathVariable("permissionId") Long permissionId,
            @RequestBody UpdateIamPermissionRequest request) {
        return ApiResponse.success(permissionApplicationService.updatePermission(permissionId, new UpdateIamPermissionCommand(
                request.name(),
                request.serviceName(),
                request.menuCode(),
                request.description(),
                request.enabled())));
    }

    @Operation(summary = "启用权限", description = "启用 IAM 权限定义")
    @PreAuthorize("hasAuthority('iam:update')")
    @PostMapping("/{permissionId}/enable")
    public ApiResponse<IamPermission> enablePermission(
            @Parameter(description = "权限 ID")
            @PathVariable("permissionId") Long permissionId) {
        return ApiResponse.success(permissionApplicationService.setPermissionEnabled(permissionId, true));
    }

    @Operation(summary = "停用权限", description = "停用 IAM 权限定义")
    @PreAuthorize("hasAuthority('iam:update')")
    @PostMapping("/{permissionId}/disable")
    public ApiResponse<IamPermission> disablePermission(
            @Parameter(description = "权限 ID")
            @PathVariable("permissionId") Long permissionId) {
        return ApiResponse.success(permissionApplicationService.setPermissionEnabled(permissionId, false));
    }
}
