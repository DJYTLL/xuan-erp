package com.xuan.erp.tenant.interfaces.dto;

public record TenantConfigRequest(
        Long tenantId,
        String configKey,
        String configValue,
        String valueType,
        String description,
        Boolean publicConfig,
        Boolean sensitive,
        Boolean encrypted
) {
}
