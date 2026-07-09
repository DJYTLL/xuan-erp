package com.xuan.erp.tenant.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RetryTenantOutboxEventRequest(
        @Schema(description = "操作人", example = "ops")
        String operator,
        @NotBlank(message = "回放原因不能为空")
        @Schema(description = "回放原因", example = "人工触发死信回放")
        String reason
) {
}
