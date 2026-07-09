package com.xuan.erp.tenant.application.command;

public record UpdateTenantConfigCommand(
        String configValue,
        String valueType,
        String description,
        boolean publicConfig,
        boolean sensitive,
        boolean encrypted
) {
}
