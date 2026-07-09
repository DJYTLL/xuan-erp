package com.xuan.erp.iam.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * IAM 菜单持久化记录，表达 iam_menu 表字段在基础设施层的结构。
 */
public record IamMenuRecord(
        Long id,
        String code,
        Long parentId,
        String title,
        String i18nKey,
        String path,
        String icon,
        String permissionCode,
        Integer sortNo,
        Boolean enabled,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
