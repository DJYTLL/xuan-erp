package com.xuan.erp.audit;

import com.xuan.erp.audit.application.query.AuditLogQuery;
import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import com.xuan.erp.audit.infrastructure.persistence.entity.AuditLogRecord;
import com.xuan.erp.audit.infrastructure.persistence.mapper.AuditLogPersistenceMapper;
import com.xuan.erp.audit.infrastructure.persistence.repository.AuditLogRepositoryAdapter;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditLogRepositoryAdapterTest {

    @Test
    void savesAuditLogAndReturnsPostgresqlGeneratedId() {
        FakeAuditLogPersistenceMapper mapper = new FakeAuditLogPersistenceMapper();
        AuditLogRepositoryAdapter repository = new AuditLogRepositoryAdapter(mapper);

        AuditLog saved = repository.save(new AuditLog(
                null,
                1L,
                "admin",
                100L,
                "iam:user:create",
                "IamUser",
                "100",
                "{}",
                AuditWriteStatus.SUCCESS,
                "req-001",
                "127.0.0.1",
                "Mozilla",
                42L,
                "POST",
                "/api/iam/users",
                200,
                null,
                null,
                1L,
                "default",
                false,
                OffsetDateTime.parse("2026-07-12T09:00:00+08:00")
        ));

        assertEquals(88L, saved.id());
        assertEquals("SUCCESS", mapper.received.status());
        assertEquals("iam:user:create", mapper.received.action());
    }

    @Test
    void searchesAuditLogsAndMapsRecordsToDomain() {
        FakeAuditLogPersistenceMapper mapper = new FakeAuditLogPersistenceMapper();
        AuditLogRepositoryAdapter repository = new AuditLogRepositoryAdapter(mapper);

        List<AuditLog> logs = repository.search(new AuditLogQuery(
                1L,
                "admin",
                "iam:user:create",
                "IamUser",
                AuditWriteStatus.SUCCESS,
                null,
                null,
                20));

        assertEquals(1, logs.size());
        assertEquals(1L, mapper.query.tenantId());
        assertEquals(88L, logs.getFirst().id());
        assertEquals(AuditWriteStatus.SUCCESS, logs.getFirst().status());
    }

    private static final class FakeAuditLogPersistenceMapper implements AuditLogPersistenceMapper {

        private AuditLogRecord received;
        private AuditLogQuery query;

        @Override
        public Long insert(AuditLogRecord auditLog) {
            received = auditLog;
            return 88L;
        }

        @Override
        public List<AuditLogRecord> search(AuditLogQuery query) {
            this.query = query;
            return List.of(new AuditLogRecord(
                    88L,
                    query.tenantId(),
                    query.actorUsername(),
                    100L,
                    query.action(),
                    "IamUser",
                    "100",
                    "{}",
                    "SUCCESS",
                    "req-001",
                    "127.0.0.1",
                    "Mozilla",
                    42L,
                    "POST",
                    "/api/iam/users",
                    200,
                    null,
                    null,
                    1L,
                    "default",
                    false,
                    OffsetDateTime.parse("2026-07-12T09:00:00+08:00")
            ));
        }
    }
}
