package com.xuan.erp.tenant.application.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TenantPlanAssignmentApplicationService {

    private static final String RESOURCE_NAME = "tenant-plan-assignments";

    private final TenantResourceApplicationService resourceService;

    public TenantPlanAssignmentApplicationService(TenantResourceApplicationService resourceService) {
        this.resourceService = resourceService;
    }

    public List<Map<String, Object>> listAssignments() {
        return resourceService.list(RESOURCE_NAME);
    }

    public Map<String, Object> getAssignment(Long id) {
        return resourceService.get(RESOURCE_NAME, id);
    }

    public Map<String, Object> createAssignment(Map<String, Object> values) {
        return resourceService.create(RESOURCE_NAME, values);
    }

    public Map<String, Object> updateAssignment(Long id, Map<String, Object> values) {
        return resourceService.update(RESOURCE_NAME, id, values);
    }

    public void deleteAssignment(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }
}
