package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.command.CreateIamMenuCommand;
import com.xuan.erp.iam.application.command.UpdateIamMenuCommand;
import com.xuan.erp.iam.application.service.IamMenuApplicationService;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.interfaces.dto.CreateIamMenuRequest;
import com.xuan.erp.iam.interfaces.dto.UpdateIamMenuRequest;
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

    @Operation(summary = "查询菜单选项", description = "供权限、初始化模板和列权限配置页选择菜单或页面，不开放菜单管理写能力")
    @PreAuthorize("@xuanPermission.hasAny('iam-menu:view', 'iam-column-permission:view', 'iam-role-column-permission:view', 'iam-permission:view', 'iam-init-template:view')")
    @GetMapping("/options")
    public ApiResponse<List<IamMenu>> listMenuOptions() {
        return ApiResponse.success(menuApplicationService.listMenus());
    }

    @Operation(summary = "查询菜单清单", description = "查询 IAM 中维护的全局菜单定义")
    @PreAuthorize("@xuanPermission.has('iam-menu:view')")
    @GetMapping
    public ApiResponse<List<IamMenu>> listMenus() {
        return ApiResponse.success(menuApplicationService.listMenus());
    }

    @Operation(summary = "新增菜单", description = "新增 IAM 全局菜单定义")
    @PreAuthorize("@xuanPermission.has('iam-menu:create')")
    @PostMapping
    public ApiResponse<IamMenu> createMenu(@RequestBody CreateIamMenuRequest request) {
        return ApiResponse.success(menuApplicationService.createMenu(new CreateIamMenuCommand(
                request.code(),
                request.parentId(),
                request.title(),
                request.i18nKey(),
                request.path(),
                request.icon(),
                request.permissionCode(),
                request.sortNo())));
    }

    @Operation(summary = "修改菜单", description = "修改 IAM 全局菜单展示字段和启用状态")
    @PreAuthorize("@xuanPermission.has('iam-menu:update')")
    @PutMapping("/{menuId}")
    public ApiResponse<IamMenu> updateMenu(
            @Parameter(description = "菜单 ID")
            @PathVariable("menuId") Long menuId,
            @RequestBody UpdateIamMenuRequest request) {
        return ApiResponse.success(menuApplicationService.updateMenu(menuId, new UpdateIamMenuCommand(
                request.parentId(),
                request.title(),
                request.i18nKey(),
                request.path(),
                request.icon(),
                request.permissionCode(),
                request.sortNo(),
                request.enabled())));
    }

    @Operation(summary = "启用菜单", description = "启用 IAM 全局菜单")
    @PreAuthorize("@xuanPermission.has('iam-menu:update')")
    @PostMapping("/{menuId}/enable")
    public ApiResponse<IamMenu> enableMenu(
            @Parameter(description = "菜单 ID")
            @PathVariable("menuId") Long menuId) {
        return ApiResponse.success(menuApplicationService.setMenuEnabled(menuId, true));
    }

    @Operation(summary = "停用菜单", description = "停用 IAM 全局菜单")
    @PreAuthorize("@xuanPermission.has('iam-menu:update')")
    @PostMapping("/{menuId}/disable")
    public ApiResponse<IamMenu> disableMenu(
            @Parameter(description = "菜单 ID")
            @PathVariable("menuId") Long menuId) {
        return ApiResponse.success(menuApplicationService.setMenuEnabled(menuId, false));
    }
}
