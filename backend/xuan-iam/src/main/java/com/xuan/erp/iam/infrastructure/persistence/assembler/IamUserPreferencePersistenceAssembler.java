package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.xuan.erp.iam.domain.model.IamUserPreference;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserPreferenceRecord;

/**
 * IAM 用户通用偏好持久化装配器，负责领域模型和记录对象转换。
 */
public final class IamUserPreferencePersistenceAssembler {

    private IamUserPreferencePersistenceAssembler() {
    }

    public static IamUserPreference toDomain(IamUserPreferenceRecord record) {
        return new IamUserPreference(
                record.id(),
                record.tenantId(),
                record.userId(),
                record.preferenceKey(),
                record.preferenceJson(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt());
    }

    public static IamUserPreferenceRecord toRecord(IamUserPreference preference) {
        return new IamUserPreferenceRecord(
                preference.id(),
                preference.tenantId(),
                preference.userId(),
                preference.preferenceKey(),
                preference.preferenceJson(),
                preference.createdBy(),
                preference.createdAt(),
                preference.updatedBy(),
                preference.updatedAt());
    }
}
