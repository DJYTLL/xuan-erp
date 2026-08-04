package com.xuan.erp.iam.domain.repository;

import java.util.List;

/**
 * IAM 租户权限池仓储端口，表达租户可被分配到角色的权限上限。
 */
public interface IamTenantPermissionEntitlementRepository {

    /**
     * 查询租户当前有效权限池编码。
     */
    List<String> findPermissionCodesByTenantId(Long tenantId);

    /**
     * 查询当前绑定指定初始化模板的租户。
     */
    List<Long> findTenantIdsByInitTemplateCode(String initTemplateCode);

    /**
     * 用指定权限主键集合替换租户权限池。
     */
    void replaceTenantEntitlements(
            Long tenantId,
            String initTemplateCode,
            List<Long> permissionIds,
            long entitlementVersion,
            String operator);

    /**
     * 生成租户下一次权限池版本号。
     */
    long nextEntitlementVersion(Long tenantId);
}
