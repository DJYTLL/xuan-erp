package com.xuan.erp.iam.interfaces.assembler;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.command.LoginIamUserCommand;
import com.xuan.erp.iam.application.query.IamLoginView;
import com.xuan.erp.iam.interfaces.dto.IamCurrentUserResponse;
import com.xuan.erp.iam.interfaces.dto.IamLoginRequest;
import com.xuan.erp.iam.interfaces.dto.IamLoginResponse;

/**
 * IAM 认证装配器，负责登录请求、登录响应和当前用户响应之间的转换。
 */
public final class IamAuthenticationAssembler {

    private IamAuthenticationAssembler() {
    }

    public static LoginIamUserCommand toCommand(IamLoginRequest request) {
        return new LoginIamUserCommand(request.tenantId(), request.username(), request.password());
    }

    public static IamLoginResponse toResponse(IamLoginView view) {
        return new IamLoginResponse("Bearer", view.accessToken(), view.accessTokenExpiresAt(), toCurrentUserResponse(view.currentUser()));
    }

    public static IamCurrentUserResponse toCurrentUserResponse(CurrentUser currentUser) {
        return new IamCurrentUserResponse(
                currentUser.userId(),
                currentUser.tenantId(),
                currentUser.username(),
                currentUser.roles(),
                currentUser.authVersion(),
                currentUser.permissions());
    }
}
