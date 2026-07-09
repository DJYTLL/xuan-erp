package com.xuan.erp.tenant.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeleteRequest(
        @NotBlank(message = "删除原因不能为空")
        @Schema(description = "删除原因", example = "测试数据清理")
        String reason,
        @Schema(description = "操作人", example = "admin")
        String operator,
        @Schema(description = "幂等键，客户端重复提交时用于去重", example = "tenant-delete-20260707-001")
        String idempotencyKey
) {
}
