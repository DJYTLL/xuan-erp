package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建 IAM 菜单请求体，承载后台菜单管理页面提交的菜单定义。
 */
@Schema(description = "创建 IAM 菜单请求体")
public record CreateIamMenuRequest(
        @Schema(description = "菜单编码", example = "iam-role-management")
        String code,
        @Schema(description = "父菜单 ID", example = "12")
        Long parentId,
        @Schema(description = "菜单标题", example = "角色管理")
        String title,
        @Schema(description = "国际化键", example = "menu.iamRoles")
        String i18nKey,
        @Schema(description = "前端路由路径", example = "/system/iam/roles")
        String path,
        @Schema(description = "图标键", example = "ShieldCheck")
        String icon,
        @Schema(description = "进入菜单所需权限编码", example = "iam:view")
        String permissionCode,
        @Schema(description = "排序号", example = "122")
        Integer sortNo
) {
}
