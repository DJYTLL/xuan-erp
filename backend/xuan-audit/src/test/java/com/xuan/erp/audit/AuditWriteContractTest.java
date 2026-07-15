package com.xuan.erp.audit;

import com.xuan.erp.audit.application.command.AuditActionNames;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import com.xuan.erp.audit.interfaces.dto.AuditWriteRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditWriteContractTest {

    @Test
    void auditWriteRequestConvertsInboundDtoToCommandWithNormalizedStatus() {
        var command = new AuditWriteRequest(
                1L,
                "admin",
                100L,
                "iam:user:create",
                "IamUser",
                "100",
                "{\"username\":\"demo\"}",
                "failed",
                "req-001",
                "127.0.0.1",
                "Mozilla",
                42L,
                "POST",
                "/api/iam/users",
                500,
                "IAM_USER_EXISTS",
                "用户已存在",
                1L,
                "default",
                false
        ).toCommand();

        assertEquals(1L, command.tenantId());
        assertEquals("iam:user:create", command.action());
        assertEquals("IamUser", command.entityType());
        assertEquals(AuditWriteStatus.FAILED, command.status());
        assertEquals("POST", command.method());
        assertEquals("/api/iam/users", command.path());
    }

    @Test
    void auditWriteRequestDefaultsBlankStatusToSuccess() {
        var command = new AuditWriteRequest(
                1L,
                "admin",
                100L,
                "tenant:config:update",
                "TenantConfig",
                "1",
                null,
                " ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1L,
                "default",
                false
        ).toCommand();

        assertEquals(AuditWriteStatus.SUCCESS, command.status());
    }

    @Test
    void auditWriteRequestAcceptsGatewaySecurityEventWithSystemTenant() {
        var command = new AuditWriteRequest(
                0L,
                null,
                null,
                "security:authentication:missing",
                "GatewaySecurity",
                "/api/test/secured",
                "{\"failureType\":\"AUTHENTICATION_MISSING\"}",
                "failed",
                "req-missing-token",
                "127.0.0.1",
                "Mozilla",
                null,
                "GET",
                "/api/test/secured",
                401,
                "SECURITY_AUTHENTICATION_MISSING",
                "Not Authenticated",
                null,
                null,
                false
        ).toCommand();

        assertEquals(0L, command.tenantId());
        assertEquals("security:authentication:missing", command.action());
        assertEquals("GatewaySecurity", command.entityType());
        assertEquals(AuditWriteStatus.FAILED, command.status());
        assertEquals("SECURITY_AUTHENTICATION_MISSING", command.errorCode());
    }

    @Test
    void auditWriteStatusParsesCanonicalCodesCaseInsensitively() {
        assertEquals(AuditWriteStatus.SUCCESS, AuditWriteStatus.fromCode("success"));
        assertEquals(AuditWriteStatus.FAILED, AuditWriteStatus.fromCode("FAILED"));
    }

    @Test
    void auditActionNamesRequireDomainResourceAndVerbSegments() {
        assertEquals("sales:order:submit", AuditActionNames.normalize(" sales:order:submit "));

        assertThrows(IllegalArgumentException.class, () -> AuditActionNames.normalize("sales:submit"));
        assertThrows(IllegalArgumentException.class, () -> AuditActionNames.normalize("Sales:Order:Submit"));
        assertThrows(IllegalArgumentException.class, () -> AuditActionNames.normalize("sales:order:submit!"));
        assertThrows(IllegalArgumentException.class, () -> AuditActionNames.normalize(
                "sales:order:" + "submit".repeat(20)
        ));
    }
}
