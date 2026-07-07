package com.xuan.erp.tenant.domain.repository;

import com.xuan.erp.tenant.domain.model.resource.TenantResourceDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TenantResourceRepository {

    List<Map<String, Object>> list(TenantResourceDefinition resource);

    Optional<Map<String, Object>> findById(TenantResourceDefinition resource, Long id);

    Map<String, Object> create(TenantResourceDefinition resource, Map<String, Object> values);

    Map<String, Object> update(TenantResourceDefinition resource, Long id, Map<String, Object> values);

    void softDelete(TenantResourceDefinition resource, Long id, String reason, String operator);
}
