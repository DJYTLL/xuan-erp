package com.xuan.erp.audit.application.service;

import com.xuan.erp.audit.application.query.InterfaceTraceDetail;
import com.xuan.erp.audit.application.query.InterfaceTraceQuery;
import com.xuan.erp.audit.application.query.InterfaceTraceSummary;
import com.xuan.erp.audit.application.query.SqlRankingEntry;
import com.xuan.erp.audit.application.query.SqlRankingQuery;
import com.xuan.erp.audit.infrastructure.persistence.repository.PgStatStatementsRepository;
import com.xuan.erp.audit.infrastructure.rpc.SkyWalkingOapClient;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ObservabilityQueryApplicationService {

    private final SkyWalkingOapClient skyWalkingOapClient;
    private final PgStatStatementsRepository pgStatStatementsRepository;

    public ObservabilityQueryApplicationService(
            SkyWalkingOapClient skyWalkingOapClient,
            PgStatStatementsRepository pgStatStatementsRepository
    ) {
        this.skyWalkingOapClient = skyWalkingOapClient;
        this.pgStatStatementsRepository = pgStatStatementsRepository;
    }

    public List<InterfaceTraceSummary> listInterfaceTraces(InterfaceTraceQuery query) {
        return skyWalkingOapClient.listTraces(query);
    }

    public InterfaceTraceDetail getInterfaceTrace(String traceId) {
        return skyWalkingOapClient.getTrace(traceId);
    }

    public List<SqlRankingEntry> listSqlRankings(SqlRankingQuery query) {
        return pgStatStatementsRepository.findRankings(query);
    }
}
