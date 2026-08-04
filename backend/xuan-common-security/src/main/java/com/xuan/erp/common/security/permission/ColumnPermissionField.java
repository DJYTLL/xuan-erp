package com.xuan.erp.common.security.permission;

/**
 * 响应 DTO 字段和 IAM 列权限字段的映射。
 *
 * @param columnKey IAM 列权限字段编码
 * @param componentName Java record 响应组件名
 * @param maskType 脱敏类型
 */
public record ColumnPermissionField(
        String columnKey,
        String componentName,
        ColumnMaskType maskType) {

    public ColumnPermissionField {
        if (!hasText(columnKey)) {
            throw new IllegalArgumentException("columnKey must not be blank");
        }
        if (!hasText(componentName)) {
            throw new IllegalArgumentException("componentName must not be blank");
        }
        columnKey = columnKey.trim();
        componentName = componentName.trim();
        maskType = maskType == null ? ColumnMaskType.NONE : maskType;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
