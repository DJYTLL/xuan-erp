package com.xuan.erp.iam.application.query;

import java.time.OffsetDateTime;

/**
 * IAM 侧使用的租户状态视图，由 xuan-tenant 查询结果转换而来。
 */
public record IamTenantStatusView(
        Long tenantId,
        String code,
        String status,
        boolean loginAllowed,
        String loginDeniedReason,
        OffsetDateTime currentPlanExpiresAt
) {
}
