package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import java.util.List;
import java.util.Optional;

public interface TenantOutboxEventRepository {

    Optional<TenantOutboxEvent> findById(Long eventId);

    List<TenantOutboxEvent> findPublishable(int limit);

    TenantOutboxEvent append(TenantOutboxEvent event);

    TenantOutboxEvent save(TenantOutboxEvent event);
}
