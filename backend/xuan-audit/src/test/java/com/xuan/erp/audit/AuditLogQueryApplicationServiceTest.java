package com.xuan.erp.audit;

import com.xuan.erp.audit.application.query.AuditLogQuery;
import com.xuan.erp.audit.application.service.AuditLogQueryApplicationService;
import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import com.xuan.erp.audit.domain.repository.AuditLogRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditLogQueryApplicationServiceTest {

    @Test
    void searchesAuditLogsThroughRepositoryWithNormalizedLimit() {
        InMemoryAuditLogRepository repository = new InMemoryAuditLogRepository();
        AuditLogQueryApplicationService service = new AuditLogQueryApplicationService(repository);

        List<AuditLog> result = service.search(new AuditLogQuery(
                1001L,
                " admin ",
                " iam:auth:login-success ",
                "IamUser",
                AuditWriteStatus.SUCCESS,
                OffsetDateTime.parse("2026-07-12T10:00:00+08:00"),
                OffsetDateTime.parse("2026-07-12T11:00:00+08:00"),
                500
        ));

        assertEquals(1, result.size());
        assertEquals(100, repository.received.limit());
        assertEquals("admin", repository.received.actorUsername());
        assertEquals("iam:auth:login-success", repository.received.action());
    }

    private static final class InMemoryAuditLogRepository implements AuditLogRepository {

        private AuditLogQuery received;

        @Override
        public AuditLog save(AuditLog auditLog) {
            return auditLog.withId(1L);
        }

        @Override
        public List<AuditLog> search(AuditLogQuery query) {
            this.received = query;
            return List.of(new AuditLog(
                    1L,
                    query.tenantId(),
                    query.actorUsername(),
                    1L,
                    query.action(),
                    "IamUser",
                    "1",
                    null,
                    AuditWriteStatus.SUCCESS,
                    null,
                    null,
                    null,
                    null,
                    "POST",
                    "/api/iam/auth/login",
                    null,
                    null,
                    null,
                    query.tenantId(),
                    null,
                    false,
                    OffsetDateTime.parse("2026-07-12T10:30:00+08:00")));
        }
    }
}
