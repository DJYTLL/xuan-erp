package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.service.IamMenuApplicationService;
import com.xuan.erp.iam.domain.model.IamMenu;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 菜单目录接口控制器，负责全局菜单清单查询入口。
 */
@Tag(name = "IAM 菜单目录", description = "全局菜单清单查询接口")
@RestController
@RequestMapping("/api/iam/menus")
public class IamMenuController {

    private final IamMenuApplicationService menuApplicationService;

    public IamMenuController(IamMenuApplicationService menuApplicationService) {
        this.menuApplicationService = menuApplicationService;
    }

    @Operation(summary = "查询菜单清单", description = "查询 IAM 中维护的全局菜单定义")
    @PreAuthorize("hasAuthority('iam:view')")
    @GetMapping
    public ApiResponse<List<IamMenu>> listMenus() {
        return ApiResponse.success(menuApplicationService.listMenus());
    }
}
