package com.xuan.erp.audit;

import com.xuan.erp.audit.application.command.AuditWriteCommand;
import com.xuan.erp.audit.application.service.AuditWriteApplicationService;
import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import com.xuan.erp.audit.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditWriteApplicationServiceTest {

    @Test
    void writesAuditCommandThroughRepositoryAndReturnsCreatedResult() {
        InMemoryAuditLogRepository repository = new InMemoryAuditLogRepository();
        AuditWriteApplicationService service = new AuditWriteApplicationService(repository);

        var result = service.write(new AuditWriteCommand(
                1L,
                "admin",
                100L,
                "iam:user:create",
                "IamUser",
                "100",
                "{\"username\":\"demo\"}",
                AuditWriteStatus.SUCCESS,
                "req-001",
                "127.0.0.1",
                "Mozilla",
                42L,
                "post",
                "/api/iam/users",
                200,
                null,
                null,
                1L,
                "default",
                false
        ));

        assertEquals(99L, result.id());
        assertEquals(AuditWriteStatus.SUCCESS, result.status());
        assertEquals("iam:user:create", repository.saved.action());
        assertEquals("POST", repository.saved.method());
        assertEquals("/api/iam/users", repository.saved.path());
    }

    private static final class InMemoryAuditLogRepository implements AuditLogRepository {

        private AuditLog saved;

        @Override
        public AuditLog save(AuditLog auditLog) {
            saved = auditLog;
            return auditLog.withId(99L);
        }
    }
}
