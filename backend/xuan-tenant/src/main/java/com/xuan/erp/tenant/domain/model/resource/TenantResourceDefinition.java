package com.xuan.erp.tenant.domain.model.resource;

import java.util.List;

public record TenantResourceDefinition(
        String resourceName,
        String tableName,
        List<String> writableColumns,
        boolean supportsSoftDelete
) {
}
