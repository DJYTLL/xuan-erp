package com.xuan.erp.tenant.interfaces.dto;

import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record TenantInternalStatusResponse(
        Long tenantId,
        String code,
        String name,
        TenantStatus status,
        boolean loginAllowed,
        String loginDeniedReason,
        OffsetDateTime currentPlanExpiresAt,
        String permissionHash,
        String iamInitTemplateCode,
        List<String> columnPermissionTemplateCodes,
        String defaultColumnPermissionTemplateCode
) {
}
