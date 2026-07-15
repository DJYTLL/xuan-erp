package com.xuan.erp.audit;

import com.xuan.erp.audit.application.command.AuditWriteCommand;
import com.xuan.erp.audit.application.result.AuditWriteResult;
import com.xuan.erp.audit.application.service.AuditWriteApplicationService;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import com.xuan.erp.audit.domain.repository.AuditLogRepository;
import com.xuan.erp.audit.interfaces.controller.AuditWriteController;
import com.xuan.erp.audit.interfaces.dto.AuditWriteRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditWriteControllerTest {

    @Test
    void postsAuditWriteRequestToApplicationService() {
        CapturingAuditWriteApplicationService service = new CapturingAuditWriteApplicationService();
        AuditWriteController controller = new AuditWriteController(service);

        var response = controller.write(new AuditWriteRequest(
                1L,
                "admin",
                100L,
                "tenant:config:update",
                "TenantConfig",
                "1",
                null,
                "success",
                "req-002",
                "127.0.0.1",
                "Mozilla",
                18L,
                "PUT",
                "/api/tenants/1/config",
                200,
                null,
                null,
                1L,
                "default",
                false
        ));

        assertEquals("SUCCESS", response.code());
        assertEquals(123L, response.data().id());
        assertEquals("tenant:config:update", service.received.action());
        assertEquals(AuditWriteStatus.SUCCESS, service.received.status());
    }

    private static final class CapturingAuditWriteApplicationService extends AuditWriteApplicationService {

        private AuditWriteCommand received;

        private CapturingAuditWriteApplicationService() {
            super(new NoopAuditLogRepository());
        }

        @Override
        public AuditWriteResult write(AuditWriteCommand command) {
            received = command;
            return new AuditWriteResult(123L, command.status());
        }
    }

    private static final class NoopAuditLogRepository implements AuditLogRepository {

        @Override
        public com.xuan.erp.audit.domain.model.AuditLog save(com.xuan.erp.audit.domain.model.AuditLog auditLog) {
            return auditLog;
        }
    }
}
