package com.xuan.erp.tenant.application.command;

public record ChangeTenantStatusCommand(
        String reason,
        String operator,
        String idempotencyKey
) {

    public ChangeTenantStatusCommand(String reason, String operator) {
        this(reason, operator, null);
    }
}
