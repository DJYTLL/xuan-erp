package com.xuan.erp.tenant;

import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantLoginEligibility;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantDomainTest {

    @Test
    void provisionedOrEnabledTenantAllowsLoginWhenPlanIsNotExpired() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-18T20:00:00+08:00");

        assertTrue(tenant(TenantStatus.PROVISIONED).loginEligibility(now.plusDays(1), now).allowed());
        assertTrue(tenant(TenantStatus.ENABLED).loginEligibility(null, now).allowed());
    }

    @Test
    void disabledTenantDoesNotAllowLogin() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-18T20:00:00+08:00");

        TenantLoginEligibility eligibility = tenant(TenantStatus.DISABLED).loginEligibility(null, now);

        assertFalse(eligibility.allowed());
        assertEquals("租户状态不允许登录: DISABLED", eligibility.deniedReason());
    }

    @Test
    void tenantWithExpiredPlanDoesNotAllowLogin() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-18T20:00:00+08:00");

        TenantLoginEligibility eligibility = tenant(TenantStatus.ENABLED).loginEligibility(now, now);

        assertFalse(eligibility.allowed());
        assertEquals("租户套餐已到期", eligibility.deniedReason());
    }

    @Test
    void provisionedTenantCannotBeDisabledDirectly() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-18T20:00:00+08:00");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> tenant(TenantStatus.PROVISIONED).disable("直接停用", "admin", now));

        assertEquals("tenant must be enabled before disabling", error.getMessage());
    }

    private static Tenant tenant(TenantStatus status) {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-18T19:00:00+08:00");
        return new Tenant(
                6L,
                "5",
                "5",
                "5",
                status,
                "5",
                "5",
                status == TenantStatus.PROVISIONED || status == TenantStatus.ENABLED || status == TenantStatus.DISABLED
                        ? createdAt
                        : null,
                status == TenantStatus.ENABLED ? createdAt : null,
                status == TenantStatus.DISABLED ? createdAt : null,
                status == TenantStatus.DISABLED ? "停用" : null,
                null,
                "system",
                createdAt,
                "system",
                createdAt,
                null,
                null,
                null);
    }
}
