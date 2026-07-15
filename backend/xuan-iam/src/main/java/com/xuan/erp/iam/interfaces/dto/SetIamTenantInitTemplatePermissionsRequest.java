package com.xuan.erp.iam.interfaces.dto;

import java.util.List;

/**
 * 保存租户初始化模板权限请求 DTO，承载替换后的权限编码集合。
 */
public record SetIamTenantInitTemplatePermissionsRequest(
        List<String> permissionCodes,
        String operator
) {
}
