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

/**
 * 通用租户资源应用服务，负责按资源目录解析资源定义并执行通用 CRUD 操作。
 */
@Service
public class TenantResourceApplicationService {

    private final TenantResourceCatalog catalog;
    private final TenantResourceRepository repository;

    /**
     * 使用默认资源目录初始化通用资源应用服务。
     */
    @Autowired
    public TenantResourceApplicationService(TenantResourceRepository repository) {
        this(TenantResourceCatalog.defaultCatalog(), repository);
    }

    /**
     * 注入资源目录与资源仓储，便于在测试或扩展场景下替换目录定义。
     */
    public TenantResourceApplicationService(TenantResourceCatalog catalog, TenantResourceRepository repository) {
        this.catalog = catalog;
        this.repository = repository;
    }

    /**
     * 查询指定资源类型下的全部记录。
     */
    public List<Map<String, Object>> list(String resourceName) {
        return repository.list(requireResource(resourceName));
    }

    /**
     * 按主键查询指定资源类型的一条记录。
     */
    public Map<String, Object> get(String resourceName, Long id) {
        TenantResourceDefinition resource = requireResource(resourceName);
        return repository.findById(resource, id)
                .orElseThrow(() -> new BusinessException("TENANT_RESOURCE_NOT_FOUND", "资源不存在"));
    }

    /**
     * 创建指定资源类型的新记录，并先按可写字段白名单清洗输入数据。
     */
    public Map<String, Object> create(String resourceName, Map<String, Object> values) {
        TenantResourceDefinition resource = requireResource(resourceName);
        return repository.create(resource, sanitize(resource, values));
    }

    /**
     * 更新指定资源类型的记录，并先按可写字段白名单清洗输入数据。
     */
    public Map<String, Object> update(String resourceName, Long id, Map<String, Object> values) {
        TenantResourceDefinition resource = requireResource(resourceName);
        return repository.update(resource, id, sanitize(resource, values));
    }

    /**
     * 对支持软删除的资源执行逻辑删除，并校验删除原因与操作人。
     */
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

    /**
     * 根据资源名称查找资源定义，不支持的资源名称会抛出业务异常。
     */
    private TenantResourceDefinition requireResource(String resourceName) {
        return catalog.find(resourceName)
                .orElseThrow(() -> new BusinessException("TENANT_RESOURCE_UNSUPPORTED", "不支持的租户资源：" + resourceName));
    }

    /**
     * 根据资源定义的可写字段白名单过滤输入值，同时兼容 snake_case 与 camelCase 键名。
     */
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

    /**
     * 将下划线命名的列名转换为驼峰命名，便于兼容前端常见传参风格。
     */
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
