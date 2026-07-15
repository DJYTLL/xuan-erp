package com.xuan.erp.iam.domain.repository;

import java.util.List;

/**
 * IAM 角色权限仓储端口，隔离角色授权关系的读取和替换写入。
 */
public interface IamRolePermissionRepository {

    /**
     * 查询指定角色已拥有的有效权限编码。
     */
    List<String> findPermissionCodesByRoleId(Long tenantId, Long roleId);

    /**
     * 使用新的权限主键集合替换角色授权。
     */
    void replaceRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds, String operator);

    /**
     * 查询拥有指定角色的有效用户主键。
     */
    List<Long> findUserIdsByRoleId(Long tenantId, Long roleId);

    /**
     * 查询指定用户拥有的有效角色主键。
     */
    List<Long> findRoleIdsByUserId(Long tenantId, Long userId);

    /**
     * 查询指定用户通过角色获得的有效权限编码。
     */
    List<String> findPermissionCodesByUserId(Long tenantId, Long userId);

    /**
     * 使用新的角色主键集合替换用户角色授权。
     */
    void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds, String operator);
}
