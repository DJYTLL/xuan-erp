package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;

public interface TenantOutboxEventRepository {

    TenantOutboxEvent append(TenantOutboxEvent event);
}
