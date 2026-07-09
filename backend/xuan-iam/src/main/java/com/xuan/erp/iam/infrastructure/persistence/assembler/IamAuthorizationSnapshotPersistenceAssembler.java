package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamAuthorizationSnapshotRecord;
import java.util.List;
import java.util.Map;

/**
 * IAM 授权快照持久化装配器，负责领域对象与持久化记录之间的转换。
 */
public final class IamAuthorizationSnapshotPersistenceAssembler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private static final TypeReference<Map<String, List<String>>> COLUMN_SETTINGS = new TypeReference<>() { };

    private IamAuthorizationSnapshotPersistenceAssembler() {
    }

    public static IamAuthorizationSnapshotRecord toRecord(IamAuthorizationSnapshot snapshot) {
        return new IamAuthorizationSnapshotRecord(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.userId(),
                snapshot.authVersion(),
                write(snapshot.roleIds() == null ? List.of() : snapshot.roleIds()),
                write(snapshot.permissionCodes() == null ? List.of() : snapshot.permissionCodes()),
                write(snapshot.menuCodes() == null ? List.of() : snapshot.menuCodes()),
                write(snapshot.columnSettings() == null ? Map.of() : snapshot.columnSettings()),
                snapshot.snapshotHash(),
                snapshot.expiresAt(),
                snapshot.builtAt(),
                snapshot.createdBy(),
                snapshot.createdAt(),
                snapshot.updatedBy(),
                snapshot.updatedAt());
    }

    public static IamAuthorizationSnapshot toDomain(IamAuthorizationSnapshotRecord record) {
        return new IamAuthorizationSnapshot(
                record.id(),
                record.tenantId(),
                record.userId(),
                record.authVersion(),
                read(record.roleIdsJson(), LONG_LIST, List.of()),
                read(record.permissionCodesJson(), STRING_LIST, List.of()),
                read(record.menuCodesJson(), STRING_LIST, List.of()),
                read(record.columnSettingsJson(), COLUMN_SETTINGS, Map.of()),
                record.snapshotHash(),
                record.expiresAt(),
                record.builtAt(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt());
    }

    private static String write(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("IAM 授权快照序列化失败", ex);
        }
    }

    private static <T> T read(String value, TypeReference<T> typeReference, T fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return OBJECT_MAPPER.readValue(value, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("IAM 授权快照反序列化失败", ex);
        }
    }
}
