package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.port.IamAccessTokenIssuer;
import com.xuan.erp.iam.application.service.IamAuthenticationApplicationService;
import com.xuan.erp.iam.application.service.IamCurrentAuthorizationApplicationService;
import com.xuan.erp.iam.interfaces.assembler.IamAuthenticationAssembler;
import com.xuan.erp.iam.interfaces.assembler.IamCurrentAuthorizationAssembler;
import com.xuan.erp.iam.interfaces.dto.IamCurrentMenuNodeResponse;
import com.xuan.erp.iam.interfaces.dto.IamCurrentPermissionSnapshotResponse;
import com.xuan.erp.iam.interfaces.dto.IamCurrentUserResponse;
import com.xuan.erp.iam.interfaces.dto.IamLoginRequest;
import com.xuan.erp.iam.interfaces.dto.IamLoginResponse;
import com.xuan.erp.iam.interfaces.dto.IamRefreshTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 认证接口控制器，提供登录、公钥发布和当前用户查询接口。
 */
@Tag(name = "IAM 认证", description = "登录、公钥发布和当前用户接口")
@RestController
public class IamAuthenticationController {

    private final IamAuthenticationApplicationService authenticationApplicationService;
    private final IamCurrentAuthorizationApplicationService currentAuthorizationApplicationService;
    private final IamAccessTokenIssuer accessTokenIssuer;

    public IamAuthenticationController(
            IamAuthenticationApplicationService authenticationApplicationService,
            IamCurrentAuthorizationApplicationService currentAuthorizationApplicationService,
            IamAccessTokenIssuer accessTokenIssuer) {
        this.authenticationApplicationService = authenticationApplicationService;
        this.currentAuthorizationApplicationService = currentAuthorizationApplicationService;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    @Operation(summary = "登录", description = "按租户、用户名和密码完成登录，并返回 Bearer 访问令牌")
    @PostMapping("/api/iam/auth/login")
    public ApiResponse<IamLoginResponse> login(@RequestBody IamLoginRequest request) {
        return ApiResponse.success(IamAuthenticationAssembler.toResponse(
                authenticationApplicationService.login(IamAuthenticationAssembler.toCommand(request))));
    }

    @Operation(summary = "刷新访问令牌", description = "使用 refresh token 轮换刷新令牌，并返回新的 Bearer 访问令牌")
    @PostMapping("/api/iam/auth/refresh")
    public ApiResponse<IamLoginResponse> refresh(@RequestBody IamRefreshTokenRequest request) {
        return ApiResponse.success(IamAuthenticationAssembler.toResponse(
                authenticationApplicationService.refresh(IamAuthenticationAssembler.toCommand(request))));
    }

    @Operation(summary = "退出登录", description = "撤销 refresh token，阻止后续续期；access token 等待自然过期")
    @PostMapping("/api/iam/auth/logout")
    public ApiResponse<Void> logout(@RequestBody IamRefreshTokenRequest request) {
        authenticationApplicationService.logout(IamAuthenticationAssembler.toRevokeCommand(request));
        return ApiResponse.success(null);
    }

    @Operation(summary = "查询当前用户", description = "返回当前 Bearer Token 解析出的轻量用户上下文")
    @GetMapping("/api/iam/auth/current-user")
    public ApiResponse<IamCurrentUserResponse> currentUser(Authentication authentication) {
        CurrentUser currentUser = requireCurrentUser(authentication);
        authenticationApplicationService.validateCurrentTenantStatus(currentUser);
        return ApiResponse.success(IamAuthenticationAssembler.toCurrentUserResponse(
                currentUser,
                authenticationApplicationService.resolveCurrentTenantDisplay(currentUser)));
    }

    @Operation(summary = "查询当前用户菜单树", description = "返回当前登录用户可见菜单树")
    @GetMapping("/api/iam/menus/current")
    public ApiResponse<List<IamCurrentMenuNodeResponse>> currentMenus(Authentication authentication) {
        CurrentUser currentUser = requireCurrentUser(authentication);
        authenticationApplicationService.validateCurrentTenantStatus(currentUser);
        return ApiResponse.success(IamCurrentAuthorizationAssembler.toMenuTreeResponse(
                currentAuthorizationApplicationService.getCurrentPermissionSnapshot(currentUser).menus()));
    }

    @Operation(summary = "查询当前用户权限快照", description = "返回当前登录用户的菜单树、路由权限、按钮权限和权限版本")
    @GetMapping("/api/iam/permissions/current")
    public ApiResponse<IamCurrentPermissionSnapshotResponse> currentPermissions(Authentication authentication) {
        CurrentUser currentUser = requireCurrentUser(authentication);
        authenticationApplicationService.validateCurrentTenantStatus(currentUser);
        return ApiResponse.success(IamCurrentAuthorizationAssembler.toResponse(
                currentAuthorizationApplicationService.getCurrentPermissionSnapshot(currentUser)));
    }

    @Operation(summary = "查询 JWK 公钥集", description = "返回 IAM 对外发布的 RSA 公钥 JWK Set")
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return accessTokenIssuer.publicJwkSet();
    }

    private CurrentUser requireCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException("IAM_UNAUTHORIZED", "当前请求未包含 IAM 登录上下文");
        }
        return currentUser;
    }
}
