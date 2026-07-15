package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.service.IamUserPreferenceApplicationService;
import com.xuan.erp.iam.interfaces.assembler.IamUserPreferenceAssembler;
import com.xuan.erp.iam.interfaces.dto.IamUserPreferenceResponse;
import com.xuan.erp.iam.interfaces.dto.SaveIamUserPreferenceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 用户偏好接口控制器，负责当前登录用户的前端布局、标签页和业务页面偏好。
 */
@Tag(name = "IAM 用户偏好", description = "当前登录用户的通用偏好配置接口")
@RestController
@RequestMapping("/api/iam/user-preferences")
public class IamUserPreferenceController {

    private final IamUserPreferenceApplicationService preferenceApplicationService;

    public IamUserPreferenceController(IamUserPreferenceApplicationService preferenceApplicationService) {
        this.preferenceApplicationService = preferenceApplicationService;
    }

    @Operation(summary = "查询用户偏好", description = "按偏好键查询当前登录用户偏好")
    @GetMapping("/{preferenceKey}")
    public ApiResponse<IamUserPreferenceResponse> getPreference(
            @Parameter(description = "偏好键")
            @PathVariable("preferenceKey") String preferenceKey,
            Authentication authentication) {
        CurrentUser currentUser = requireCurrentUser(authentication);
        return ApiResponse.success(IamUserPreferenceAssembler.toResponse(
                preferenceApplicationService.getPreference(currentUser, preferenceKey)));
    }

    @Operation(summary = "保存用户偏好", description = "按偏好键保存当前登录用户偏好")
    @PutMapping("/{preferenceKey}")
    public ApiResponse<IamUserPreferenceResponse> savePreference(
            @Parameter(description = "偏好键")
            @PathVariable("preferenceKey") String preferenceKey,
            @RequestBody SaveIamUserPreferenceRequest request,
            Authentication authentication) {
        CurrentUser currentUser = requireCurrentUser(authentication);
        return ApiResponse.success(IamUserPreferenceAssembler.toResponse(
                preferenceApplicationService.savePreference(currentUser, preferenceKey, request.value())));
    }

    private CurrentUser requireCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException("IAM_UNAUTHORIZED", "当前请求未包含 IAM 登录上下文");
        }
        return currentUser;
    }
}
