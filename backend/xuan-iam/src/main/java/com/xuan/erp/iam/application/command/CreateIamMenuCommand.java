package com.xuan.erp.iam.application.command;

/**
 * 创建 IAM 菜单命令，承载后台菜单管理页提交的全局菜单定义。
 */
public record CreateIamMenuCommand(
        String code,
        Long parentId,
        String title,
        String i18nKey,
        String path,
        String icon,
        String permissionCode,
        Integer sortNo
) {
}
