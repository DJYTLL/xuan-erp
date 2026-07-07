package com.xuan.erp.tenant.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.domain.repository.TenantResourceRepository;
import com.xuan.erp.tenant.domain.service.TenantResourceCatalog;
import com.xuan.erp.tenant.domain.model.resource.TenantResourceDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TenantResourceApplicationService {

    private final TenantResourceCatalog catalog;
    private final TenantResourceRepository repository;

    @Autowired
    public TenantResourceApplicationService(TenantResourceRepository repository) {
        this(TenantResourceCatalog.defaultCatalog(), repository);
    }

    public TenantResourceApplicationService(TenantResourceCatalog catalog, TenantResourceRepository repository) {
        this.catalog = catalog;
        this.repository = repository;
    }

    public List<Map<String, Object>> list(String resourceName) {
        return repository.list(requireResource(resourceName));
    }

    public Map<String, Object> get(String resourceName, Long id) {
        TenantResourceDefinition resource = requireResource(resourceName);
        return repository.findById(resource, id)
                .orElseThrow(() -> new BusinessException("TENANT_RESOURCE_NOT_FOUND", "资源不存在"));
    }

    public Map<String, Object> create(String resourceName, Map<String, Object> values) {
        TenantResourceDefinition resource = requireResource(resourceName);
        return repository.create(resource, sanitize(resource, values));
    }

    public Map<String, Object> update(String resourceName, Long id, Map<String, Object> values) {
        TenantResourceDefinition resource = requireResource(resourceName);
        return repository.update(resource, id, sanitize(resource, values));
    }

    public void delete(String resourceName, Long id, String reason, String operator) {
        TenantResourceDefinition resource = requireResource(resourceName);
        if (!resource.supportsSoftDelete()) {
            throw new BusinessException("TENANT_RESOURCE_DELETE_UNSUPPORTED", "当前资源不支持软删除");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("TENANT_DELETE_REASON_REQUIRED", "删除原因不能为空");
        }
        repository.softDelete(resource, id, reason.trim(), operator == null || operator.isBlank() ? "system" : operator.trim());
    }

    private TenantResourceDefinition requireResource(String resourceName) {
        return catalog.find(resourceName)
                .orElseThrow(() -> new BusinessException("TENANT_RESOURCE_UNSUPPORTED", "不支持的租户资源：" + resourceName));
    }

    private Map<String, Object> sanitize(TenantResourceDefinition resource, Map<String, Object> values) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (String column : resource.writableColumns()) {
            String camelKey = toCamelCase(column);
            if (values.containsKey(camelKey)) {
                sanitized.put(column, values.get(camelKey));
            } else if (values.containsKey(column)) {
                sanitized.put(column, values.get(column));
            }
        }
        return sanitized;
    }

    private String toCamelCase(String column) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char ch : column.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
