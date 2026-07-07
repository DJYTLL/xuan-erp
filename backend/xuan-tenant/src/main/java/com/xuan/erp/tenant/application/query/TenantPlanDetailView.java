package com.xuan.erp.tenant.application.query;

import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import java.math.BigDecimal;

public record TenantPlanDetailView(
        Long id,
        String code,
        String name,
        TenantPlanStatus status,
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
