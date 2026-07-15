package com.xuan.erp.audit;

import com.xuan.erp.audit.application.query.SqlRankingQuery;
import com.xuan.erp.audit.interfaces.dto.InterfaceTraceQueryRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditObservabilityQueryContractTest {

    @Test
    void sqlRankingQueryDefaultsToFirstLoopDatabasesAndTotalTimeSorting() {
        SqlRankingQuery query = SqlRankingQuery.create(null, null, 0);

        assertEquals(List.of("xuan_iam", "xuan_tenant", "xuan_audit"), query.databaseNames());
        assertEquals("total_exec_time", query.sortBy());
        assertEquals(20, query.limit());
    }

    @Test
    void sqlRankingQueryRejectsUnsupportedDatabaseNamesAndSortColumns() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlRankingQuery.create(List.of("postgres"), "total_exec_time", 20));
        assertThrows(IllegalArgumentException.class,
                () -> SqlRankingQuery.create(List.of("xuan_iam"), "query;drop table", 20));
    }

    @Test
    void interfaceTraceQueryRequestDefaultsToRecentWindowAndSafeLimit() {
        var query = new InterfaceTraceQueryRequest(null, null, null, null, 0).toQuery();

        assertNotNull(query.startTime());
        assertNotNull(query.endTime());
        assertEquals(20, query.limit());
    }

    @Test
    void interfaceTraceQueryRequestFormatsFrontendDateTimeToSkyWalkingUtcMinuteDuration() {
        var query = new InterfaceTraceQueryRequest(
                "xuan-iam",
                null,
                "2026-07-12 01:02:03",
                "2026-07-12 02:03:04",
                10
        ).toQuery();

        assertEquals("2026-07-11 1702", query.startTime());
        assertEquals("2026-07-11 1803", query.endTime());
    }
}
