package com.xuan.erp.tenant.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RetryTenantProvisionTaskRequest(
        @NotBlank(message = "步骤键不能为空")
        @Schema(description = "需要重试的步骤键", example = "IAM_BOOTSTRAP")
        String stepKey,
        @Schema(description = "操作人", example = "ops")
        String operator,
        @NotBlank(message = "重试原因不能为空")
        @Schema(description = "重试原因", example = "人工补偿后重试")
        String reason
) {
}
