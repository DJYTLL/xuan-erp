package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import java.util.Optional;

public interface TenantOutboxEventRepository {

    Optional<TenantOutboxEvent> findById(Long eventId);

    TenantOutboxEvent append(TenantOutboxEvent event);

    TenantOutboxEvent save(TenantOutboxEvent event);
}
