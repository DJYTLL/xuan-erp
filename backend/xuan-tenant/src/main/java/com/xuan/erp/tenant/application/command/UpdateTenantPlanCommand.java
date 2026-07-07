package com.xuan.erp.tenant.application.command;

import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import java.math.BigDecimal;

public record UpdateTenantPlanCommand(
        String name,
        BillingCycle billingCycle,
        BigDecimal priceAmount,
        String currency,
        Integer maxUserCount,
        Integer maxWarehouseCount,
        BigDecimal maxStorageGb,
        String featureFlagsJson,
        Integer sortNo,
        String remark
) {
}
