package com.xuan.erp.tenant.application.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
/**
 * 租户域名应用服务，提供租户域名资源的增删改查操作。
 */
@Service
public class TenantDomainApplicationService {

    private static final String RESOURCE_NAME = "tenant-domains";

    private final TenantResourceApplicationService resourceService;

    /**
     * 注入底层通用租户资源应用服务。
     */
    public TenantDomainApplicationService(TenantResourceApplicationService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 查询全部租户域名记录。
     */
    public List<Map<String, Object>> listDomains() {
        return resourceService.list(RESOURCE_NAME);
    }

    /**
     * 按主键查询单条租户域名。
     */
    public Map<String, Object> getDomain(Long id) {
        return resourceService.get(RESOURCE_NAME, id);
    }

    /**
     * 新增一条租户域名记录。
     */
    public Map<String, Object> createDomain(Map<String, Object> values) {
        return resourceService.create(RESOURCE_NAME, values);
    }

    /**
     * 更新指定租户域名记录。
     */
    public Map<String, Object> updateDomain(Long id, Map<String, Object> values) {
        return resourceService.update(RESOURCE_NAME, id, values);
    }

    /**
     * 逻辑删除指定租户域名，并记录删除原因与操作人。
     */
    public void deleteDomain(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }
}
