package com.xuan.erp.iam.application.query;

import java.util.List;
import java.util.Map;

/**
 * IAM 当前权限快照查询视图，返回当前用户菜单树和前端权限控制所需的最小结构。
 */
public record IamCurrentPermissionSnapshotView(
        List<IamCurrentMenuNodeView> menus,
        List<String> routePermissions,
        List<String> buttonPermissions,
        Map<String, List<String>> columnPermissions,
        Map<String, List<String>> fieldPermissions,
        List<String> dataScopes,
        Map<String, List<String>> stateActionRules,
        Long authVersion
) {
}
