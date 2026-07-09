package com.xuan.erp.tenant.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantScopedConfigRequest(
        @NotBlank(message = "配置值不能为空")
        @Schema(description = "配置值", example = "玄云 ERP")
        String configValue,
        @NotBlank(message = "值类型不能为空")
        @Schema(description = "值类型", example = "string")
        String valueType,
        @Schema(description = "配置说明", example = "品牌名称")
        String description,
        @NotNull(message = "是否公开不能为空")
        @Schema(description = "是否为公开配置", example = "true")
        Boolean publicConfig,
        @NotNull(message = "是否敏感不能为空")
        @Schema(description = "是否为敏感配置", example = "false")
        Boolean sensitive,
        @NotNull(message = "是否加密不能为空")
        @Schema(description = "是否已加密", example = "false")
        Boolean encrypted
) {
}
