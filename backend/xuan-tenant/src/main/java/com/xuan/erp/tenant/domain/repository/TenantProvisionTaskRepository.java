package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import java.util.Optional;

public interface TenantProvisionTaskRepository {

    Optional<TenantProvisionTask> findActiveByTenantIdAndTaskKey(Long tenantId, String taskKey);

    TenantProvisionTask save(TenantProvisionTask task);
}
