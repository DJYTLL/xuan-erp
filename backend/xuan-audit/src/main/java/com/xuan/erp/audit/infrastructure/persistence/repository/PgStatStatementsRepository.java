package com.xuan.erp.audit.infrastructure.persistence.repository;

import com.xuan.erp.audit.application.query.SqlRankingEntry;
import com.xuan.erp.audit.application.query.SqlRankingQuery;
import java.util.List;

public interface PgStatStatementsRepository {

    List<SqlRankingEntry> findRankings(SqlRankingQuery query);
}
