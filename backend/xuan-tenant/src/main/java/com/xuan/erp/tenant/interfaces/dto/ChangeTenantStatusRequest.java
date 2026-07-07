package com.xuan.erp.tenant.interfaces.dto;

public record ChangeTenantStatusRequest(
        String reason,
        String operator
) {
}
