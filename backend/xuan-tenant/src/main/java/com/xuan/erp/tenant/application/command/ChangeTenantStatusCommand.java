package com.xuan.erp.tenant.application.command;

public record ChangeTenantStatusCommand(
        String reason,
        String operator
) {
}
