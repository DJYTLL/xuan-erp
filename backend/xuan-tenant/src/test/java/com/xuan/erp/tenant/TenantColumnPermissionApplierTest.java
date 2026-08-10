package com.xuan.erp.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.api.PageResult;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.permission.ColumnAccess;
import com.xuan.erp.common.security.permission.PermissionSnapshot;
import com.xuan.erp.common.security.permission.PermissionSnapshotProvider;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import com.xuan.erp.tenant.interfaces.dto.TenantResponse;
import com.xuan.erp.tenant.interfaces.security.TenantColumnPermissionApplier;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TenantColumnPermissionApplierTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appliesTenantColumnPermissionsBeforeReturningTenantListRows() {
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of("tenant_admin"), 5L, Set.of("tenant:view"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser,
                "access-token",
                Set.of()));
        PermissionSnapshotProvider provider = (user, accessToken) -> new PermissionSnapshot(
                user.tenantId(),
                user.userId(),
                user.username(),
                user.roles(),
                Set.of("tenant:view"),
                Map.of("tenant", Map.of(
                        "code", ColumnAccess.VISIBLE,
                        "name", ColumnAccess.VISIBLE,
                        "status", ColumnAccess.VISIBLE,
                        "contactName", ColumnAccess.HIDDEN,
                        "contactPhone", ColumnAccess.MASKED,
                        "currentPlanName", ColumnAccess.HIDDEN,
                        "currentPlanExpiresAt", ColumnAccess.HIDDEN,
                        "primaryDomain", ColumnAccess.HIDDEN,
                        "provisionedAt", ColumnAccess.HIDDEN,
                        "remark", ColumnAccess.HIDDEN)),
                user.authVersion());
        TenantColumnPermissionApplier applier = new TenantColumnPermissionApplier(provider);

        TenantResponse visible = applier.applyToTenantListRow(row());

        assertThat(visible.code()).isEqualTo("acme");
        assertThat(visible.name()).isEqualTo("玄云");
        assertThat(visible.status()).isEqualTo(TenantStatus.ENABLED);
        assertThat(visible.contactName()).isNull();
        assertThat(visible.contactPhone()).isEqualTo("138****0000");
        assertThat(visible.currentPlanAssignmentId()).isNull();
        assertThat(visible.currentPlanId()).isNull();
        assertThat(visible.currentPlanCode()).isNull();
        assertThat(visible.currentPlanName()).isNull();
        assertThat(visible.currentPlanExpiresAt()).isNull();
        assertThat(visible.primaryDomainId()).isNull();
        assertThat(visible.primaryDomain()).isNull();
        assertThat(visible.provisionedAt()).isNull();
        assertThat(visible.remark()).isNull();
    }

    @Test
    void serializedTenantListResponseOmitsHiddenColumnFieldsAndNeverLeaksRawSensitiveValues() throws Exception {
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of("tenant_admin"), 5L, Set.of("tenant:view"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser,
                "access-token",
                Set.of()));
        PermissionSnapshotProvider provider = (user, accessToken) -> new PermissionSnapshot(
                user.tenantId(),
                user.userId(),
                user.username(),
                user.roles(),
                Set.of("tenant:view"),
                Map.of("tenant", Map.of(
                        "code", ColumnAccess.VISIBLE,
                        "name", ColumnAccess.VISIBLE,
                        "status", ColumnAccess.VISIBLE,
                        "contactName", ColumnAccess.HIDDEN,
                        "contactPhone", ColumnAccess.MASKED,
                        "currentPlanName", ColumnAccess.HIDDEN,
                        "currentPlanExpiresAt", ColumnAccess.HIDDEN,
                        "primaryDomain", ColumnAccess.HIDDEN,
                        "provisionedAt", ColumnAccess.HIDDEN,
                        "remark", ColumnAccess.HIDDEN)),
                user.authVersion());
        TenantColumnPermissionApplier applier = new TenantColumnPermissionApplier(provider);
        TenantResponse visible = applier.applyToTenantListRow(row());

        String json = new ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(ApiResponse.success(new PageResult<>(List.of(visible), 1, 1, 20)));

        assertThat(json).contains("\"code\":\"acme\"");
        assertThat(json).contains("\"contactPhone\":\"138****0000\"");
        assertThat(json).doesNotContain("13800000000");
        assertThat(json).doesNotContain("\"contactName\"");
        assertThat(json).doesNotContain("\"currentPlanAssignmentId\"");
        assertThat(json).doesNotContain("\"currentPlanId\"");
        assertThat(json).doesNotContain("\"currentPlanCode\"");
        assertThat(json).doesNotContain("\"currentPlanName\"");
        assertThat(json).doesNotContain("\"currentPlanExpiresAt\"");
        assertThat(json).doesNotContain("\"primaryDomainId\"");
        assertThat(json).doesNotContain("\"primaryDomain\"");
        assertThat(json).doesNotContain("acme.example.com");
        assertThat(json).doesNotContain("\"provisionedAt\"");
        assertThat(json).doesNotContain("\"remark\"");
        assertThat(json).doesNotContain("内部备注");
    }

    @Test
    void canBeCreatedBySpringContainerWithPermissionSnapshotProvider() {
        assertThatNoException().isThrownBy(() -> {
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                context.registerBean(PermissionSnapshotProvider.class,
                        () -> TenantColumnPermissionApplierTest::emptySnapshot);
                context.register(TenantColumnPermissionApplier.class);
                context.refresh();

                assertThat(context.getBean(TenantColumnPermissionApplier.class)).isNotNull();
            }
        });
    }

    private static TenantResponse row() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-19T10:00:00Z");
        return new TenantResponse(
                1L,
                "acme",
                "玄云",
                TenantStatus.ENABLED,
                "张三",
                "13800000000",
                now,
                now,
                "内部备注",
                11L,
                7L,
                "standard",
                "标准版",
                now.plusMonths(1),
                21L,
                "acme.example.com",
                3L,
                "ENABLE",
                now);
    }

    private static PermissionSnapshot emptySnapshot(CurrentUser user, String accessToken) {
        return new PermissionSnapshot(
                user.tenantId(),
                user.userId(),
                user.username(),
                user.roles(),
                user.permissions(),
                Map.of(),
                user.authVersion());
    }
}
