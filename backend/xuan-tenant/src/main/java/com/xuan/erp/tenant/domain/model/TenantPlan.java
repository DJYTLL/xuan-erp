package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TenantPlan(
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
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {

    public boolean active() {
        return deletedAt == null;
    }
}
