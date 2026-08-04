package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantPlanAssignment;
import java.util.List;

public interface TenantPlanAssignmentRepository {

    TenantPlanAssignment save(TenantPlanAssignment assignment);

    List<TenantPlanAssignment> findActiveByPlanId(Long planId);
}
