package com.xuan.erp.tenant.application.query;

import com.xuan.erp.tenant.domain.model.type.ConfigValueType;

public record TenantConfigDetailView(
        Long id,
        Long tenantId,
        String configKey,
        String configValue,
        ConfigValueType valueType,
        String description,
        boolean publicConfig,
        boolean sensitive,
        boolean encrypted
) {
}
