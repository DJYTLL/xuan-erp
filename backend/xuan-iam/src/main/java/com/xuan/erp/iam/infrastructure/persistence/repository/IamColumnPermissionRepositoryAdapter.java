package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.repository.IamColumnPermissionRepository;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamColumnPermissionRuleRecord;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamColumnPermissionPersistenceMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * IAM 列权限仓储适配器，负责合并当前用户所有角色的列权限规则。
 */
@Repository
public class IamColumnPermissionRepositoryAdapter implements IamColumnPermissionRepository {

    private final IamColumnPermissionPersistenceMapper mapper;

    public IamColumnPermissionRepositoryAdapter(IamColumnPermissionPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, Map<String, String>> findMergedColumnPermissionsByRoleIds(Long tenantId, List<Long> roleIds) {
        if (tenantId == null || tenantId <= 0) {
            return Map.of();
        }
        Map<String, Map<String, String>> merged = new LinkedHashMap<>();
        for (IamColumnPermissionRuleRecord rule : mapper.findColumnPermissionRulesByRoleIds(tenantId, roleIds)) {
            if (!hasText(rule.resourceKey()) || !hasText(rule.columnKey())) {
                continue;
            }
            Map<String, String> resourceRules = merged.computeIfAbsent(rule.resourceKey().trim(), ignored -> new LinkedHashMap<>());
            resourceRules.merge(rule.columnKey().trim(), normalizeAccess(rule.accessMode()), this::widerAccess);
        }
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        merged.forEach((resourceKey, columns) -> {
            Map<String, String> normalizedColumns = new LinkedHashMap<>();
            columns.forEach((columnKey, access) -> normalizedColumns.put(columnKey, normalizeAccess(access)));
            result.put(resourceKey, Map.copyOf(normalizedColumns));
        });
        return Map.copyOf(result);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeAccess(String value) {
        if (!hasText(value)) {
            return "HIDDEN";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "VISIBLE", "MASKED", "HIDDEN" -> normalized;
            default -> "HIDDEN";
        };
    }

    private String widerAccess(String current, String next) {
        return accessRank(next) > accessRank(current) ? normalizeAccess(next) : normalizeAccess(current);
    }

    private int accessRank(String value) {
        return switch (normalizeAccess(value)) {
            case "VISIBLE" -> 3;
            case "MASKED" -> 2;
            default -> 1;
        };
    }
}
