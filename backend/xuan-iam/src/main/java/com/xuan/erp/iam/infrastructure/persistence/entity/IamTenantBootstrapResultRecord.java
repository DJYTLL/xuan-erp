package com.xuan.erp.iam.infrastructure.persistence.entity;

/**
 * IAM 租户初始化结果记录，表达数据库函数返回的菜单授权新增数量。
 */
public record IamTenantBootstrapResultRecord(
        Integer insertedCount
) {
}
