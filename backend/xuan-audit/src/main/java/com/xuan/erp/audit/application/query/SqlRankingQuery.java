package com.xuan.erp.audit.application.query;

import java.util.List;
import java.util.Set;

public record SqlRankingQuery(List<String> databaseNames, String sortBy, int limit) {

    private static final List<String> DEFAULT_DATABASES = List.of("xuan_iam", "xuan_tenant", "xuan_audit");
    private static final Set<String> ALLOWED_DATABASES = Set.of(
            "xuan_gateway",
            "xuan_tenant",
            "xuan_iam",
            "xuan_audit",
            "xuan_product",
            "xuan_party",
            "xuan_warehouse",
            "xuan_inventory",
            "xuan_sales",
            "xuan_procurement",
            "xuan_finance",
            "xuan_document",
            "xuan_manufacturing",
            "xuan_query"
    );
    private static final Set<String> ALLOWED_SORTS = Set.of(
            "total_exec_time",
            "mean_exec_time",
            "max_exec_time",
            "calls",
            "rows"
    );

    public static SqlRankingQuery create(List<String> databaseNames, String sortBy, int limit) {
        List<String> normalizedDatabases = normalizeDatabases(databaseNames);
        String normalizedSort = normalizeSort(sortBy);
        int normalizedLimit = normalizeLimit(limit);
        return new SqlRankingQuery(normalizedDatabases, normalizedSort, normalizedLimit);
    }

    private static List<String> normalizeDatabases(List<String> databaseNames) {
        if (databaseNames == null || databaseNames.isEmpty()) {
            return DEFAULT_DATABASES;
        }
        List<String> normalized = databaseNames.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return DEFAULT_DATABASES;
        }
        for (String databaseName : normalized) {
            if (!ALLOWED_DATABASES.contains(databaseName)) {
                throw new IllegalArgumentException("不支持的数据库：" + databaseName);
            }
        }
        return normalized;
    }

    private static String normalizeSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "total_exec_time";
        }
        String normalized = sortBy.trim();
        if (!ALLOWED_SORTS.contains(normalized)) {
            throw new IllegalArgumentException("不支持的 SQL 排名排序字段：" + sortBy);
        }
        return normalized;
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
