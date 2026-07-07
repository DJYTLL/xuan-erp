package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;
import java.util.Locale;

public record Tenant(
        Long id,
        String code,
        String normalizedCode,
        String name,
        TenantStatus status,
        String contactName,
        String contactPhone,
        OffsetDateTime provisionedAt,
        OffsetDateTime enabledAt,
        OffsetDateTime disabledAt,
        String disabledReason,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {

    public static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("tenant code must not be blank");
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    public boolean active() {
        return deletedAt == null;
    }
}
