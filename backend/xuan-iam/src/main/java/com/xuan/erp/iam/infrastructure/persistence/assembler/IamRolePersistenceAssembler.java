package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamRoleRecord;

/**
 * IAM 角色持久化装配器，负责领域对象与持久化记录之间的转换。
 */
public final class IamRolePersistenceAssembler {

    private IamRolePersistenceAssembler() {
    }

    public static IamRole toDomain(IamRoleRecord record) {
        return new IamRole(
                record.id(),
                record.tenantId(),
                record.code(),
                record.name(),
                record.description(),
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
