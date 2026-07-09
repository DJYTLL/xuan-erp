package com.xuan.erp.tenant.application.command;

public record DeleteTenantCommand(
        String reason,
        String operator,
        String idempotencyKey
) {

    public DeleteTenantCommand(String reason, String operator) {
        this(reason, operator, null);
    }
}
