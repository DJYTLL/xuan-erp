package com.xuan.erp.tenant.application.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
/**
 * 租户套餐分配应用服务，提供套餐分配记录的增删改查入口。
 */
@Service
public class TenantPlanAssignmentApplicationService {

    private static final String RESOURCE_NAME = "tenant-plan-assignments";

    private final TenantResourceApplicationService resourceService;

    /**
     * 注入底层通用租户资源应用服务。
     */
    public TenantPlanAssignmentApplicationService(TenantResourceApplicationService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 查询全部套餐分配记录。
     */
    public List<Map<String, Object>> listAssignments() {
        return resourceService.list(RESOURCE_NAME);
    }

    /**
     * 按主键查询单条套餐分配记录。
     */
    public Map<String, Object> getAssignment(Long id) {
        return resourceService.get(RESOURCE_NAME, id);
    }

    /**
     * 新增一条套餐分配记录。
     */
    public Map<String, Object> createAssignment(Map<String, Object> values) {
        return resourceService.create(RESOURCE_NAME, values);
    }

    /**
     * 更新指定套餐分配记录。
     */
    public Map<String, Object> updateAssignment(Long id, Map<String, Object> values) {
        return resourceService.update(RESOURCE_NAME, id, values);
    }

    /**
     * 逻辑删除指定套餐分配记录，并记录删除原因与操作人。
     */
    public void deleteAssignment(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }
}
