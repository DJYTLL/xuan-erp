package com.xuan.erp.iam.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 租户初始化 Mapper，负责调用 PostgreSQL 租户初始化函数。
 */
@Mapper
public interface IamTenantBootstrapMapper {

    /**
     * 调用租户初始化数据库函数并返回本次新增的菜单授权数量。
     */
    Integer bootstrapTenant(
            @Param("tenantId") Long tenantId,
            @Param("adminUsername") String adminUsername,
            @Param("adminPasswordHash") String adminPasswordHash,
            @Param("adminDisplayName") String adminDisplayName,
            @Param("adminEmail") String adminEmail,
            @Param("adminPhone") String adminPhone,
            @Param("iamInitTemplateCode") String iamInitTemplateCode,
            @Param("requestedBy") String requestedBy);
}
