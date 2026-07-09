package com.xuan.erp.tenant.application.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
/**
 * 租户联系人应用服务，提供租户联系人的增删改查操作入口。
 */
@Service
public class TenantContactApplicationService {

    private static final String RESOURCE_NAME = "tenant-contacts";

    private final TenantResourceApplicationService resourceService;

    /**
     * 注入底层通用租户资源应用服务。
     */
    public TenantContactApplicationService(TenantResourceApplicationService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 查询全部租户联系人记录。
     */
    public List<Map<String, Object>> listContacts() {
        return resourceService.list(RESOURCE_NAME);
    }

    /**
     * 按主键查询单条租户联系人。
     */
    public Map<String, Object> getContact(Long id) {
        return resourceService.get(RESOURCE_NAME, id);
    }

    /**
     * 新增一条租户联系人记录。
     */
    public Map<String, Object> createContact(Map<String, Object> values) {
        return resourceService.create(RESOURCE_NAME, values);
    }

    /**
     * 更新指定租户联系人记录。
     */
    public Map<String, Object> updateContact(Long id, Map<String, Object> values) {
        return resourceService.update(RESOURCE_NAME, id, values);
    }

    /**
     * 逻辑删除指定租户联系人，并记录删除原因与操作人。
     */
    public void deleteContact(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }
}
