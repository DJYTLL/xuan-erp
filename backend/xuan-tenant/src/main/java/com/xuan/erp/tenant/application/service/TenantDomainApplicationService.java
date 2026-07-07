package com.xuan.erp.tenant.application.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TenantDomainApplicationService {

    private static final String RESOURCE_NAME = "tenant-domains";

    private final TenantResourceApplicationService resourceService;

    public TenantDomainApplicationService(TenantResourceApplicationService resourceService) {
        this.resourceService = resourceService;
    }

    public List<Map<String, Object>> listDomains() {
        return resourceService.list(RESOURCE_NAME);
    }

    public Map<String, Object> getDomain(Long id) {
        return resourceService.get(RESOURCE_NAME, id);
    }

    public Map<String, Object> createDomain(Map<String, Object> values) {
        return resourceService.create(RESOURCE_NAME, values);
    }

    public Map<String, Object> updateDomain(Long id, Map<String, Object> values) {
        return resourceService.update(RESOURCE_NAME, id, values);
    }

    public void deleteDomain(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }
}
