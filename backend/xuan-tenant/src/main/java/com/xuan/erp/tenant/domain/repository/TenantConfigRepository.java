package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantConfig;
import java.util.List;
import java.util.Optional;

public interface TenantConfigRepository {

    Optional<TenantConfig> findActiveByTenantIdAndKey(Long tenantId, String configKey);

    List<TenantConfig> findPublicByTenantId(Long tenantId);

    TenantConfig save(TenantConfig tenantConfig);
}
