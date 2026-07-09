package com.xuan.erp.tenant.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @NotBlank(message = "租户名称不能为空")
        @Size(max = 120, message = "租户名称长度不能超过 120")
        @Schema(description = "租户名称", example = "玄云 ERP")
        String name,
        @Schema(description = "联系人姓名", example = "李四")
        String contactName,
        @Schema(description = "联系人电话", example = "13900000000")
        String contactPhone,
        @Schema(description = "备注", example = "升级后的租户")
        String remark,
        @Schema(description = "幂等键，客户端重复提交时用于去重", example = "tenant-update-20260707-001")
        String idempotencyKey
) {
}
