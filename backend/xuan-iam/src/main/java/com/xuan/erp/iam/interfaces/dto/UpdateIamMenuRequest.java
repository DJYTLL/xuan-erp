package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改 IAM 菜单请求体，承载菜单展示字段、权限码和启用状态调整。
 */
@Schema(description = "修改 IAM 菜单请求体")
public record UpdateIamMenuRequest(
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
        Integer sortNo,
        @Schema(description = "是否启用", example = "true")
        Boolean enabled
) {
}
