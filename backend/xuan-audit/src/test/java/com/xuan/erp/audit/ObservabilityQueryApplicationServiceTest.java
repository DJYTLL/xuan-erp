package com.xuan.erp.audit;

import com.xuan.erp.audit.application.query.InterfaceTraceDetail;
import com.xuan.erp.audit.application.query.InterfaceTraceQuery;
import com.xuan.erp.audit.application.query.InterfaceTraceSummary;
import com.xuan.erp.audit.application.query.SqlRankingEntry;
import com.xuan.erp.audit.application.query.SqlRankingQuery;
import com.xuan.erp.audit.application.service.ObservabilityQueryApplicationService;
import com.xuan.erp.audit.infrastructure.persistence.repository.PgStatStatementsRepository;
import com.xuan.erp.audit.infrastructure.rpc.SkyWalkingOapClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservabilityQueryApplicationServiceTest {

    @Test
    void listsInterfaceTracesThroughSkyWalkingOapClient() {
        FakeSkyWalkingOapClient oapClient = new FakeSkyWalkingOapClient();
        ObservabilityQueryApplicationService service = new ObservabilityQueryApplicationService(
                oapClient,
                query -> List.of()
        );

        List<InterfaceTraceSummary> traces = service.listInterfaceTraces(
                new InterfaceTraceQuery("xuan-iam", "/api/iam/auth/login", "2026-07-11 10:00", "2026-07-11 11:00", 20)
        );

        assertEquals(1, traces.size());
        assertEquals("trace-001", traces.getFirst().traceId());
        assertEquals("xuan-iam", oapClient.receivedQuery.serviceName());
    }

    @Test
    void loadsTraceDetailThroughSkyWalkingOapClient() {
        ObservabilityQueryApplicationService service = new ObservabilityQueryApplicationService(
                new FakeSkyWalkingOapClient(),
                query -> List.of()
        );

        InterfaceTraceDetail detail = service.getInterfaceTrace("trace-001");

        assertEquals("trace-001", detail.traceId());
        assertEquals(1, detail.spans().size());
    }

    @Test
    void queriesSqlRankingsFromPgStatStatementsWithDefaultDatabases() {
        FakePgStatStatementsRepository repository = new FakePgStatStatementsRepository();
        ObservabilityQueryApplicationService service = new ObservabilityQueryApplicationService(
                new FakeSkyWalkingOapClient(),
                repository
        );

        List<SqlRankingEntry> rankings = service.listSqlRankings(SqlRankingQuery.create(null, null, 0));

        assertEquals(List.of("xuan_iam", "xuan_tenant", "xuan_audit"), repository.receivedQuery.databaseNames());
        assertEquals("total_exec_time", repository.receivedQuery.sortBy());
        assertEquals(1, rankings.size());
        assertEquals("xuan_iam", rankings.getFirst().databaseName());
    }

    private static final class FakeSkyWalkingOapClient implements SkyWalkingOapClient {

        private InterfaceTraceQuery receivedQuery;

        @Override
        public List<InterfaceTraceSummary> listTraces(InterfaceTraceQuery query) {
            receivedQuery = query;
            return List.of(new InterfaceTraceSummary(
                    "trace-001",
                    query.serviceName(),
                    query.endpointName(),
                    42,
                    query.startTime(),
                    false
            ));
        }

        @Override
        public InterfaceTraceDetail getTrace(String traceId) {
            return new InterfaceTraceDetail(traceId, new ArrayList<>(List.of(
                    new com.xuan.erp.audit.application.query.InterfaceSpanDetail(
                            "0", "-1", "xuan-iam", "/api/iam/auth/login", "Entry", 42, false
                    )
            )));
        }
    }

    private static final class FakePgStatStatementsRepository implements PgStatStatementsRepository {

        private SqlRankingQuery receivedQuery;

        @Override
        public List<SqlRankingEntry> findRankings(SqlRankingQuery query) {
            receivedQuery = query;
            return List.of(new SqlRankingEntry("xuan_iam", "select 1", 10, 120.5, 12.05, 30.2, 10));
        }
    }
}
