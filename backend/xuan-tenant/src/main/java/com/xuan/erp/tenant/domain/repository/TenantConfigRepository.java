package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantConfig;
import java.util.List;
import java.util.Optional;

public interface TenantConfigRepository {

    Optional<TenantConfig> findById(Long id);

    Optional<TenantConfig> findActiveByTenantIdAndKey(Long tenantId, String configKey);

    List<TenantConfig> findByTenantId(Long tenantId, long offset, long limit);

    long countByTenantId(Long tenantId);

    List<TenantConfig> findPublicByTenantId(Long tenantId);

    TenantConfig save(TenantConfig tenantConfig);
}
