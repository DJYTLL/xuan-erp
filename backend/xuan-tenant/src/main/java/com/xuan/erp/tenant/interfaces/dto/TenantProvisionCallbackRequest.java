package com.xuan.erp.tenant.interfaces.dto;

import com.xuan.erp.tenant.application.command.TenantProvisionCallbackCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record TenantProvisionCallbackRequest(
        @NotBlank(message = "回执事件 ID 不能为空")
        @Size(max = 120, message = "回执事件 ID 长度不能超过 120")
        @Schema(description = "回执事件 ID", example = "iam-provision-20260714-001")
        String eventId,
        @NotBlank(message = "回执事件类型不能为空")
        @Size(max = 120, message = "回执事件类型长度不能超过 120")
        @Schema(description = "回执事件类型", example = "TenantIamProvisionStepCompleted")
        String eventType,
        @Schema(description = "租户初始化任务键", example = "tenant:create:acme")
        String taskKey,
        @Schema(description = "初始化幂等键", example = "tenant-create-20260714-001")
        String idempotencyKey,
        @NotBlank(message = "初始化步骤不能为空")
        @Size(max = 80, message = "初始化步骤长度不能超过 80")
        @Schema(description = "初始化步骤，首期固定 IAM_BOOTSTRAP", example = "IAM_BOOTSTRAP")
        String provisionStep,
        @Schema(description = "步骤是否成功", example = "true")
        boolean success,
        @Schema(description = "失败错误码", example = "IAM_BOOTSTRAP_FAILED")
        String errorCode,
        @Schema(description = "失败错误信息", example = "菜单初始化失败")
        String errorMessage,
        @Schema(description = "步骤回执结果载荷")
        Map<String, Object> resultPayload,
        @Schema(description = "回执来源服务或操作人", example = "xuan-iam")
        String operator
) {

    public TenantProvisionCallbackCommand toCommand() {
        return new TenantProvisionCallbackCommand(
                eventId,
                eventType,
                taskKey,
                idempotencyKey,
                provisionStep,
                success,
                errorCode,
                errorMessage,
                resultPayload,
                operator);
    }
}
