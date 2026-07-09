package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantDetailSupplement;
import java.util.List;
import java.util.Optional;

public interface TenantRepository {

    Optional<Tenant> findById(Long id);

    Optional<Tenant> findActiveByNormalizedCode(String normalizedCode);

    List<Tenant> findActiveTenants();

    List<Tenant> findActiveTenants(long offset, long limit);

    long countActiveTenants();

    default TenantDetailSupplement getDetailSupplement(Long tenantId) {
        return TenantDetailSupplement.empty();
    }

    Tenant save(Tenant tenant);
}
