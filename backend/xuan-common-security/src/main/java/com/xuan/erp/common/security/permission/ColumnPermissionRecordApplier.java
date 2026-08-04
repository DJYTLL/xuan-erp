package com.xuan.erp.common.security.permission;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于 IAM 列权限快照裁剪 Java record 响应 DTO。
 */
public class ColumnPermissionRecordApplier {

    public <T> List<T> applyList(
            PermissionSnapshot snapshot,
            String resourceKey,
            Collection<T> rows,
            Collection<ColumnPermissionField> fields) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> apply(snapshot, resourceKey, row, fields))
                .toList();
    }

    public <T> T apply(
            PermissionSnapshot snapshot,
            String resourceKey,
            T row,
            Collection<ColumnPermissionField> fields) {
        if (row == null || snapshot == null || !snapshot.hasColumnRules(resourceKey) || fields == null || fields.isEmpty()) {
            return row;
        }
        Class<?> rowType = row.getClass();
        if (!rowType.isRecord()) {
            throw new IllegalArgumentException("column permission response applier only supports Java record DTOs: " + rowType.getName());
        }
        Map<String, ColumnPermissionField> fieldByComponent = fields.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ColumnPermissionField::componentName,
                        Function.identity(),
                        (left, right) -> right));
        if (fieldByComponent.isEmpty()) {
            return row;
        }
        return instantiateRecord(row, rowType, componentValues(snapshot, resourceKey, row, rowType, fieldByComponent));
    }

    private <T> Object[] componentValues(
            PermissionSnapshot snapshot,
            String resourceKey,
            T row,
            Class<?> rowType,
            Map<String, ColumnPermissionField> fieldByComponent) {
        RecordComponent[] components = rowType.getRecordComponents();
        Object[] values = new Object[components.length];
        for (int index = 0; index < components.length; index++) {
            RecordComponent component = components[index];
            Object value = readComponent(row, component);
            ColumnPermissionField field = fieldByComponent.get(component.getName());
            values[index] = field == null
                    ? value
                    : applyAccess(value, snapshot.columnAccess(resourceKey, field.columnKey()), field.maskType(), component.getType());
        }
        return values;
    }

    private Object applyAccess(Object value, ColumnAccess access, ColumnMaskType maskType, Class<?> targetType) {
        if (access == ColumnAccess.HIDDEN) {
            return hiddenValue(targetType);
        }
        if (access == ColumnAccess.MASKED) {
            return mask(value, maskType);
        }
        return value;
    }

    private Object hiddenValue(Class<?> targetType) {
        if (!targetType.isPrimitive()) {
            return null;
        }
        if (targetType == boolean.class) {
            return false;
        }
        if (targetType == char.class) {
            return '\0';
        }
        if (targetType == byte.class) {
            return (byte) 0;
        }
        if (targetType == short.class) {
            return (short) 0;
        }
        if (targetType == int.class) {
            return 0;
        }
        if (targetType == long.class) {
            return 0L;
        }
        if (targetType == float.class) {
            return 0F;
        }
        if (targetType == double.class) {
            return 0D;
        }
        return null;
    }

    private Object mask(Object value, ColumnMaskType maskType) {
        if (!(value instanceof String text) || text.isBlank()) {
            return value;
        }
        return switch (maskType == null ? ColumnMaskType.DEFAULT : maskType) {
            case PHONE -> maskPhone(text);
            case EMAIL -> maskEmail(text);
            case NONE, DEFAULT -> defaultMask(text);
        };
    }

    private String maskPhone(String value) {
        if (value.length() < 7) {
            return defaultMask(value);
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String maskEmail(String value) {
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            return defaultMask(value);
        }
        String localPart = value.substring(0, atIndex);
        String domain = value.substring(atIndex);
        if (localPart.length() == 1) {
            return "*" + domain;
        }
        return localPart.charAt(0) + "***" + domain;
    }

    private String defaultMask(String value) {
        return "*".repeat(Math.min(Math.max(value.length(), 1), 6));
    }

    private Object readComponent(Object row, RecordComponent component) {
        try {
            return component.getAccessor().invoke(row);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("failed to read record component " + component.getName(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiateRecord(T row, Class<?> rowType, Object[] values) {
        RecordComponent[] components = rowType.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        for (int index = 0; index < components.length; index++) {
            parameterTypes[index] = components[index].getType();
        }
        try {
            Constructor<?> constructor = rowType.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(values);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("failed to create column permission response record " + rowType.getName(), ex);
        }
    }
}
