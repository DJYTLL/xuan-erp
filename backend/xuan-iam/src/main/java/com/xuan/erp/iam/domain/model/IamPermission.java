package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 权限领域模型，表示由各业务服务同步到 IAM 的权限定义。
 */
public record IamPermission(
        Long id,
        String code,
        String name,
        String serviceName,
        String menuCode,
        String description,
        boolean enabled,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {

    public boolean active() {
        return deletedAt == null;
    }
}
