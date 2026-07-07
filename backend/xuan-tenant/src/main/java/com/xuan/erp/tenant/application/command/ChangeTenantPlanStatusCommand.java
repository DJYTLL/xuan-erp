package com.xuan.erp.tenant.application.command;

public record ChangeTenantPlanStatusCommand(
        String reason,
        String operator
) {
}
