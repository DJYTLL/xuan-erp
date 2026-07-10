package com.xuan.erp.iam.interfaces.assembler;

import com.xuan.erp.iam.application.query.IamCurrentMenuNodeView;
import com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView;
import com.xuan.erp.iam.interfaces.dto.IamCurrentMenuNodeResponse;
import com.xuan.erp.iam.interfaces.dto.IamCurrentPermissionSnapshotResponse;
import java.util.List;

/**
 * IAM 当前权限快照装配器，负责把当前菜单树和权限快照查询视图转换成接口响应体。
 */
public final class IamCurrentAuthorizationAssembler {

    private IamCurrentAuthorizationAssembler() {
    }

    public static IamCurrentPermissionSnapshotResponse toResponse(IamCurrentPermissionSnapshotView view) {
        return new IamCurrentPermissionSnapshotResponse(
                toMenuTreeResponse(view.menus()),
                view.routePermissions(),
                view.buttonPermissions(),
                view.columnPermissions(),
                view.fieldPermissions(),
                view.dataScopes(),
                view.stateActionRules(),
                view.authVersion());
    }

    public static List<IamCurrentMenuNodeResponse> toMenuTreeResponse(List<IamCurrentMenuNodeView> menus) {
        return menus.stream()
                .map(IamCurrentAuthorizationAssembler::toMenuNodeResponse)
                .toList();
    }

    private static IamCurrentMenuNodeResponse toMenuNodeResponse(IamCurrentMenuNodeView view) {
        return new IamCurrentMenuNodeResponse(
                view.code(),
                view.title(),
                view.i18nKey(),
                view.path(),
                view.icon(),
                view.permissionCode(),
                view.sortNo(),
                toMenuTreeResponse(view.children()));
    }
}
