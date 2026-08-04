package com.xuan.erp.iam.infrastructure.persistence.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 租户权限池 Mapper，负责 iam_tenant_permission_entitlement 表读写。
 */
@Mapper
public interface IamTenantPermissionEntitlementPersistenceMapper {

    List<String> findPermissionCodesByTenantId(@Param("tenantId") Long tenantId);

    List<Long> findTenantIdsByInitTemplateCode(@Param("initTemplateCode") String initTemplateCode);

    long nextEntitlementVersion(@Param("tenantId") Long tenantId);

    int disableTenantEntitlements(@Param("tenantId") Long tenantId, @Param("operator") String operator);

    int upsertTenantInitTemplateBinding(
            @Param("tenantId") Long tenantId,
            @Param("initTemplateCode") String initTemplateCode,
            @Param("entitlementVersion") long entitlementVersion,
            @Param("operator") String operator);

    int insertTenantEntitlement(
            @Param("tenantId") Long tenantId,
            @Param("initTemplateCode") String initTemplateCode,
            @Param("permissionId") Long permissionId,
            @Param("entitlementVersion") long entitlementVersion,
            @Param("operator") String operator);
}
