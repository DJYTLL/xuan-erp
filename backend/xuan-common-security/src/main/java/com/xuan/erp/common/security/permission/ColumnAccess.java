package com.xuan.erp.common.security.permission;

/**
 * 列权限访问级别。
 */
public enum ColumnAccess {
    HIDDEN(1),
    MASKED(2),
    VISIBLE(3);

    private final int rank;

    ColumnAccess(int rank) {
        this.rank = rank;
    }

    public static ColumnAccess parse(String value) {
        if (value == null || value.isBlank()) {
            return HIDDEN;
        }
        try {
            return ColumnAccess.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return HIDDEN;
        }
    }

    public static ColumnAccess wider(ColumnAccess current, ColumnAccess next) {
        ColumnAccess left = current == null ? HIDDEN : current;
        ColumnAccess right = next == null ? HIDDEN : next;
        return right.rank > left.rank ? right : left;
    }
}
