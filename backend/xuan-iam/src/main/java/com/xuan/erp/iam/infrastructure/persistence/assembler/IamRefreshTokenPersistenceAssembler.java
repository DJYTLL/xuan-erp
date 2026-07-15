package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.xuan.erp.iam.domain.model.IamRefreshToken;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamRefreshTokenRecord;

/**
 * IAM refresh token 持久化装配器，负责领域对象与持久化记录互转。
 */
public final class IamRefreshTokenPersistenceAssembler {

    private IamRefreshTokenPersistenceAssembler() {
    }

    public static IamRefreshTokenRecord toRecord(IamRefreshToken token) {
        return new IamRefreshTokenRecord(
                token.id(),
                token.tenantId(),
                token.userId(),
                token.tokenHash(),
                token.tokenFamilyId(),
                token.expiresAt(),
                token.revokedAt(),
                token.replacedByTokenHash(),
                token.lastUsedAt(),
                token.audienceTenantId(),
                token.deviceId(),
                token.ipAddress(),
                token.userAgent(),
                token.createdBy(),
                token.createdAt(),
                token.updatedBy(),
                token.updatedAt());
    }

    public static IamRefreshToken toDomain(IamRefreshTokenRecord record) {
        return new IamRefreshToken(
                record.id(),
                record.tenantId(),
                record.userId(),
                record.tokenHash(),
                record.tokenFamilyId(),
                record.expiresAt(),
                record.revokedAt(),
                record.replacedByTokenHash(),
                record.lastUsedAt(),
                record.audienceTenantId(),
                record.deviceId(),
                record.ipAddress(),
                record.userAgent(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt());
    }
}
