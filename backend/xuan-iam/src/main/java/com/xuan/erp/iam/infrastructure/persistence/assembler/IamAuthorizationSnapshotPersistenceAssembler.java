package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamAuthorizationSnapshotRecord;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IAM 授权快照持久化装配器，负责领域对象与持久化记录之间的转换。
 */
public final class IamAuthorizationSnapshotPersistenceAssembler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

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
                readColumnSettings(record.columnSettingsJson()),
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

    private static Map<String, Map<String, String>> readColumnSettings(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(value);
            if (root == null || !root.isObject()) {
                return Map.of();
            }
            Map<String, Map<String, String>> result = new LinkedHashMap<>();
            root.fields().forEachRemaining(resource -> {
                if (resource.getKey() == null || resource.getKey().isBlank()) {
                    return;
                }
                Map<String, String> columns = readResourceColumns(resource.getValue());
                if (!columns.isEmpty()) {
                    result.put(resource.getKey().trim(), Map.copyOf(columns));
                }
            });
            return Map.copyOf(result);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("IAM 授权快照列权限反序列化失败", ex);
        }
    }

    private static Map<String, String> readResourceColumns(JsonNode node) {
        Map<String, String> columns = new LinkedHashMap<>();
        if (node == null || node.isNull()) {
            return columns;
        }
        if (node.isArray()) {
            node.forEach(column -> {
                if (column.isTextual() && !column.asText().isBlank()) {
                    columns.put(column.asText().trim(), "VISIBLE");
                }
            });
            return columns;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(column -> {
                if (column.getKey() != null && !column.getKey().isBlank()) {
                    columns.put(column.getKey().trim(), normalizeAccess(column.getValue().asText()));
                }
            });
        }
        return columns;
    }

    private static String normalizeAccess(String value) {
        if (value == null || value.isBlank()) {
            return "HIDDEN";
        }
        return switch (value.trim().toUpperCase()) {
            case "VISIBLE", "MASKED", "HIDDEN" -> value.trim().toUpperCase();
            default -> "HIDDEN";
        };
    }
}
