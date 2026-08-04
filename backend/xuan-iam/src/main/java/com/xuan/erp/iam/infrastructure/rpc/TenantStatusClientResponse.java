package com.xuan.erp.iam.infrastructure.rpc;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * xuan-tenant 内部租户状态接口返回的数据结构。
 */
public record TenantStatusClientResponse(
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
}
