package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 当前菜单树节点响应体，返回前端可见菜单树节点。
 *
 * @param code 菜单编码
 * @param title 菜单标题
 * @param i18nKey 国际化键
 * @param path 前端路由路径
 * @param icon 菜单图标
 * @param permissionCode 进入菜单所需权限编码
 * @param sortNo 菜单排序号
 * @param children 子菜单节点
 */
@Schema(description = "当前菜单树节点响应体")
public record IamCurrentMenuNodeResponse(
        @Schema(description = "菜单编码", example = "product")
        String code,
        @Schema(description = "菜单标题", example = "商品")
        String title,
        @Schema(description = "国际化键", example = "menu.product")
        String i18nKey,
        @Schema(description = "前端路由路径", example = "/product")
        String path,
        @Schema(description = "菜单图标", example = "Package")
        String icon,
        @Schema(description = "进入菜单所需权限编码", example = "product:view")
        String permissionCode,
        @Schema(description = "菜单排序号", example = "20")
        int sortNo,
        @Schema(description = "子菜单节点")
        List<IamCurrentMenuNodeResponse> children
) {
}
