package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.Tenant;
import java.util.List;
import java.util.Optional;

public interface TenantRepository {

    Optional<Tenant> findById(Long id);

    Optional<Tenant> findActiveByNormalizedCode(String normalizedCode);

    List<Tenant> findActiveTenants();

    Tenant save(Tenant tenant);
}
