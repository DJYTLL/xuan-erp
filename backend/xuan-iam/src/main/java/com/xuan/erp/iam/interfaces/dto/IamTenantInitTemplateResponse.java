package com.xuan.erp.iam.interfaces.dto;

import java.util.List;

/**
 * 租户初始化权限模板响应 DTO，返回模板基础信息和权限编码集合。
 */
public record IamTenantInitTemplateResponse(
        Long id,
        String code,
        String name,
        String description,
        List<String> permissionCodes,
        boolean defaultTemplate,
        boolean enabled
) {
}
