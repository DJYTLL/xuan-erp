package com.xuan.erp.iam.application.query;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * IAM 侧使用的租户状态视图，由 xuan-tenant 查询结果转换而来。
 */
public record IamTenantStatusView(
        Long tenantId,
        String code,
        String name,
        String status,
        boolean loginAllowed,
        String loginDeniedReason,
        OffsetDateTime currentPlanExpiresAt,
        String permissionHash,
        String iamInitTemplateCode,
        List<String> columnPermissionTemplateCodes,
        String defaultColumnPermissionTemplateCode
) {
    public IamTenantStatusView(
            Long tenantId,
            String code,
            String name,
            String status,
            boolean loginAllowed,
            String loginDeniedReason,
            OffsetDateTime currentPlanExpiresAt
    ) {
        this(tenantId, code, name, status, loginAllowed, loginDeniedReason, currentPlanExpiresAt, null, null, null, null);
    }
}
