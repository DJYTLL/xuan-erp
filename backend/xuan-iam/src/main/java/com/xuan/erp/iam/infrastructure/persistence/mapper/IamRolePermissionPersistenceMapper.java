package com.xuan.erp.iam.infrastructure.persistence.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 角色权限持久化 Mapper，负责 iam_role_permission 表的授权读写。
 */
@Mapper
public interface IamRolePermissionPersistenceMapper {

    /**
     * 查询指定角色已拥有的有效权限编码。
     */
    List<String> findPermissionCodesByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    /**
     * 软删除指定角色当前有效权限授权。
     */
    int disableRolePermissions(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId, @Param("operator") String operator);

    /**
     * 幂等新增指定角色的权限授权。
     */
    int insertRolePermission(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId,
            @Param("operator") String operator);

    /**
     * 幂等授予租户管理员成员指定管理员角色。
     */
    int insertTenantAdminUserRole(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId,
            @Param("operator") String operator);

    /**
     * 软删除超出当前租户权限池的角色授权。
     */
    int removeRolePermissionsOutsideTenantEntitlements(@Param("tenantId") Long tenantId, @Param("operator") String operator);

    /**
     * 查询拥有指定角色的有效用户主键。
     */
    List<Long> findUserIdsByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    /**
     * 查询指定用户拥有的有效角色主键。
     */
    List<Long> findRoleIdsByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 查询指定用户通过角色获得的有效权限编码。
     */
    List<String> findPermissionCodesByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 软删除指定用户当前有效角色授权。
     */
    int disableUserRoles(@Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("operator") String operator);

    /**
     * 幂等新增指定用户的角色授权。
     */
    int insertUserRole(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("roleId") Long roleId,
            @Param("operator") String operator);
}
