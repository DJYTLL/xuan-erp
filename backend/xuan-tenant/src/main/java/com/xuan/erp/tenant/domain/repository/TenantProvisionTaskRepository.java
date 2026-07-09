package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import java.util.List;
import java.util.Optional;

public interface TenantProvisionTaskRepository {

    Optional<TenantProvisionTask> findById(Long taskId);

    List<TenantProvisionTask> findActiveByTenantId(Long tenantId);

    Optional<TenantProvisionTask> findActiveByTenantIdAndTaskKey(Long tenantId, String taskKey);

    Optional<TenantProvisionTask> findActiveByTaskKeyAndIdempotencyKey(String taskKey, String idempotencyKey);

    TenantProvisionTask save(TenantProvisionTask task);
}
