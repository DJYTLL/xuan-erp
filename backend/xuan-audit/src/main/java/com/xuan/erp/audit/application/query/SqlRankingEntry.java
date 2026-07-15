package com.xuan.erp.audit.application.query;

public record SqlRankingEntry(
        String databaseName,
        String query,
        long calls,
        double totalExecTime,
        double meanExecTime,
        double maxExecTime,
        long rows
) {
}
