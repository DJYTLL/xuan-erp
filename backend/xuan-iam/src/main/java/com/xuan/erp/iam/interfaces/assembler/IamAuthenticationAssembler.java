package com.xuan.erp.iam.interfaces.assembler;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.command.LoginIamUserCommand;
import com.xuan.erp.iam.application.command.RefreshIamTokenCommand;
import com.xuan.erp.iam.application.command.RevokeIamRefreshTokenCommand;
import com.xuan.erp.iam.application.query.IamLoginView;
import com.xuan.erp.iam.application.query.IamTenantStatusView;
import com.xuan.erp.iam.interfaces.dto.IamCurrentUserResponse;
import com.xuan.erp.iam.interfaces.dto.IamLoginRequest;
import com.xuan.erp.iam.interfaces.dto.IamLoginResponse;
import com.xuan.erp.iam.interfaces.dto.IamRefreshTokenRequest;

/**
 * IAM 认证装配器，负责登录请求、登录响应和当前用户响应之间的转换。
 */
public final class IamAuthenticationAssembler {

    private IamAuthenticationAssembler() {
    }

    public static LoginIamUserCommand toCommand(IamLoginRequest request) {
        return new LoginIamUserCommand(request.tenantId(), request.tenantCode(), request.username(), request.password());
    }

    public static RefreshIamTokenCommand toCommand(IamRefreshTokenRequest request) {
        return new RefreshIamTokenCommand(request.refreshToken());
    }

    public static RevokeIamRefreshTokenCommand toRevokeCommand(IamRefreshTokenRequest request) {
        return new RevokeIamRefreshTokenCommand(request.refreshToken());
    }

    public static IamLoginResponse toResponse(IamLoginView view) {
        return new IamLoginResponse(
                "Bearer",
                view.accessToken(),
                view.accessTokenExpiresAt(),
                view.refreshToken(),
                view.refreshTokenExpiresAt(),
                toCurrentUserResponse(view.currentUser(), view.tenantCode(), view.tenantName()));
    }

    public static IamCurrentUserResponse toCurrentUserResponse(CurrentUser currentUser) {
        return toCurrentUserResponse(currentUser, null, null);
    }

    public static IamCurrentUserResponse toCurrentUserResponse(CurrentUser currentUser, IamTenantStatusView tenantStatus) {
        if (tenantStatus == null) {
            return toCurrentUserResponse(currentUser);
        }
        return toCurrentUserResponse(currentUser, tenantStatus.code(), tenantStatus.name());
    }

    public static IamCurrentUserResponse toCurrentUserResponse(CurrentUser currentUser, String tenantCode, String tenantName) {
        return new IamCurrentUserResponse(
                currentUser.userId(),
                currentUser.tenantId(),
                tenantCode,
                tenantName,
                currentUser.username(),
                currentUser.roles(),
                currentUser.authVersion(),
                currentUser.permissions());
    }
}
