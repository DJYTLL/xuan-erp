package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamMenuRecord;

/**
 * IAM 菜单持久化装配器，负责领域对象与持久化记录之间的转换。
 */
public final class IamMenuPersistenceAssembler {

    private IamMenuPersistenceAssembler() {
    }

    public static IamMenu toDomain(IamMenuRecord record) {
        return new IamMenu(
                record.id(),
                record.code(),
                record.parentId(),
                record.title(),
                record.i18nKey(),
                record.path(),
                record.icon(),
                record.permissionCode(),
                record.sortNo(),
                record.enabled(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt(),
                record.deletedBy(),
                record.deleteReason(),
                record.deletedAt());
    }
}
