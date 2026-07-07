package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantPlan;
import java.util.List;
import java.util.Optional;

public interface TenantPlanRepository {

    Optional<TenantPlan> findById(Long id);

    Optional<TenantPlan> findActiveByCode(String code);

    List<TenantPlan> findActivePlans();

    TenantPlan save(TenantPlan plan);
}
