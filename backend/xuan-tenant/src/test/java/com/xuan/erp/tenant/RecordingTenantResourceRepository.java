package com.xuan.erp.tenant;

import com.xuan.erp.tenant.domain.repository.TenantResourceRepository;
import com.xuan.erp.tenant.domain.model.resource.TenantResourceDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class RecordingTenantResourceRepository implements TenantResourceRepository {

    private TenantResourceDefinition lastResource;
    private String lastDeleteReason;

    @Override
    public List<Map<String, Object>> list(TenantResourceDefinition resource) {
        lastResource = resource;
        return List.of();
    }

    @Override
    public Optional<Map<String, Object>> findById(TenantResourceDefinition resource, Long id) {
        lastResource = resource;
        return Optional.empty();
    }

    @Override
    public Map<String, Object> create(TenantResourceDefinition resource, Map<String, Object> values) {
        lastResource = resource;
        return values;
    }

    @Override
    public Map<String, Object> update(TenantResourceDefinition resource, Long id, Map<String, Object> values) {
        lastResource = resource;
        return values;
    }

    @Override
    public void softDelete(TenantResourceDefinition resource, Long id, String reason, String operator) {
        lastResource = resource;
        lastDeleteReason = reason;
    }

    TenantResourceDefinition lastResource() {
        return lastResource;
    }

    String lastDeleteReason() {
        return lastDeleteReason;
    }
}
