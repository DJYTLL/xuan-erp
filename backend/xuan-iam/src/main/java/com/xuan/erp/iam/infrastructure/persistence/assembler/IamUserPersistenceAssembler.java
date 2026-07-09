package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserRecord;

/**
 * IAM 用户持久化装配器，负责领域对象与持久化记录之间的转换。
 */
public final class IamUserPersistenceAssembler {

    private IamUserPersistenceAssembler() {
    }

    public static IamUserRecord toRecord(IamUser user) {
        return new IamUserRecord(
                user.id(),
                user.tenantId(),
                user.username(),
                user.passwordHash(),
                user.displayName(),
                user.email(),
                user.phone(),
                user.avatarUrl(),
                user.enabled(),
                user.accountNonExpired(),
                user.accountNonLocked(),
                user.credentialsNonExpired(),
                user.lastLoginAt(),
                user.passwordChangedAt(),
                user.failedLoginCount(),
                user.lastFailedLoginAt(),
                user.lockedUntil(),
                user.mfaEnabled(),
                user.authVersion(),
                user.remark(),
                user.createdBy(),
                user.createdAt(),
                user.updatedBy(),
                user.updatedAt(),
                user.deletedBy(),
                user.deleteReason(),
                user.deletedAt());
    }

    public static IamUser toDomain(IamUserRecord record) {
        return new IamUser(
                record.id(),
                record.tenantId(),
                record.username(),
                record.passwordHash(),
                record.displayName(),
                record.email(),
                record.phone(),
                record.avatarUrl(),
                record.enabled(),
                record.accountNonExpired(),
                record.accountNonLocked(),
                record.credentialsNonExpired(),
                record.lastLoginAt(),
                record.passwordChangedAt(),
                record.failedLoginCount(),
                record.lastFailedLoginAt(),
                record.lockedUntil(),
                record.mfaEnabled(),
                record.authVersion(),
                record.remark(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt(),
                record.deletedBy(),
                record.deleteReason(),
                record.deletedAt());
    }
}
