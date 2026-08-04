package com.xuan.erp.tenant.domain.repository;

import java.util.List;
import java.util.Optional;

/**
 * 租户服务触发 IAM 按套餐模板同步租户权限的网关端口。
 */
public interface TenantIamBootstrapGateway {

    /**
     * 按指定 IAM 初始化模板重算租户权限。
     */
    void bootstrapTenant(Long tenantId, String iamInitTemplateCode, String operator);

    /**
     * 按指定套餐权限边界重算租户页面权限和列权限模板池。
     */
    default void bootstrapTenant(
            Long tenantId,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String operator) {
        bootstrapTenant(tenantId, iamInitTemplateCode, operator);
    }

    /**
     * 按指定套餐权限边界重算租户页面权限和列权限模板池，并同步 Tenant 侧计算出的权限指纹。
     */
    default void bootstrapTenant(
            Long tenantId,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String permissionHash,
            String operator) {
        bootstrapTenant(tenantId, iamInitTemplateCode, columnPermissionTemplateCodes, defaultColumnPermissionTemplateCode, operator);
    }

    /**
     * 查询 IAM 已成功同步的租户权限指纹。
     */
    default Optional<String> findLastSyncedPermissionHash(Long tenantId) {
        return Optional.empty();
    }
}
