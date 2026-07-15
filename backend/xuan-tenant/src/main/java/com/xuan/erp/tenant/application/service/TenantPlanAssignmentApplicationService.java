package com.xuan.erp.tenant.application.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
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
        return resourceService.create(RESOURCE_NAME, normalizeRequiredDefaults(values));
    }

    /**
     * 更新指定套餐分配记录。
     */
    public Map<String, Object> updateAssignment(Long id, Map<String, Object> values) {
        return resourceService.update(RESOURCE_NAME, id, normalizeRequiredDefaults(values));
    }

    /**
     * 逻辑删除指定套餐分配记录，并记录删除原因与操作人。
     */
    public void deleteAssignment(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }

    /**
     * 前端未选择生效时间时按立即生效处理，避免通用 CRUD 把 null 写入非空字段。
     */
    private Map<String, Object> normalizeRequiredDefaults(Map<String, Object> values) {
        Map<String, Object> normalized = new LinkedHashMap<>(values);
        OffsetDateTime now = OffsetDateTime.now();
        if (isBlank(normalized.get("status"))) {
            normalized.put("status", "ACTIVE");
        }
        normalizeDateAlias(normalized, "effectiveAt", "effective_at", now);
        normalizeDateAlias(normalized, "assignedAt", "assigned_at", now);
        return normalized;
    }

    private void normalizeDateAlias(Map<String, Object> values, String camelKey, String snakeKey, OffsetDateTime defaultValue) {
        Object camelValue = values.get(camelKey);
        Object snakeValue = values.get(snakeKey);
        if (!isBlank(camelValue)) {
            return;
        }
        if (!isBlank(snakeValue)) {
            values.put(camelKey, snakeValue);
            return;
        }
        values.put(camelKey, defaultValue);
    }

    private boolean isBlank(Object value) {
        return value == null || (value instanceof String text && text.isBlank());
    }
}
