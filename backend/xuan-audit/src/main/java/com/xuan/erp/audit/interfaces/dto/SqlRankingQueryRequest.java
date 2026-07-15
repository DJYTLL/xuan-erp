package com.xuan.erp.audit.interfaces.dto;

import com.xuan.erp.audit.application.query.SqlRankingQuery;
import java.util.Arrays;
import java.util.List;

public record SqlRankingQueryRequest(
        String databaseNames,
        String sortBy,
        Integer limit
) {

    public SqlRankingQuery toQuery() {
        List<String> names = databaseNames == null || databaseNames.isBlank()
                ? List.of()
                : Arrays.stream(databaseNames.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList();
        return SqlRankingQuery.create(names, sortBy, limit == null ? 0 : limit);
    }
}
