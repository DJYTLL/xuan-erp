package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.TenantStatusHistory;

public interface TenantStatusHistoryRepository {

    TenantStatusHistory append(TenantStatusHistory history);
}
