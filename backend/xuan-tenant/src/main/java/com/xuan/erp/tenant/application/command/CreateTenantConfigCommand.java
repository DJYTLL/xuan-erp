package com.xuan.erp.tenant.application.command;

public record CreateTenantConfigCommand(
        Long tenantId,
        String configKey,
        String configValue,
        String valueType,
        String description,
        boolean publicConfig,
        boolean sensitive,
        boolean encrypted
) {
}
