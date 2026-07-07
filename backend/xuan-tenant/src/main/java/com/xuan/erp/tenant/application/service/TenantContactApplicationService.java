package com.xuan.erp.tenant.application.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TenantContactApplicationService {

    private static final String RESOURCE_NAME = "tenant-contacts";

    private final TenantResourceApplicationService resourceService;

    public TenantContactApplicationService(TenantResourceApplicationService resourceService) {
        this.resourceService = resourceService;
    }

    public List<Map<String, Object>> listContacts() {
        return resourceService.list(RESOURCE_NAME);
    }

    public Map<String, Object> getContact(Long id) {
        return resourceService.get(RESOURCE_NAME, id);
    }

    public Map<String, Object> createContact(Map<String, Object> values) {
        return resourceService.create(RESOURCE_NAME, values);
    }

    public Map<String, Object> updateContact(Long id, Map<String, Object> values) {
        return resourceService.update(RESOURCE_NAME, id, values);
    }

    public void deleteContact(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }
}
