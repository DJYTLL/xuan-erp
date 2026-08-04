package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.command.CreateIamRoleCommand;
import com.xuan.erp.iam.application.command.SetIamRolePermissionsCommand;
import com.xuan.erp.iam.application.command.UpdateIamRoleCommand;
import com.xuan.erp.iam.application.query.IamRolePermissionGrantView;
import com.xuan.erp.iam.application.service.IamRoleApplicationService;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.interfaces.dto.CreateIamRoleRequest;
import com.xuan.erp.iam.interfaces.dto.SetIamRolePermissionsRequest;
import com.xuan.erp.iam.interfaces.dto.UpdateIamRoleRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 角色管理接口控制器，负责租户内角色查询入口。
 */
@Tag(name = "IAM 角色管理", description = "租户内角色查询接口")
@RestController
@RequestMapping("/api/iam/roles")
public class IamRoleController {

    private final IamRoleApplicationService roleApplicationService;

    public IamRoleController(IamRoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    @Operation(summary = "查询角色列表", description = "按租户查询未逻辑删除的 IAM 角色")
    @PreAuthorize("hasAuthority('iam-role:view')")
    @GetMapping
    public ApiResponse<List<IamRole>> listRoles(
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId) {
        return ApiResponse.success(roleApplicationService.listRoles(tenantId));
    }

    @Operation(summary = "新增角色", description = "在指定租户下新增 IAM 角色")
    @PreAuthorize("hasAuthority('iam-role:create')")
    @PostMapping
    public ApiResponse<IamRole> createRole(@RequestBody CreateIamRoleRequest request) {
        return ApiResponse.success(roleApplicationService.createRole(new CreateIamRoleCommand(
                request.tenantId(),
                request.code(),
                request.name(),
                request.description())));
    }

    @Operation(summary = "修改角色", description = "修改 IAM 角色基础资料和启用状态")
    @PreAuthorize("hasAuthority('iam-role:update')")
    @PutMapping("/{roleId}")
    public ApiResponse<IamRole> updateRole(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @RequestBody UpdateIamRoleRequest request) {
        return ApiResponse.success(roleApplicationService.updateRole(roleId, new UpdateIamRoleCommand(
                request.name(),
                request.description(),
                request.enabled())));
    }

    @Operation(summary = "启用角色", description = "启用 IAM 角色")
    @PreAuthorize("hasAuthority('iam-role:update')")
    @PostMapping("/{roleId}/enable")
    public ApiResponse<IamRole> enableRole(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId) {
        return ApiResponse.success(roleApplicationService.setRoleEnabled(roleId, true));
    }

    @Operation(summary = "停用角色", description = "停用 IAM 角色")
    @PreAuthorize("hasAuthority('iam-role:update')")
    @PostMapping("/{roleId}/disable")
    public ApiResponse<IamRole> disableRole(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId) {
        return ApiResponse.success(roleApplicationService.setRoleEnabled(roleId, false));
    }

    @Operation(summary = "查询角色权限", description = "查询指定租户角色当前拥有的权限编码")
    @PreAuthorize("hasAuthority('iam-role:view')")
    @GetMapping("/{roleId}/permissions")
    public ApiResponse<IamRolePermissionGrantView> getRolePermissions(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId) {
        return ApiResponse.success(roleApplicationService.getRolePermissions(tenantId, roleId));
    }

    @Operation(summary = "保存角色权限", description = "使用权限编码集合替换指定角色当前权限授权")
    @PreAuthorize("hasAuthority('iam-role:update')")
    @PutMapping("/{roleId}/permissions")
    public ApiResponse<IamRolePermissionGrantView> setRolePermissions(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @RequestBody SetIamRolePermissionsRequest request) {
        return ApiResponse.success(roleApplicationService.replaceRolePermissions(new SetIamRolePermissionsCommand(
                request.tenantId(),
                roleId,
                request.permissionCodes(),
                request.operator())));
    }
}
