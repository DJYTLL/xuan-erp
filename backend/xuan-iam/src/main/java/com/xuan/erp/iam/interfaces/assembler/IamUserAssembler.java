package com.xuan.erp.iam.interfaces.assembler;

import com.xuan.erp.iam.application.command.CreateIamUserCommand;
import com.xuan.erp.iam.application.command.DisableIamUserCommand;
import com.xuan.erp.iam.application.query.IamUserDetailView;
import com.xuan.erp.iam.interfaces.dto.CreateIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.DisableIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.IamUserResponse;

/**
 * IAM 用户接口组装器，负责用户请求、命令、视图和响应对象之间的转换。
 */
public final class IamUserAssembler {

    private IamUserAssembler() {
    }

    public static CreateIamUserCommand toCommand(CreateIamUserRequest request) {
        return new CreateIamUserCommand(
                request.tenantId(),
                request.username(),
                request.passwordHash(),
                request.displayName(),
                request.email(),
                request.phone(),
                request.remark());
    }

    public static DisableIamUserCommand toCommand(DisableIamUserRequest request) {
        return new DisableIamUserCommand(request.reason(), request.operator());
    }

    public static IamUserResponse toResponse(IamUserDetailView view) {
        return new IamUserResponse(
                view.id(),
                view.tenantId(),
                view.username(),
                view.displayName(),
                view.email(),
                view.phone(),
                view.enabled(),
                view.accountNonLocked(),
                view.authVersion(),
                view.remark(),
                view.createdAt(),
                view.updatedAt());
    }
}
