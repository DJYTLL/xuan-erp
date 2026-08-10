package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.command.CreateIamUserCommand;
import com.xuan.erp.iam.application.command.ResetIamUserPasswordCommand;
import com.xuan.erp.iam.application.command.SetIamUserRolesCommand;
import com.xuan.erp.iam.application.command.UpdateIamUserCommand;
import com.xuan.erp.iam.application.query.IamUserRoleGrantView;
import com.xuan.erp.iam.application.query.IamUserDetailView;
import com.xuan.erp.iam.application.service.IamUserApplicationService;
import com.xuan.erp.iam.interfaces.assembler.IamUserAssembler;
import com.xuan.erp.iam.interfaces.dto.CreateIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.DisableIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.IamUserResponse;
import com.xuan.erp.iam.interfaces.dto.ResetIamUserPasswordRequest;
import com.xuan.erp.iam.interfaces.dto.SetIamUserRolesRequest;
import com.xuan.erp.iam.interfaces.dto.UpdateIamUserRequest;
import com.xuan.erp.iam.interfaces.security.IamUserColumnPermissionApplier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
 * IAM 用户管理接口控制器，负责租户内用户账号查询、创建和停用。
 */
@Tag(name = "IAM 用户管理", description = "租户内用户账号查询、创建和停用接口")
@RestController
@RequestMapping("/api/iam/users")
public class IamUserController {

    private final IamUserApplicationService userApplicationService;
    private final IamUserColumnPermissionApplier iamUserColumnPermissionApplier;

    public IamUserController(IamUserApplicationService userApplicationService) {
        this(userApplicationService, null);
    }

    @Autowired
    public IamUserController(
            IamUserApplicationService userApplicationService,
            IamUserColumnPermissionApplier iamUserColumnPermissionApplier) {
        this.userApplicationService = userApplicationService;
        this.iamUserColumnPermissionApplier = iamUserColumnPermissionApplier;
    }

    @Operation(summary = "查询用户列表", description = "按租户查询未逻辑删除的 IAM 用户列表")
    @PreAuthorize("@xuanPermission.has('iam-user:view')")
    @GetMapping
    public ApiResponse<List<IamUserResponse>> listUsers(
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId,
            Authentication authentication) {
        return ApiResponse.success(userApplicationService.listUsers(scopedTenantId(tenantId, authentication)).stream()
                .map(IamUserAssembler::toResponse)
                .map(this::applyColumnPermissions)
                .toList());
    }

    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询 IAM 用户详情")
    @PreAuthorize("@xuanPermission.has('iam-user:view')")
    @GetMapping("/{userId}")
    public ApiResponse<IamUserResponse> getUser(
            @Parameter(description = "用户 ID")
            @PathVariable("userId") Long userId,
            Authentication authentication) {
        IamUserDetailView user = userApplicationService.getUser(userId);
        assertTenantAccess(user.tenantId(), authentication);
        return ApiResponse.success(applyColumnPermissions(IamUserAssembler.toResponse(user)));
    }

    @Operation(summary = "创建用户", description = "创建租户内 IAM 用户账号")
    @PreAuthorize("@xuanPermission.has('iam-user:create')")
    @PostMapping
    public ApiResponse<IamUserResponse> createUser(
            @RequestBody CreateIamUserRequest request,
            Authentication authentication) {
        return ApiResponse.success(applyColumnPermissions(IamUserAssembler.toResponse(userApplicationService.createUser(new CreateIamUserCommand(
                scopedTenantId(request.tenantId(), authentication),
                request.username(),
                request.initialPassword(),
                request.displayName(),
                request.email(),
                request.phone(),
                request.remark(),
                operator(request.operator(), authentication))))));
    }

    @Operation(summary = "修改用户资料", description = "修改指定 IAM 用户基础资料，不允许通过该接口修改用户名和密码")
    @PreAuthorize("@xuanPermission.has('iam-user:update')")
    @PutMapping("/{userId}")
    public ApiResponse<IamUserResponse> updateUser(
            @Parameter(description = "用户 ID")
            @PathVariable("userId") Long userId,
            @RequestBody UpdateIamUserRequest request,
            Authentication authentication) {
        return ApiResponse.success(applyColumnPermissions(IamUserAssembler.toResponse(userApplicationService.updateUser(userId, new UpdateIamUserCommand(
                scopedTenantId(request.tenantId(), authentication),
                request.displayName(),
                request.email(),
                request.phone(),
                request.enabled(),
                request.remark(),
                operator(request.operator(), authentication))))));
    }

    @Operation(summary = "重置用户密码", description = "管理员重置指定 IAM 用户密码，并提升该用户权限版本")
    @PreAuthorize("@xuanPermission.has('iam-user:reset-password')")
    @PostMapping("/{userId}/reset-password")
    public ApiResponse<IamUserResponse> resetPassword(
            @Parameter(description = "用户 ID")
            @PathVariable("userId") Long userId,
            @RequestBody ResetIamUserPasswordRequest request,
            Authentication authentication) {
        return ApiResponse.success(applyColumnPermissions(IamUserAssembler.toResponse(userApplicationService.resetPassword(userId, new ResetIamUserPasswordCommand(
                scopedTenantId(request.tenantId(), authentication),
                request.newPassword(),
                operator(request.operator(), authentication))))));
    }

    @Operation(summary = "重置租户管理员密码", description = "在租户管理页按租户重置该租户 admin 管理员账号密码")
    @PreAuthorize("@xuanPermission.has('tenant:admin-password:reset')")
    @PostMapping("/tenants/{tenantId}/admin/reset-password")
    public ApiResponse<IamUserResponse> resetTenantAdminPassword(
            @Parameter(description = "租户 ID")
            @PathVariable("tenantId") Long tenantId,
            @RequestBody ResetIamUserPasswordRequest request,
            Authentication authentication) {
        Long scopedTenantId = scopedTenantId(tenantId, authentication);
        return ApiResponse.success(applyColumnPermissions(IamUserAssembler.toResponse(userApplicationService.resetTenantAdminPassword(
                scopedTenantId,
                new ResetIamUserPasswordCommand(
                        scopedTenantId,
                        request.newPassword(),
                        operator(request.operator(), authentication))))));
    }

    @Operation(summary = "停用用户", description = "停用指定 IAM 用户并提升权限版本")
    @PreAuthorize("@xuanPermission.has('iam-user:delete')")
    @PostMapping("/{userId}/disable")
    public ApiResponse<IamUserResponse> disableUser(
            @Parameter(description = "用户 ID")
            @PathVariable("userId") Long userId,
            @RequestBody DisableIamUserRequest request,
            Authentication authentication) {
        IamUserDetailView user = userApplicationService.getUser(userId);
        assertTenantAccess(user.tenantId(), authentication);
        return ApiResponse.success(applyColumnPermissions(IamUserAssembler.toResponse(userApplicationService.disableUser(userId, IamUserAssembler.toCommand(request)))));
    }

    @Operation(summary = "查询用户角色", description = "查询指定租户用户当前拥有的角色 ID")
    @PreAuthorize("@xuanPermission.has('iam-user:view')")
    @GetMapping("/{userId}/roles")
    public ApiResponse<IamUserRoleGrantView> getUserRoles(
            @Parameter(description = "用户 ID")
            @PathVariable("userId") Long userId,
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId,
            Authentication authentication) {
        return ApiResponse.success(userApplicationService.getUserRoles(scopedTenantId(tenantId, authentication), userId));
    }

    @Operation(summary = "保存用户角色", description = "使用角色 ID 集合替换指定用户当前角色授权，并刷新该用户授权快照")
    @PreAuthorize("@xuanPermission.has('iam-user:update')")
    @PutMapping("/{userId}/roles")
    public ApiResponse<IamUserRoleGrantView> setUserRoles(
            @Parameter(description = "用户 ID")
            @PathVariable("userId") Long userId,
            @RequestBody SetIamUserRolesRequest request,
            Authentication authentication) {
        return ApiResponse.success(userApplicationService.replaceUserRoles(new SetIamUserRolesCommand(
                scopedTenantId(request.tenantId(), authentication),
                userId,
                request.roleIds(),
                operator(request.operator(), authentication))));
    }

    private Long scopedTenantId(Long requestedTenantId, Authentication authentication) {
        CurrentUser currentUser = currentUser(authentication);
        if (isPlatformSuperAdmin(currentUser)) {
            return requestedTenantId;
        }
        if (requestedTenantId != null && !requestedTenantId.equals(currentUser.tenantId())) {
            throw new BusinessException("IAM_TENANT_ACCESS_DENIED", "当前账号无权访问该租户资源");
        }
        return currentUser.tenantId();
    }

    private void assertTenantAccess(Long tenantId, Authentication authentication) {
        CurrentUser currentUser = currentUser(authentication);
        if (!isPlatformSuperAdmin(currentUser) && !tenantId.equals(currentUser.tenantId())) {
            throw new BusinessException("IAM_TENANT_ACCESS_DENIED", "当前账号无权访问该租户资源");
        }
    }

    private CurrentUser currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException("IAM_UNAUTHENTICATED", "当前账号未认证");
        }
        return currentUser;
    }

    private boolean isPlatformSuperAdmin(CurrentUser currentUser) {
        return Long.valueOf(0L).equals(currentUser.tenantId())
                && (currentUser.roles().contains("super_admin")
                || currentUser.permissions().contains("*")
                || "super_admin".equals(currentUser.username())
                || "superadmin".equals(currentUser.username()));
    }

    private IamUserResponse applyColumnPermissions(IamUserResponse response) {
        return iamUserColumnPermissionApplier == null ? response : iamUserColumnPermissionApplier.applyToUserResponse(response);
    }

    private String operator(String requestedOperator, Authentication authentication) {
        if (requestedOperator != null && !requestedOperator.isBlank()) {
            return requestedOperator;
        }
        return currentUser(authentication).username();
    }
}
