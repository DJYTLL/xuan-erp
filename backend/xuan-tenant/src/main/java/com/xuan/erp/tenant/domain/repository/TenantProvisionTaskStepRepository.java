package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantProvisionTaskStep;
import java.util.List;
import java.util.Optional;

public interface TenantProvisionTaskStepRepository {

    List<TenantProvisionTaskStep> findByTaskId(Long taskId);

    Optional<TenantProvisionTaskStep> findActiveByTaskIdAndStepKey(Long taskId, String stepKey);

    TenantProvisionTaskStep save(TenantProvisionTaskStep step);
}
