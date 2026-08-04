package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamRolePermissionPersistenceMapper;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 角色权限仓储适配器，负责通过 Mapper 替换角色授权关系。
 */
@Repository
public class IamRolePermissionRepositoryAdapter implements IamRolePermissionRepository {

    private final IamRolePermissionPersistenceMapper mapper;

    public IamRolePermissionRepositoryAdapter(IamRolePermissionPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<String> findPermissionCodesByRoleId(Long tenantId, Long roleId) {
        return mapper.findPermissionCodesByRoleId(tenantId, roleId);
    }

    @Override
    @Transactional
    public void replaceRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds, String operator) {
        mapper.disableRolePermissions(tenantId, roleId, operator);
        for (Long permissionId : permissionIds) {
            mapper.insertRolePermission(tenantId, roleId, permissionId, operator);
        }
    }

    @Override
    public void grantRoleToTenantAdmins(Long tenantId, Long roleId, String operator) {
        mapper.insertTenantAdminUserRole(tenantId, roleId, operator);
    }

    @Override
    public void removeRolePermissionsOutsideTenantEntitlements(Long tenantId, String operator) {
        mapper.removeRolePermissionsOutsideTenantEntitlements(tenantId, operator);
    }

    @Override
    public List<Long> findUserIdsByRoleId(Long tenantId, Long roleId) {
        return mapper.findUserIdsByRoleId(tenantId, roleId);
    }

    @Override
    public List<Long> findRoleIdsByUserId(Long tenantId, Long userId) {
        return mapper.findRoleIdsByUserId(tenantId, userId);
    }

    @Override
    public List<String> findPermissionCodesByUserId(Long tenantId, Long userId) {
        return mapper.findPermissionCodesByUserId(tenantId, userId);
    }

    @Override
    @Transactional
    public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds, String operator) {
        mapper.disableUserRoles(tenantId, userId, operator);
        for (Long roleId : roleIds) {
            mapper.insertUserRole(tenantId, userId, roleId, operator);
        }
    }
}
