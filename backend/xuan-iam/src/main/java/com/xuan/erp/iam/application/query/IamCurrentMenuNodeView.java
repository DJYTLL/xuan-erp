package com.xuan.erp.iam.application.query;

import java.util.List;

/**
 * IAM 当前菜单树节点查询视图，返回前端可直接渲染的当前用户菜单节点。
 */
public record IamCurrentMenuNodeView(
        String code,
        String title,
        String i18nKey,
        String path,
        String icon,
        String permissionCode,
        int sortNo,
        List<IamCurrentMenuNodeView> children
) {
}
