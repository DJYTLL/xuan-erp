package com.xuan.erp.tenant.interfaces.dto;

import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record TenantPlanResponse(
        @Schema(description = "套餐 ID", example = "1")
        Long id,
        @Schema(description = "套餐编码", example = "FREE")
        String code,
        @Schema(description = "套餐名称", example = "基础版")
        String name,
        @Schema(description = "套餐状态", example = "ENABLED")
        TenantPlanStatus status,
        @Schema(description = "计费周期", example = "MONTHLY")
        BillingCycle billingCycle,
        @Schema(description = "套餐价格", example = "0.00")
        BigDecimal priceAmount,
        @Schema(description = "币种", example = "CNY")
        String currency,
        @Schema(description = "最大用户数，空表示不限", example = "10")
        Integer maxUserCount,
        @Schema(description = "最大仓库数，空表示不限", example = "3")
        Integer maxWarehouseCount,
        @Schema(description = "最大存储 GB，空表示不限", example = "10")
        BigDecimal maxStorageGb,
        @Schema(description = "功能开关 JSON", example = "{\"modules\":[\"product\",\"sales\"]}")
        String featureFlagsJson,
        @Schema(description = "排序号", example = "10")
        Integer sortNo,
        @Schema(description = "备注", example = "V1 默认基础套餐")
        String remark
) {
}
