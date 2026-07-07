package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.service.IamUserApplicationService;
import com.xuan.erp.iam.interfaces.assembler.IamUserAssembler;
import com.xuan.erp.iam.interfaces.dto.CreateIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.DisableIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.IamUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public IamUserController(IamUserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @Operation(summary = "查询用户列表", description = "按租户查询未逻辑删除的 IAM 用户列表")
    @PreAuthorize("hasAuthority('iam:view')")
    @GetMapping
    public ApiResponse<List<IamUserResponse>> listUsers(
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId) {
        return ApiResponse.success(userApplicationService.listUsers(tenantId).stream()
                .map(IamUserAssembler::toResponse)
                .toList());
    }

    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询 IAM 用户详情")
    @PreAuthorize("hasAuthority('iam:view')")
    @GetMapping("/{userId}")
    public ApiResponse<IamUserResponse> getUser(
            @Parameter(description = "用户 ID")
            @PathVariable("userId") Long userId) {
        return ApiResponse.success(IamUserAssembler.toResponse(userApplicationService.getUser(userId)));
    }

    @Operation(summary = "创建用户", description = "创建租户内 IAM 用户账号")
    @PreAuthorize("hasAuthority('iam:create')")
    @PostMapping
    public ApiResponse<IamUserResponse> createUser(@RequestBody CreateIamUserRequest request) {
        return ApiResponse.success(IamUserAssembler.toResponse(userApplicationService.createUser(IamUserAssembler.toCommand(request))));
    }

    @Operation(summary = "停用用户", description = "停用指定 IAM 用户并提升权限版本")
    @PreAuthorize("hasAuthority('iam:delete')")
    @PostMapping("/{userId}/disable")
    public ApiResponse<IamUserResponse> disableUser(
            @Parameter(description = "用户 ID")
            @PathVariable("userId") Long userId,
            @RequestBody DisableIamUserRequest request) {
        return ApiResponse.success(IamUserAssembler.toResponse(userApplicationService.disableUser(userId, IamUserAssembler.toCommand(request))));
    }
}
