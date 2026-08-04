package com.xuan.erp.iam.domain.model;

/**
 * IAM 列权限模板明细领域模型，定义单个字段的访问级别。
 */
public record IamColumnPermissionTemplateItem(
        Long id,
        Long templateId,
        Long resourceColumnId,
        String resourceKey,
        String columnKey,
        String columnName,
        String accessMode
) {
}
