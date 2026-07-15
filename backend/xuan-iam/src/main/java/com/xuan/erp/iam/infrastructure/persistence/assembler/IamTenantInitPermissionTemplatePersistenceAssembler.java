package com.xuan.erp.iam.infrastructure.persistence.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.iam.domain.model.IamTenantInitPermissionTemplate;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamTenantInitPermissionTemplateRecord;
import java.util.List;

/**
 * IAM 租户初始化权限模板持久化装配器，负责领域对象和数据库记录之间转换。
 */
public final class IamTenantInitPermissionTemplatePersistenceAssembler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private IamTenantInitPermissionTemplatePersistenceAssembler() {
    }

    public static IamTenantInitPermissionTemplate toDomain(IamTenantInitPermissionTemplateRecord record) {
        return new IamTenantInitPermissionTemplate(
                record.id(),
                record.code(),
                record.name(),
                record.description(),
                fromJson(record.permissionCodesJson()),
                Boolean.TRUE.equals(record.defaultTemplate()),
                Boolean.TRUE.equals(record.enabled()),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt(),
                record.deletedBy(),
                record.deleteReason(),
                record.deletedAt());
    }

    public static IamTenantInitPermissionTemplateRecord toRecord(IamTenantInitPermissionTemplate template) {
        return new IamTenantInitPermissionTemplateRecord(
                template.id(),
                template.code(),
                template.name(),
                template.description(),
                toJson(template.permissionCodes()),
                template.defaultTemplate(),
                template.enabled(),
                template.createdBy(),
                template.createdAt(),
                template.updatedBy(),
                template.updatedAt(),
                template.deletedBy(),
                template.deleteReason(),
                template.deletedAt());
    }

    private static List<String> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(value, STRING_LIST);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Invalid IAM tenant init template permission_codes JSON", error);
        }
    }

    private static String toJson(List<String> value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Invalid IAM tenant init template permission codes", error);
        }
    }
}
