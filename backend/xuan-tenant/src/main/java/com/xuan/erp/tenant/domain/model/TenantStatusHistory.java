package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;

public record TenantStatusHistory(
        Long id,
        Long tenantId,
        TenantStatus fromStatus,
        TenantStatus toStatus,
        String changeType,
        String changeReason,
        OffsetDateTime changedAt,
        String changedBy,
        String traceId,
        String requestId,
        String source,
        String createdBy,
        OffsetDateTime createdAt
) {
}
