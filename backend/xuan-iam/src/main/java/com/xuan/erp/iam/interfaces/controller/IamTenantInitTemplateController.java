package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.command.CreateIamTenantInitTemplateCommand;
import com.xuan.erp.iam.application.command.SetIamTenantInitTemplatePermissionsCommand;
import com.xuan.erp.iam.application.command.UpdateIamTenantInitTemplateCommand;
import com.xuan.erp.iam.application.service.IamTenantInitTemplateApplicationService;
import com.xuan.erp.iam.domain.model.IamTenantInitPermissionTemplate;
import com.xuan.erp.iam.interfaces.dto.CreateIamTenantInitTemplateRequest;
import com.xuan.erp.iam.interfaces.dto.IamTenantInitTemplateResponse;
import com.xuan.erp.iam.interfaces.dto.SetIamTenantInitTemplatePermissionsRequest;
import com.xuan.erp.iam.interfaces.dto.UpdateIamTenantInitTemplateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 租户初始化模板控制器，提供模板列表、资料维护和权限集合保存接口。
 */
@Tag(name = "IAM 租户初始化模板", description = "租户初始化权限模板管理接口")
@RestController
@RequestMapping("/api/iam/tenant-init-templates")
public class IamTenantInitTemplateController {

    private final IamTenantInitTemplateApplicationService applicationService;

    public IamTenantInitTemplateController(IamTenantInitTemplateApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "查询租户初始化模板", description = "查询平台维护的租户初始化权限模板列表")
    @PreAuthorize("hasAuthority('iam-init-template:view')")
    @GetMapping
    public ApiResponse<List<IamTenantInitTemplateResponse>> listTemplates() {
        return ApiResponse.success(applicationService.listTemplates().stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "新增租户初始化模板", description = "新增一套租户初始化权限模板")
    @PreAuthorize("hasAuthority('iam-init-template:create')")
    @PostMapping
    public ApiResponse<IamTenantInitTemplateResponse> createTemplate(@RequestBody CreateIamTenantInitTemplateRequest request) {
        return ApiResponse.success(toResponse(applicationService.createTemplate(new CreateIamTenantInitTemplateCommand(
                request.code(),
                request.name(),
                request.description(),
                request.enabled(),
                request.defaultTemplate(),
                request.permissionCodes()))));
    }

    @Operation(summary = "修改租户初始化模板", description = "修改模板名称、说明、启用状态和默认标识")
    @PreAuthorize("hasAuthority('iam-init-template:update')")
    @PutMapping("/{templateId}")
    public ApiResponse<IamTenantInitTemplateResponse> updateTemplate(
            @Parameter(description = "模板 ID")
            @PathVariable("templateId") Long templateId,
            @RequestBody UpdateIamTenantInitTemplateRequest request) {
        return ApiResponse.success(toResponse(applicationService.updateTemplate(templateId, new UpdateIamTenantInitTemplateCommand(
                request.name(),
                request.description(),
                request.enabled(),
                request.defaultTemplate()))));
    }

    @Operation(summary = "保存租户初始化模板权限", description = "使用权限编码集合替换指定模板的初始化权限")
    @PreAuthorize("hasAuthority('iam-init-template:update')")
    @PutMapping("/{templateId}/permissions")
    public ApiResponse<IamTenantInitTemplateResponse> setTemplatePermissions(
            @Parameter(description = "模板 ID")
            @PathVariable("templateId") Long templateId,
            @RequestBody SetIamTenantInitTemplatePermissionsRequest request) {
        return ApiResponse.success(toResponse(applicationService.replaceTemplatePermissions(new SetIamTenantInitTemplatePermissionsCommand(
                templateId,
                request.permissionCodes(),
                request.operator()))));
    }

    private IamTenantInitTemplateResponse toResponse(IamTenantInitPermissionTemplate template) {
        return new IamTenantInitTemplateResponse(
                template.id(),
                template.code(),
                template.name(),
                template.description(),
                template.permissionCodes(),
                template.defaultTemplate(),
                template.enabled());
    }
}
