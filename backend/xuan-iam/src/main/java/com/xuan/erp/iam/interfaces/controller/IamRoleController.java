package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.service.IamRoleApplicationService;
import com.xuan.erp.iam.domain.model.IamRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
    @PreAuthorize("hasAuthority('iam:view')")
    @GetMapping
    public ApiResponse<List<IamRole>> listRoles(
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId) {
        return ApiResponse.success(roleApplicationService.listRoles(tenantId));
    }
}
