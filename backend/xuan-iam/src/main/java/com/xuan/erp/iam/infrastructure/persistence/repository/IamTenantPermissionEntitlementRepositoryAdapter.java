package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.repository.IamTenantPermissionEntitlementRepository;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamTenantPermissionEntitlementPersistenceMapper;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 租户权限池仓储适配器。
 */
@Repository
public class IamTenantPermissionEntitlementRepositoryAdapter implements IamTenantPermissionEntitlementRepository {

    private final IamTenantPermissionEntitlementPersistenceMapper mapper;

    public IamTenantPermissionEntitlementRepositoryAdapter(IamTenantPermissionEntitlementPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<String> findPermissionCodesByTenantId(Long tenantId) {
        return mapper.findPermissionCodesByTenantId(tenantId);
    }

    @Override
    public List<Long> findTenantIdsByInitTemplateCode(String initTemplateCode) {
        return mapper.findTenantIdsByInitTemplateCode(initTemplateCode);
    }

    @Override
    @Transactional
    public void replaceTenantEntitlements(
            Long tenantId,
            String initTemplateCode,
            List<Long> permissionIds,
            long entitlementVersion,
            String operator) {
        mapper.upsertTenantInitTemplateBinding(tenantId, initTemplateCode, entitlementVersion, operator);
        mapper.disableTenantEntitlements(tenantId, operator);
        for (Long permissionId : permissionIds.stream().distinct().sorted().toList()) {
            mapper.insertTenantEntitlement(tenantId, initTemplateCode, permissionId, entitlementVersion, operator);
        }
    }

    @Override
    public long nextEntitlementVersion(Long tenantId) {
        return mapper.nextEntitlementVersion(tenantId);
    }
}
