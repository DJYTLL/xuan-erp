package com.xuan.erp.common.security.audit;

import com.xuan.erp.common.audit.AuditWriteOutcome;
import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditEventFactoryTest {

    // 测试 token 异常会被标准化为失败审计事件，且不会记录 token 明文。
    @Test
    void createsTokenInvalidAuditEventWithoutTokenValue() {
        SecurityAuditEventFactory factory = new SecurityAuditEventFactory();

        var event = factory.failure(SecurityAuditFailure.TOKEN_INVALID, new SecurityAuditRequest(
                null,
                null,
                "req-001",
                "127.0.0.1",
                "Mozilla",
                "GET",
                "/api/tenant/configs",
                "JWT token format is invalid"
        ));

        assertThat(event.action()).isEqualTo("security:token:invalid");
        assertThat(event.tenantId()).isEqualTo(0L);
        assertThat(event.entityType()).isEqualTo("GatewaySecurity");
        assertThat(event.status()).isEqualTo(AuditWriteOutcome.FAILED);
        assertThat(event.httpStatus()).isEqualTo(401);
        assertThat(event.errorCode()).isEqualTo("SECURITY_TOKEN_INVALID");
        assertThat(event.errorMessage()).isEqualTo("JWT token format is invalid");
        assertThat(event.detail()).doesNotContain("Bearer");
    }

    // 测试权限拒绝会保留当前登录人和认证租户，方便审计页追溯是谁被拒绝。
    @Test
    void createsPermissionDeniedAuditEventWithCurrentUser() {
        SecurityAuditEventFactory factory = new SecurityAuditEventFactory();
        CurrentUser currentUser = new CurrentUser(
                11L,
                1001L,
                "tenant-admin",
                Set.of("tenant_admin"),
                7L,
                Set.of("iam:view")
        );

        var event = factory.failure(SecurityAuditFailure.PERMISSION_DENIED, new SecurityAuditRequest(
                currentUser,
                1001L,
                "req-002",
                "127.0.0.1",
                "Mozilla",
                "POST",
                "/api/iam/users",
                "Access Denied"
        ));

        assertThat(event.action()).isEqualTo("security:permission:denied");
        assertThat(event.tenantId()).isEqualTo(1001L);
        assertThat(event.actorUserId()).isEqualTo(11L);
        assertThat(event.actorUsername()).isEqualTo("tenant-admin");
        assertThat(event.authTenantId()).isEqualTo(1001L);
        assertThat(event.crossTenant()).isFalse();
        assertThat(event.httpStatus()).isEqualTo(403);
        assertThat(event.errorCode()).isEqualTo("SECURITY_PERMISSION_DENIED");
    }

    // 测试请求租户和 token 租户不一致时，审计事件会明确标记跨租户风险。
    @Test
    void marksTenantContextMismatchAsCrossTenantAuditEvent() {
        SecurityAuditEventFactory factory = new SecurityAuditEventFactory();
        CurrentUser currentUser = new CurrentUser(11L, 1001L, "tenant-admin", Set.of("iam:view"));

        var event = factory.failure(SecurityAuditFailure.TENANT_CONTEXT_INVALID, new SecurityAuditRequest(
                currentUser,
                2002L,
                "req-003",
                "127.0.0.1",
                "Mozilla",
                "GET",
                "/api/tenants/2002/configs",
                "请求租户与 token 租户不一致"
        ));

        assertThat(event.action()).isEqualTo("security:tenant-context:invalid");
        assertThat(event.tenantId()).isEqualTo(2002L);
        assertThat(event.authTenantId()).isEqualTo(1001L);
        assertThat(event.crossTenant()).isTrue();
        assertThat(event.httpStatus()).isEqualTo(403);
        assertThat(event.errorCode()).isEqualTo("SECURITY_TENANT_CONTEXT_INVALID");
    }
}
