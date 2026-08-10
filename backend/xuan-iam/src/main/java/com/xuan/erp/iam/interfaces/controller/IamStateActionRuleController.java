package com.xuan.erp.iam.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.command.CreateIamResourceActionCommand;
import com.xuan.erp.iam.application.command.CreateIamResourceStateCommand;
import com.xuan.erp.iam.application.command.IamRoleStateActionRuleCommand;
import com.xuan.erp.iam.application.command.SetIamRoleStateActionRulesCommand;
import com.xuan.erp.iam.application.command.UpdateIamResourceActionCommand;
import com.xuan.erp.iam.application.command.UpdateIamResourceStateCommand;
import com.xuan.erp.iam.application.service.IamStateActionRuleApplicationService;
import com.xuan.erp.iam.domain.model.IamResourceAction;
import com.xuan.erp.iam.domain.model.IamResourceState;
import com.xuan.erp.iam.domain.model.IamRoleStateActionRule;
import com.xuan.erp.iam.interfaces.dto.CreateIamResourceActionRequest;
import com.xuan.erp.iam.interfaces.dto.CreateIamResourceStateRequest;
import com.xuan.erp.iam.interfaces.dto.IamResourceActionResponse;
import com.xuan.erp.iam.interfaces.dto.IamResourceStateResponse;
import com.xuan.erp.iam.interfaces.dto.IamRoleStateActionRuleResponse;
import com.xuan.erp.iam.interfaces.dto.SetIamRoleStateActionRulesRequest;
import com.xuan.erp.iam.interfaces.dto.UpdateIamResourceActionRequest;
import com.xuan.erp.iam.interfaces.dto.UpdateIamResourceStateRequest;
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
 * IAM 状态动作权限管理控制器，提供资源状态、资源动作和角色动作矩阵维护接口。
 */
@Tag(name = "IAM 状态动作权限管理", description = "资源状态、资源动作和角色状态动作授权矩阵管理接口")
@RestController
@RequestMapping("/api/iam/state-action")
public class IamStateActionRuleController {

    private final IamStateActionRuleApplicationService applicationService;

    public IamStateActionRuleController(IamStateActionRuleApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "查询资源状态", description = "按租户、资源和启用状态查询可参与状态动作授权的资源状态")
    @PreAuthorize("@xuanPermission.has('iam-state-action:view')")
    @GetMapping("/resources/states")
    public ApiResponse<List<IamResourceStateResponse>> listStates(
            @Parameter(description = "租户 ID；为空时查询平台通用和所有租户状态")
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @Parameter(description = "资源标识")
            @RequestParam(value = "resourceKey", required = false) String resourceKey,
            @Parameter(description = "是否启用")
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return ApiResponse.success(applicationService.listStates(tenantId, resourceKey, enabled).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "新增资源状态", description = "新增一个可参与状态动作授权的资源状态")
    @PreAuthorize("@xuanPermission.has('iam-state-action:create')")
    @PostMapping("/resources/states")
    public ApiResponse<IamResourceStateResponse> createState(@RequestBody CreateIamResourceStateRequest request) {
        return ApiResponse.success(toResponse(applicationService.createState(new CreateIamResourceStateCommand(
                request.tenantId(),
                request.resourceKey(),
                request.stateCode(),
                request.stateName(),
                request.description(),
                request.sortNo(),
                request.enabled(),
                request.metadataJson(),
                request.operator()))));
    }

    @Operation(summary = "修改资源状态", description = "修改资源状态名称、说明、排序、启用状态和扩展元数据")
    @PreAuthorize("@xuanPermission.has('iam-state-action:update')")
    @PutMapping("/resources/states/{stateId}")
    public ApiResponse<IamResourceStateResponse> updateState(
            @Parameter(description = "状态 ID")
            @PathVariable("stateId") Long stateId,
            @RequestBody UpdateIamResourceStateRequest request) {
        return ApiResponse.success(toResponse(applicationService.updateState(stateId, new UpdateIamResourceStateCommand(
                request.stateName(),
                request.description(),
                request.sortNo(),
                request.enabled(),
                request.metadataJson(),
                request.operator()))));
    }

    @Operation(summary = "启用资源状态", description = "启用指定资源状态")
    @PreAuthorize("@xuanPermission.has('iam-state-action:update')")
    @PostMapping("/resources/states/{stateId}/enable")
    public ApiResponse<IamResourceStateResponse> enableState(
            @Parameter(description = "状态 ID")
            @PathVariable("stateId") Long stateId,
            @Parameter(description = "操作人")
            @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(toResponse(applicationService.setStateEnabled(stateId, true, operator)));
    }

    @Operation(summary = "停用资源状态", description = "停用指定资源状态")
    @PreAuthorize("@xuanPermission.has('iam-state-action:update')")
    @PostMapping("/resources/states/{stateId}/disable")
    public ApiResponse<IamResourceStateResponse> disableState(
            @Parameter(description = "状态 ID")
            @PathVariable("stateId") Long stateId,
            @Parameter(description = "操作人")
            @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(toResponse(applicationService.setStateEnabled(stateId, false, operator)));
    }

    @Operation(summary = "查询资源动作", description = "按租户、资源和启用状态查询可参与状态动作授权的资源动作")
    @PreAuthorize("@xuanPermission.has('iam-state-action:view')")
    @GetMapping("/resources/actions")
    public ApiResponse<List<IamResourceActionResponse>> listActions(
            @Parameter(description = "租户 ID；为空时查询平台通用和所有租户动作")
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @Parameter(description = "资源标识")
            @RequestParam(value = "resourceKey", required = false) String resourceKey,
            @Parameter(description = "是否启用")
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return ApiResponse.success(applicationService.listActions(tenantId, resourceKey, enabled).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "新增资源动作", description = "新增一个可参与状态动作授权的资源动作")
    @PreAuthorize("@xuanPermission.has('iam-state-action:create')")
    @PostMapping("/resources/actions")
    public ApiResponse<IamResourceActionResponse> createAction(@RequestBody CreateIamResourceActionRequest request) {
        return ApiResponse.success(toResponse(applicationService.createAction(new CreateIamResourceActionCommand(
                request.tenantId(),
                request.resourceKey(),
                request.actionCode(),
                request.actionName(),
                request.permissionCode(),
                request.description(),
                request.sortNo(),
                request.enabled(),
                request.metadataJson(),
                request.operator()))));
    }

    @Operation(summary = "修改资源动作", description = "修改资源动作名称、基础权限码、说明、排序、启用状态和扩展元数据")
    @PreAuthorize("@xuanPermission.has('iam-state-action:update')")
    @PutMapping("/resources/actions/{actionId}")
    public ApiResponse<IamResourceActionResponse> updateAction(
            @Parameter(description = "动作 ID")
            @PathVariable("actionId") Long actionId,
            @RequestBody UpdateIamResourceActionRequest request) {
        return ApiResponse.success(toResponse(applicationService.updateAction(actionId, new UpdateIamResourceActionCommand(
                request.actionName(),
                request.permissionCode(),
                request.description(),
                request.sortNo(),
                request.enabled(),
                request.metadataJson(),
                request.operator()))));
    }

    @Operation(summary = "启用资源动作", description = "启用指定资源动作")
    @PreAuthorize("@xuanPermission.has('iam-state-action:update')")
    @PostMapping("/resources/actions/{actionId}/enable")
    public ApiResponse<IamResourceActionResponse> enableAction(
            @Parameter(description = "动作 ID")
            @PathVariable("actionId") Long actionId,
            @Parameter(description = "操作人")
            @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(toResponse(applicationService.setActionEnabled(actionId, true, operator)));
    }

    @Operation(summary = "停用资源动作", description = "停用指定资源动作")
    @PreAuthorize("@xuanPermission.has('iam-state-action:update')")
    @PostMapping("/resources/actions/{actionId}/disable")
    public ApiResponse<IamResourceActionResponse> disableAction(
            @Parameter(description = "动作 ID")
            @PathVariable("actionId") Long actionId,
            @Parameter(description = "操作人")
            @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(toResponse(applicationService.setActionEnabled(actionId, false, operator)));
    }

    @Operation(summary = "查询角色状态动作规则", description = "查询指定角色当前配置的状态动作授权矩阵")
    @PreAuthorize("@xuanPermission.has('iam-state-action:view')")
    @GetMapping("/roles/{roleId}/rules")
    public ApiResponse<List<IamRoleStateActionRuleResponse>> listRoleRules(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "租户 ID")
            @RequestParam("tenantId") Long tenantId) {
        return ApiResponse.success(applicationService.listRoleStateActionRules(tenantId, roleId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "保存角色状态动作规则", description = "使用规则集合替换指定角色当前状态动作授权矩阵")
    @PreAuthorize("@xuanPermission.has('iam-state-action:update')")
    @PutMapping("/roles/{roleId}/rules")
    public ApiResponse<List<IamRoleStateActionRuleResponse>> setRoleRules(
            @Parameter(description = "角色 ID")
            @PathVariable("roleId") Long roleId,
            @RequestBody SetIamRoleStateActionRulesRequest request) {
        return ApiResponse.success(applicationService.replaceRoleStateActionRules(new SetIamRoleStateActionRulesCommand(
                        request.tenantId(),
                        roleId,
                        request.rules() == null ? List.of() : request.rules().stream()
                                .map(rule -> new IamRoleStateActionRuleCommand(
                                        rule.resourceKey(),
                                        rule.stateCode(),
                                        rule.actionCode()))
                                .toList(),
                        request.operator()))
                .stream()
                .map(this::toResponse)
                .toList());
    }

    private IamResourceStateResponse toResponse(IamResourceState state) {
        return new IamResourceStateResponse(
                state.id(),
                state.tenantId(),
                state.resourceKey(),
                state.stateCode(),
                state.stateName(),
                state.description(),
                state.sortNo(),
                state.enabled(),
                state.metadataJson());
    }

    private IamResourceActionResponse toResponse(IamResourceAction action) {
        return new IamResourceActionResponse(
                action.id(),
                action.tenantId(),
                action.resourceKey(),
                action.actionCode(),
                action.actionName(),
                action.permissionCode(),
                action.description(),
                action.sortNo(),
                action.enabled(),
                action.metadataJson());
    }

    private IamRoleStateActionRuleResponse toResponse(IamRoleStateActionRule rule) {
        return new IamRoleStateActionRuleResponse(
                rule.id(),
                rule.tenantId(),
                rule.roleId(),
                rule.resourceKey(),
                rule.stateCode(),
                rule.actionCode(),
                rule.enabled());
    }
}
