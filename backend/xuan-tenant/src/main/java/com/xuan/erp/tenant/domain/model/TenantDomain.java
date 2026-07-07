package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.TenantDomainStatus;
import java.time.OffsetDateTime;
import java.util.Locale;

public record TenantDomain(
        Long id,
        Long tenantId,
        String domain,
        String normalizedDomain,
        TenantDomainStatus status,
        boolean primaryDomain,
        String verificationToken,
        OffsetDateTime verifiedAt,
        OffsetDateTime lastCheckedAt,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {

    public static String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            throw new IllegalArgumentException("tenant domain must not be blank");
        }
        return domain.trim().toLowerCase(Locale.ROOT);
    }
}
