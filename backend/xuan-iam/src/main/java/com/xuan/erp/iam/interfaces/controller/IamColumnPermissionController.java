package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.command.CreateIamColumnPermissionTemplateCommand;
import com.xuan.erp.iam.application.command.IamColumnPermissionTemplateItemCommand;
import com.xuan.erp.iam.application.command.IamRoleColumnPermissionRuleCommand;
import com.xuan.erp.iam.application.command.SetIamColumnPermissionTemplateItemsCommand;
import com.xuan.erp.iam.application.command.SetIamRoleColumnPermissionsCommand;
import com.xuan.erp.iam.application.command.SetIamRoleColumnPermissionTemplateCommand;
import com.xuan.erp.iam.application.command.SetIamTenantColumnPermissionTemplatesCommand;
import com.xuan.erp.iam.application.command.UpdateIamColumnPermissionTemplateCommand;
import com.xuan.erp.iam.application.service.IamColumnPermissionApplicationService;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplate;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplateItem;
import com.xuan.erp.iam.domain.model.IamResourceColumn;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionRule;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionTemplateBinding;
import com.xuan.erp.iam.domain.model.IamTenantColumnPermissionTemplateAssignment;
import com.xuan.erp.iam.interfaces.dto.CreateIamColumnPermissionTemplateRequest;
import com.xuan.erp.iam.interfaces.dto.IamColumnPermissionTemplateItemResponse;
import com.xuan.erp.iam.interfaces.dto.IamColumnPermissionTemplateResponse;
import com.xuan.erp.iam.interfaces.dto.IamResourceColumnResponse;
import com.xuan.erp.iam.interfaces.dto.IamRoleColumnPermissionRuleResponse;
import com.xuan.erp.iam.interfaces.dto.IamRoleColumnPermissionTemplateBindingResponse;
import com.xuan.erp.iam.interfaces.dto.IamTenantColumnPermissionTemplateAssignmentResponse;
import com.xuan.erp.iam.interfaces.dto.SetIamColumnPermissionTemplateItemsRequest;
import com.xuan.erp.iam.interfaces.dto.SetIamRoleColumnPermissionsRequest;
import com.xuan.erp.iam.interfaces.dto.SetIamRoleColumnPermissionTemplateRequest;
import com.xuan.erp.iam.interfaces.dto.SetIamTenantColumnPermissionTemplatesRequest;
import com.xuan.erp.iam.interfaces.dto.UpdateIamColumnPermissionTemplateRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 列权限管理控制器，提供资源字段、列权限模板和角色模板绑定接口。
 */
@Tag(name = "IAM 列权限管理", description = "列权限模板、字段规则和角色模板绑定管理接口")
@RestController
@RequestMapping("/api/iam/column-permissions")
public class IamColumnPermissionController {

    private final IamColumnPermissionApplicationService applicationService;

    public IamColumnPermissionController(IamColumnPermissionApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "查询资源字段", description = "查询可参与列权限控制的资源字段清单")
    @PreAuthorize("@xuanPermission.hasAny('iam-column-permission:view', 'iam-role-column-permission:view')")
    @GetMapping("/resources")
    public ApiResponse<List<IamResourceColumnResponse>> listResourceColumns() {
        return ApiResponse.success(applicationService.listResourceColumns().stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "查询列权限模板", description = "按租户、关键字和启用状态查询列权限模板")
    @PreAuthorize("@xuanPermission.has('iam-column-permission:view')")
    @GetMapping("/templates")
    public ApiResponse<List<IamColumnPermissionTemplateResponse>> listTemplates(
            @Parameter(description = "租户 ID；为空时查询平台默认模板和所有租户模板")
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @Parameter(description = "模板编码或名称关键字")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "是否启用")
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return ApiResponse.success(applicationService.listTemplates(tenantId, keyword, enabled).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "新增列权限模板", description = "新增一套列权限模板")
    @PreAuthorize("@xuanPermission.has('iam-column-permission:create')")
    @PostMapping("/templates")
    public ApiResponse<IamColumnPermissionTemplateResponse> createTemplate(
            @RequestBody CreateIamColumnPermissionTemplateRequest request) {
        return ApiResponse.success(toResponse(applicationService.createTemplate(new CreateIamColumnPermissionTemplateCommand(
                request.tenantId(),
                request.code(),
                request.name(),
                request.description(),
                request.enabled(),
                request.operator()))));
    }

    @Operation(summary = "修改列权限模板", description = "修改列权限模板基础信息")
    @PreAuthorize("@xuanPermission.has('iam-column-permission:update')")
    @PutMapping("/templates/{templateId}")
    public ApiResponse<IamColumnPermissionTemplateResponse> updateTemplate(
            @Parameter(description = "模板 ID")
            @PathVariable("templateId") Long templateId,
            @RequestBody UpdateIamColumnPermissionTemplateRequest request) {
        return ApiResponse.success(toResponse(applicationService.updateTemplate(templateId, new UpdateIamColumnPermissionTemplateCommand(
                request.name(),
                request.description(),
                request.enabled(),
                request.operator()))));
    }

    @Operation(summary = "启用列权限模板", description = "启用指定列权限模板")
    @PreAuthorize("@xuanPermission.has('iam-column-permission:update')")
    @PostMapping("/templates/{templateId}/enable")
    public ApiResponse<IamColumnPermissionTemplateResponse> enableTemplate(
            @Parameter(description = "模板 ID")
            @PathVariable("templateId") Long templateId,
            @Parameter(description = "操作人")
            @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(toResponse(applicationService.setTemplateEnabled(templateId, true, operator)));
    }

    @Operation(summary = "停用列权限模板", description = "停用指定列权限模板")
    @PreAuthorize("@xuanPermission.has('iam-column-permission:update')")
    @PostMapping("/templates/{templateId}/disable")
    public ApiResponse<IamColumnPermissionTemplateResponse> disableTemplate(
            @Parameter(description = "模板 ID")
            @PathVariable("templateId") Long templateId,
            @Parameter(description = "操作人")
            @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(toResponse(applicationService.setTemplateEnabled(templateId, false, operator)));
    }

    @Operation(summary = "查询列权限模板规则", description = "查询指定模板下每个资源字段的访问级别")
    @PreAuthorize("@xuanPermission.hasAny('iam-column-permission:view', 'iam-role-column-permission:view')")
    @GetMapping("/templates/{templateId}/items")
    public ApiResponse<List<IamColumnPermissionTemplateItemResponse>> listTemplateItems(
            @Parameter(description = "模板 ID")
            @PathVariable("templateId") Long templateId) {
        return ApiResponse.success(applicationService.listTemplateItems(templateId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "保存列权限模板规则", description = "使用字段规则集合替换指定模板的列权限明细")
    @PreAuthorize("@xuanPermission.has('iam-column-permission:update')")
    @PutMapping("/templates/{templateId}/items")
    public ApiResponse<List<IamColumnPermissionTemplateItemResponse>> setTemplateItems(
            @Parameter(description = "模板 ID")
            @PathVariable("templateId") Long templateId,
            @RequestBody SetIamColumnPermissionTemplateItemsRequest request) {
        return ApiResponse.success(applicationService.replaceTemplateItems(new SetIamColumnPermissionTemplateItemsCommand(
                        templateId,
                        request.items() == null ? List.of() : request.items().stream()
                                .map(item -> new IamColumnPermissionTemplateItemCommand(
                                        item.resourceColumnId(),
                                        item.accessMode()))
                                .toList(),
                        request.operator()))
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "查询租户可用列权限模板", description = "查询指定租户已分配、可供角色绑定的列权限模板")
    @PreAuthorize("@xuanPermission.hasAny('iam-column-permission:view', 'iam-role-column-permission:view')")
    @GetMapping("/tenants/{tenantId}/templates")
    public ApiResponse<List<IamTenantColumnPermissionTemplateAssignmentResponse>> listTenantTemplateAssignments(
            @Parameter(description = "租户 ID")
            @PathVariable("tenantId") Long tenantId) {
        return ApiResponse.success(applicationService.listTenantTemplateAssignments(tenantId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "保存租户可用列权限模板", description = "用平台列权限模板集合替换指定租户的可用模板池")
    @PreAuthorize("@xuanPermission.has('iam-column-permission:update')")
    @PutMapping("/tenants/{tenantId}/templates")
    public ApiResponse<List<IamTenantColumnPermissionTemplateAssignmentResponse>> setTenantTemplateAssignments(
            @Parameter(description = "租户 ID")
            @PathVariable("tenantId") Long tenantId,
            @RequestBody SetIamTenantColumnPermissionTemplatesRequest request) {
        return ApiResponse.success(applicationService.replaceTenantTemplateAssignments(new SetIamTenantColumnPermissionTemplatesCommand(
                        tenantId,
                        request.templateIds(),
                        request.defaultTemplateId(),
                        request.operator()))
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "查询角色列权限规则", description = "查询指定角色独立配置的列权限字段规则")
    @PreAuthorize("@xuanPermission.has('iam-role-column-permission:view')")
    @GetMapping("/roles/{roleId}/column-permissions")
    public ApiResponse<List<IamRoleColumnPermissionRuleResponse>> listRoleColumnPermissions(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId) {
        return ApiResponse.success(applicationService.listRoleColumnPermissionRules(tenantId, roleId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "保存角色列权限规则", description = "使用字段规则集合替换指定角色的独立列权限规则")
    @PreAuthorize("@xuanPermission.has('iam-role-column-permission:update')")
    @PutMapping("/roles/{roleId}/column-permissions")
    public ApiResponse<List<IamRoleColumnPermissionRuleResponse>> setRoleColumnPermissions(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @RequestBody SetIamRoleColumnPermissionsRequest request) {
        return ApiResponse.success(applicationService.replaceRoleColumnPermissionRules(new SetIamRoleColumnPermissionsCommand(
                        request.tenantId(),
                        roleId,
                        request.rules() == null ? List.of() : request.rules().stream()
                                .map(rule -> new IamRoleColumnPermissionRuleCommand(
                                        rule.resourceColumnId(),
                                        rule.accessMode()))
                                .toList(),
                        request.operator()))
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "查询角色列权限模板绑定", description = "查询指定角色当前绑定的列权限模板")
    @PreAuthorize("@xuanPermission.hasAny('iam-column-permission:view', 'iam-role-column-permission:view')")
    @GetMapping("/roles/{roleId}/column-permission-template")
    public ApiResponse<IamRoleColumnPermissionTemplateBindingResponse> getRoleTemplateBinding(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId) {
        return ApiResponse.success(toResponse(applicationService.getRoleTemplateBinding(tenantId, roleId)));
    }

    @Operation(summary = "保存角色列权限模板绑定", description = "使用模板替换指定角色当前列权限模板绑定")
    @PreAuthorize("@xuanPermission.has('iam-column-permission:update')")
    @PutMapping("/roles/{roleId}/column-permission-template")
    public ApiResponse<IamRoleColumnPermissionTemplateBindingResponse> setRoleTemplateBinding(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @RequestBody SetIamRoleColumnPermissionTemplateRequest request) {
        return ApiResponse.success(toResponse(applicationService.setRoleTemplateBinding(new SetIamRoleColumnPermissionTemplateCommand(
                request.tenantId(),
                roleId,
                request.templateId(),
                request.operator()))));
    }

    private IamResourceColumnResponse toResponse(IamResourceColumn column) {
        return new IamResourceColumnResponse(
                column.id(),
                column.resourceKey(),
                column.columnKey(),
                column.columnName(),
                column.dataType(),
                column.maskType(),
                column.enabled(),
                column.sortNo());
    }

    private IamColumnPermissionTemplateResponse toResponse(IamColumnPermissionTemplate template) {
        return new IamColumnPermissionTemplateResponse(
                template.id(),
                template.tenantId(),
                template.code(),
                template.name(),
                template.description(),
                template.enabled());
    }

    private IamColumnPermissionTemplateItemResponse toResponse(IamColumnPermissionTemplateItem item) {
        return new IamColumnPermissionTemplateItemResponse(
                item.id(),
                item.templateId(),
                item.resourceColumnId(),
                item.resourceKey(),
                item.columnKey(),
                item.columnName(),
                item.accessMode());
    }

    private IamTenantColumnPermissionTemplateAssignmentResponse toResponse(IamTenantColumnPermissionTemplateAssignment assignment) {
        return new IamTenantColumnPermissionTemplateAssignmentResponse(
                assignment.tenantId(),
                assignment.templateId(),
                assignment.templateCode(),
                assignment.templateName(),
                assignment.templateDescription(),
                assignment.defaultTemplate(),
                assignment.templateEnabled());
    }

    private IamRoleColumnPermissionRuleResponse toResponse(IamRoleColumnPermissionRule rule) {
        return new IamRoleColumnPermissionRuleResponse(
                rule.id(),
                rule.tenantId(),
                rule.roleId(),
                rule.resourceColumnId(),
                rule.resourceKey(),
                rule.columnKey(),
                rule.columnName(),
                rule.accessMode());
    }

    private IamRoleColumnPermissionTemplateBindingResponse toResponse(IamRoleColumnPermissionTemplateBinding binding) {
        if (binding == null) {
            return null;
        }
        return new IamRoleColumnPermissionTemplateBindingResponse(
                binding.tenantId(),
                binding.roleId(),
                binding.templateId(),
                binding.templateCode(),
                binding.templateName());
    }
}
