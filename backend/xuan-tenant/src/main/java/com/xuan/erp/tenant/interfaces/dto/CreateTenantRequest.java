package com.xuan.erp.tenant.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTenantRequest(
        @Schema(description = "租户编码", example = "default")
        String code,
        @Schema(description = "租户名称", example = "默认租户")
        String name,
        @Schema(description = "联系人姓名", example = "系统管理员")
        String contactName,
        @Schema(description = "联系人电话", example = "13800000000")
        String contactPhone,
        @Schema(description = "备注", example = "本地联调租户")
        String remark
) {
}
