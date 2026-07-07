package com.xuan.erp.tenant.interfaces.dto;

import java.time.OffsetDateTime;

public record TenantPlanAssignmentRequest(
        Long tenantId,
        Long previousPlanId,
        Long planId,
        String status,
        OffsetDateTime effectiveAt,
        OffsetDateTime expiresAt,
        OffsetDateTime assignedAt,
        String assignedBy,
        String changeReason,
        String source,
        String remark
) {
}
