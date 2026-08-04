package com.xuan.erp.iam.domain.model;

/**
 * IAM 资源字段领域模型，描述业务资源中可被列权限消费的字段。
 */
public record IamResourceColumn(
        Long id,
        String resourceKey,
        String columnKey,
        String columnName,
        String dataType,
        String maskType,
        boolean enabled,
        Integer sortNo
) {
}
