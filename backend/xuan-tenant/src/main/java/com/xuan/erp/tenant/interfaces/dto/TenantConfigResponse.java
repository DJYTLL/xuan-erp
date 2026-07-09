package com.xuan.erp.tenant.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TenantConfigResponse(
        @Schema(description = "配置 ID", example = "1")
        Long id,
        @Schema(description = "租户 ID", example = "1")
        Long tenantId,
        @Schema(description = "配置键", example = "brand.name")
        String configKey,
        @Schema(description = "配置值，敏感配置会脱敏显示", example = "玄云 ERP")
        String configValue,
        @Schema(description = "值类型", example = "string")
        String valueType,
        @Schema(description = "配置说明", example = "品牌名称")
        String description,
        @Schema(description = "是否公开", example = "true")
        boolean publicConfig,
        @Schema(description = "是否敏感", example = "false")
        boolean sensitive,
        @Schema(description = "是否加密", example = "false")
        boolean encrypted
) {
}
