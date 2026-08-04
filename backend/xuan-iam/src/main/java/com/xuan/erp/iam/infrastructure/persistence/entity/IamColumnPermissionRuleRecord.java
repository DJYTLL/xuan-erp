package com.xuan.erp.iam.infrastructure.persistence.entity;

/**
 * IAM 列权限规则查询记录。
 */
public record IamColumnPermissionRuleRecord(
        String resourceKey,
        String columnKey,
        String accessMode
) {
}
