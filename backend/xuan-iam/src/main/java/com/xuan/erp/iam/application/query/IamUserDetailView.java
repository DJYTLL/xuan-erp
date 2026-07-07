package com.xuan.erp.iam.application.query;

import java.time.OffsetDateTime;

/**
 * IAM 用户详情查询视图，用于应用层向接口层暴露用户基础信息。
 */
public record IamUserDetailView(
        Long id,
        Long tenantId,
        String username,
        String displayName,
        String email,
        String phone,
        boolean enabled,
        boolean accountNonLocked,
        long authVersion,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
