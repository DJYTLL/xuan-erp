package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 菜单领域模型，表示全局菜单目录和进入菜单所需权限。
 */
public record IamMenu(
        Long id,
        String code,
        Long parentId,
        String title,
        String i18nKey,
        String path,
        String icon,
        String permissionCode,
        int sortNo,
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
