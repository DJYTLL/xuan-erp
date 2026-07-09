package com.xuan.erp.tenant.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

public record TenantConfigRecord(
        Long id,
        Long tenantId,
        String configKey,
        String configValue,
        String valueType,
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
}
