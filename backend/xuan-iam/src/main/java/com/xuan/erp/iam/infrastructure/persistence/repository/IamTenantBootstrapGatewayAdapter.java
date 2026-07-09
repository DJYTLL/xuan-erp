package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.application.port.IamTenantBootstrapGateway;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamTenantBootstrapMapper;
import org.springframework.stereotype.Repository;

/**
 * IAM 租户初始化网关适配器，负责通过 MyBatis Mapper 调用数据库初始化函数。
 */
@Repository
public class IamTenantBootstrapGatewayAdapter implements IamTenantBootstrapGateway {

    private final IamTenantBootstrapMapper mapper;

    public IamTenantBootstrapGatewayAdapter(IamTenantBootstrapMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Integer bootstrapTenant(
            Long tenantId,
            String adminUsername,
            String adminPasswordHash,
            String adminDisplayName,
            String adminEmail,
            String adminPhone,
            String requestedBy) {
        Integer insertedCount = mapper.bootstrapTenant(
                tenantId,
                adminUsername,
                adminPasswordHash,
                adminDisplayName,
                adminEmail,
                adminPhone,
                requestedBy);
        return insertedCount == null ? 0 : insertedCount;
    }
}
