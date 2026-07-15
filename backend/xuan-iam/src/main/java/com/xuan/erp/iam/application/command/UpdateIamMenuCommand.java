package com.xuan.erp.iam.application.command;

/**
 * 修改 IAM 菜单命令，保持菜单编码不可变，允许调整展示与权限字段。
 */
public record UpdateIamMenuCommand(
        Long parentId,
        String title,
        String i18nKey,
        String path,
        String icon,
        String permissionCode,
        Integer sortNo,
        Boolean enabled
) {
}
