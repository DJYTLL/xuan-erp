package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.query.TenantProvisionTaskStepView;
import com.xuan.erp.tenant.application.query.TenantProvisionTaskView;
import com.xuan.erp.tenant.application.service.TenantProvisionApplicationService;
import com.xuan.erp.tenant.interfaces.dto.RetryTenantProvisionTaskRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantProvisionTaskResponse;
import com.xuan.erp.tenant.interfaces.dto.TenantProvisionTaskStepResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "租户初始化任务", description = "租户初始化任务查询与人工重试接口")
@RestController
@RequestMapping("/api")
public class TenantProvisionTaskController {

    private final TenantProvisionApplicationService service;

    public TenantProvisionTaskController(TenantProvisionApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "查询租户初始化任务", description = "按租户 ID 查询初始化任务及步骤")
    @PreAuthorize("@xuanPermission.has('tenant-provision:view')")
    @GetMapping("/tenants/{tenantId}/provision-tasks")
    public ApiResponse<List<TenantProvisionTaskResponse>> listTasks(
            @PathVariable("tenantId") @Min(value = 1, message = "租户 ID 必须大于 0") Long tenantId) {
        return ApiResponse.success(service.listTasks(tenantId).stream().map(this::toResponse).toList());
    }

    @Operation(summary = "重试初始化任务步骤", description = "按任务 ID 和步骤键触发人工重试入口")
    @PreAuthorize("@xuanPermission.has('tenant-provision:manage')")
    @PostMapping("/tenant-provision-tasks/{taskId}/retry")
    public ApiResponse<Void> retryTask(
            @PathVariable("taskId") @Min(value = 1, message = "任务 ID 必须大于 0") Long taskId,
            @Valid @RequestBody RetryTenantProvisionTaskRequest request) {
        service.retryTask(taskId, request.stepKey(), request.operator(), request.reason());
        return ApiResponse.success(null);
    }

    private TenantProvisionTaskResponse toResponse(TenantProvisionTaskView view) {
        return new TenantProvisionTaskResponse(
                view.id(),
                view.tenantId(),
                view.taskKey(),
                view.taskType(),
                view.status(),
                view.lastErrorCode(),
                view.lastErrorMessage(),
                view.steps().stream().map(this::toResponse).toList()
        );
    }

    private TenantProvisionTaskStepResponse toResponse(TenantProvisionTaskStepView view) {
        return new TenantProvisionTaskStepResponse(
                view.id(),
                view.provisionTaskId(),
                view.stepKey(),
                view.stepName(),
                view.status(),
                view.sequenceNo(),
                view.lastErrorCode(),
                view.lastErrorMessage()
        );
    }
}
