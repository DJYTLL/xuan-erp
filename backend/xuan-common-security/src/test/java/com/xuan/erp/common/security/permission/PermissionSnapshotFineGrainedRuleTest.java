package com.xuan.erp.common.security.permission;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 细粒度权限快照消费测试，验证业务服务可统一读取数据范围和状态动作规则。
 */
class PermissionSnapshotFineGrainedRuleTest {

    // 测试业务服务可以按资源读取数据范围，且兼容全局范围和资源范围。
    @Test
    void resolvesDataScopesForBusinessResource() {
        PermissionSnapshot snapshot = new PermissionSnapshot(
                1L,
                7L,
                "operator",
                Set.of("sales"),
                Set.of("sales-order:view"),
                Map.of(),
                Set.of("sales-order:SELF", "sales-order:DEPARTMENT", "GLOBAL_AUDIT"),
                Map.of(),
                3L);

        assertThat(snapshot.dataScopes("sales-order"))
                .containsExactlyInAnyOrder("SELF", "DEPARTMENT", "GLOBAL_AUDIT");
        assertThat(snapshot.hasDataScope("sales-order", "SELF")).isTrue();
        assertThat(snapshot.hasDataScope("purchase-order", "SELF")).isFalse();
    }

    // 测试业务服务可以按资源和状态判断动作是否允许，未配置状态默认拒绝。
    @Test
    void checksStateActionsByResourceAndState() {
        PermissionSnapshot snapshot = new PermissionSnapshot(
                1L,
                7L,
                "operator",
                Set.of("sales"),
                Set.of("sales-order:view"),
                Map.of(),
                Set.of(),
                Map.of(
                        "sales-order:DRAFT", Set.of("update", "submit"),
                        "sales-order:APPROVED", Set.of("close")),
                3L);

        assertThat(snapshot.allowedActions("sales-order", "DRAFT"))
                .containsExactlyInAnyOrder("update", "submit");
        assertThat(snapshot.isStateActionAllowed("sales-order", "DRAFT", "submit")).isTrue();
        assertThat(snapshot.isStateActionAllowed("sales-order", "APPROVED", "submit")).isFalse();
        assertThat(snapshot.isStateActionAllowed("sales-order", "ARCHIVED", "delete")).isFalse();
    }

    // 测试平台超管直接拥有全部数据范围和状态动作，仍要求 tenantId=0。
    @Test
    void letsPlatformSuperAdminConsumeAllFineGrainedRules() {
        PermissionSnapshot platformSuperAdmin = new PermissionSnapshot(
                0L,
                1L,
                "super_admin",
                Set.of("super_admin"),
                Set.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                1L);
        PermissionSnapshot tenantNamedSuperAdmin = new PermissionSnapshot(
                1L,
                2L,
                "super_admin",
                Set.of("super_admin"),
                Set.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                1L);

        assertThat(platformSuperAdmin.hasDataScope("tenant", "ANY")).isTrue();
        assertThat(platformSuperAdmin.isStateActionAllowed("tenant", "ANY", "delete")).isTrue();
        assertThat(tenantNamedSuperAdmin.hasDataScope("tenant", "ANY")).isFalse();
        assertThat(tenantNamedSuperAdmin.isStateActionAllowed("tenant", "ANY", "delete")).isFalse();
    }
}
