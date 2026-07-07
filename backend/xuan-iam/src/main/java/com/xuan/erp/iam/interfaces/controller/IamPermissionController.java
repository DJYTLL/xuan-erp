package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.service.IamPermissionApplicationService;
import com.xuan.erp.iam.domain.model.IamPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
