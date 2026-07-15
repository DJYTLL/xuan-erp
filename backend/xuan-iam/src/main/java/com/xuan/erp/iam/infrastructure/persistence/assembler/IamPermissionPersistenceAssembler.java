package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamPermissionRecord;

/**
 * IAM 权限持久化装配器，负责领域对象与持久化记录之间的转换。
 */
public final class IamPermissionPersistenceAssembler {

    private IamPermissionPersistenceAssembler() {
    }

    public static IamPermission toDomain(IamPermissionRecord record) {
        return new IamPermission(
                record.id(),
                record.code(),
                record.name(),
                record.serviceName(),
                record.menuCode(),
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

    public static IamPermissionRecord toRecord(IamPermission permission) {
        return new IamPermissionRecord(
                permission.id(),
                permission.code(),
                permission.name(),
                permission.serviceName(),
                permission.menuCode(),
                permission.description(),
                permission.enabled(),
                permission.createdBy(),
                permission.createdAt(),
                permission.updatedBy(),
                permission.updatedAt(),
                permission.deletedBy(),
                permission.deleteReason(),
                permission.deletedAt());
    }
}
