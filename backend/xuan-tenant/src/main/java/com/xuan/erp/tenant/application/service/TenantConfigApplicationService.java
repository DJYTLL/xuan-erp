package com.xuan.erp.tenant.application.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TenantConfigApplicationService {

    private static final String RESOURCE_NAME = "tenant-configs";

    private final TenantResourceApplicationService resourceService;

    public TenantConfigApplicationService(TenantResourceApplicationService resourceService) {
        this.resourceService = resourceService;
    }

    public List<Map<String, Object>> listConfigs() {
        return resourceService.list(RESOURCE_NAME);
    }

    public Map<String, Object> getConfig(Long id) {
        return resourceService.get(RESOURCE_NAME, id);
    }

    public Map<String, Object> createConfig(Map<String, Object> values) {
        return resourceService.create(RESOURCE_NAME, values);
    }

    public Map<String, Object> updateConfig(Long id, Map<String, Object> values) {
        return resourceService.update(RESOURCE_NAME, id, values);
    }

    public void deleteConfig(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }
}
