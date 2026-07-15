package com.xuan.erp.audit;

import com.xuan.erp.audit.application.query.AuditLogQuery;
import com.xuan.erp.audit.application.service.AuditLogQueryApplicationService;
import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import com.xuan.erp.audit.domain.repository.AuditLogRepository;
import com.xuan.erp.audit.interfaces.controller.AuditLogQueryController;
import com.xuan.erp.common.api.ApiResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuditLogQueryControllerTest {

    @Test
    void exposesGetAuditLogsQueryEndpoint() {
        CapturingAuditLogQueryService service = new CapturingAuditLogQueryService();
        AuditLogQueryController controller = new AuditLogQueryController(service);

        ApiResponse<List<AuditLog>> response = controller.search(
                1001L,
                "admin",
                "iam:auth:login-failed",
                "IamUser",
                "FAILED",
                "2026-07-12T10:00:00+08:00",
                "2026-07-12T11:00:00+08:00",
                20
        );

        assertEquals("SUCCESS", response.code());
        assertEquals(1001L, service.received.tenantId());
        assertEquals(AuditWriteStatus.FAILED, service.received.status());
        assertEquals("IamUser", service.received.entityType());
    }

    @Test
    void queryEndpointDeclaresExplicitRequestParamNames() throws NoSuchMethodException {
        Method search = AuditLogQueryController.class.getDeclaredMethod(
                "search",
                Long.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Integer.class
        );

        for (Parameter parameter : search.getParameters()) {
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            assertFalse(requestParam.value().isBlank(), "查询参数必须显式声明名称，避免依赖 -parameters 编译参数");
        }
    }

    private static final class CapturingAuditLogQueryService extends AuditLogQueryApplicationService {

        private AuditLogQuery received;

        private CapturingAuditLogQueryService() {
            super(new NoopAuditLogRepository());
        }

        @Override
        public List<AuditLog> search(AuditLogQuery query) {
            this.received = query;
            return List.of();
        }
    }

    private static final class NoopAuditLogRepository implements AuditLogRepository {

        @Override
        public AuditLog save(AuditLog auditLog) {
            return auditLog;
        }

        @Override
        public List<AuditLog> search(AuditLogQuery query) {
            return List.of();
        }
    }
}
