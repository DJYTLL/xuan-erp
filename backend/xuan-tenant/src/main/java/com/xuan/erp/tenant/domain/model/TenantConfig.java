package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.ConfigValueType;
import java.time.OffsetDateTime;

public record TenantConfig(
        Long id,
        Long tenantId,
        String configKey,
        String configValue,
        ConfigValueType valueType,
        String description,
        boolean publicConfig,
        boolean sensitive,
        boolean encrypted,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {

    public boolean publicReadable() {
        return publicConfig && !sensitive && deletedAt == null;
    }

    public String displayValue() {
        return sensitive ? "******" : configValue;
    }

    public TenantConfig validateBoundary() {
        if (publicConfig && sensitive) {
            throw new IllegalStateException("public config cannot be sensitive");
        }
        return this;
    }
}
