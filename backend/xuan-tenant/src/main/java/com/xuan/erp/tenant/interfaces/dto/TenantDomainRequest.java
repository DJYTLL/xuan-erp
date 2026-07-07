package com.xuan.erp.tenant.interfaces.dto;

import java.time.OffsetDateTime;

public record TenantDomainRequest(
        Long tenantId,
        String domain,
        String normalizedDomain,
        String status,
        Boolean primary,
        String verificationToken,
        OffsetDateTime verifiedAt,
        OffsetDateTime lastCheckedAt,
        String remark
) {
}
