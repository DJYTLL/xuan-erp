package com.xuan.erp.tenant.interfaces.dto;

public record ChangeTenantPlanStatusRequest(
        String reason,
        String operator
) {
}
