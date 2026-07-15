package com.xuan.erp.tenant.application.query;

import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;

public record TenantInternalStatusView(
        Long tenantId,
        String code,
        TenantStatus status,
        boolean loginAllowed,
        String loginDeniedReason,
        OffsetDateTime currentPlanExpiresAt
) {
}
