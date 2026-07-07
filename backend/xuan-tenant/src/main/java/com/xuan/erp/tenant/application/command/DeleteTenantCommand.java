package com.xuan.erp.tenant.application.command;

public record DeleteTenantCommand(
        String reason,
        String operator
) {
}
