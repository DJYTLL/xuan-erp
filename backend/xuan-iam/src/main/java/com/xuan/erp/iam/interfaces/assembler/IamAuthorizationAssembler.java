package com.xuan.erp.iam.interfaces.assembler;

import com.xuan.erp.iam.application.command.RebuildAuthorizationSnapshotCommand;
import com.xuan.erp.iam.application.query.IamAuthorizationSnapshotView;
import com.xuan.erp.iam.interfaces.dto.IamAuthorizationSnapshotResponse;
import com.xuan.erp.iam.interfaces.dto.RebuildAuthorizationSnapshotRequest;

/**
 * IAM 授权接口组装器，负责请求、命令、视图和响应对象之间的转换。
 */
public final class IamAuthorizationAssembler {

    private IamAuthorizationAssembler() {
    }

    public static RebuildAuthorizationSnapshotCommand toCommand(RebuildAuthorizationSnapshotRequest request) {
        return new RebuildAuthorizationSnapshotCommand(
                request.tenantId(),
                request.userId(),
                request.authVersion(),
                request.roleIds(),
                request.permissionCodes(),
                request.menuCodes());
    }

    public static IamAuthorizationSnapshotResponse toResponse(IamAuthorizationSnapshotView view) {
        return new IamAuthorizationSnapshotResponse(
                view.id(),
                view.tenantId(),
                view.userId(),
                view.authVersion(),
                view.roleIds(),
                view.permissionCodes(),
                view.menuCodes(),
                view.columnSettings(),
                view.snapshotHash(),
                view.builtAt());
    }
}
