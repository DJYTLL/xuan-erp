package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.PlanAssignmentStatus;
import java.time.OffsetDateTime;

public record TenantPlanAssignment(
        Long id,
        Long tenantId,
        Long previousPlanId,
        Long planId,
        PlanAssignmentStatus status,
        OffsetDateTime effectiveAt,
        OffsetDateTime expiresAt,
        OffsetDateTime assignedAt,
        String assignedBy,
        String changeReason,
        String source,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
