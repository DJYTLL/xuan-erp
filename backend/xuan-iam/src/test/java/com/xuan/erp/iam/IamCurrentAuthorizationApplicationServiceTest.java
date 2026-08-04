package com.xuan.erp.iam;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.query.IamCurrentMenuNodeView;
import com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView;
import com.xuan.erp.iam.application.service.IamCurrentAuthorizationApplicationService;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamTenantPermissionEntitlementRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamCurrentAuthorizationApplicationServiceTest {

    @Test
    void buildsCurrentPermissionSnapshotFromCurrentUserAndAuthorizationSnapshot() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(2L, "system", null, "系统", "/system", "iam:view", 120),
                menu(1L, "product", null, "商品", "/product", "product:view", 20),
                menu(3L, "product-sku", 1L, "商品档案", "/product/sku", "product:view", 10));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"),
                permission("product:view", "商品查看", "xuan-product", "product"));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository(
                snapshot(1L, 1001L, 7L, List.of("iam:view", "product:view"), List.of("system", "product", "product-sku")));

        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                snapshotRepository,
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                entitlements(1L, "iam:view", "product:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 1L, "tenant_admin", Set.of("tenant_admin"), 7L, Set.of("iam:view", "product:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(7L, view.authVersion());
        assertEquals(List.of("iam:view", "product:view"), view.routePermissions());
        assertEquals(List.of("iam:view", "product:view"), view.buttonPermissions());
        assertEquals(List.of("product", "system"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
        assertEquals(List.of("product-sku"), view.menus().get(0).children().stream().map(IamCurrentMenuNodeView::code).toList());
        assertTrue(view.fieldPermissions().isEmpty());
        assertTrue(view.dataScopes().isEmpty());
        assertTrue(view.stateActionRules().isEmpty());
    }

    @Test
    void fallsBackToCurrentUserPermissionsWhenSnapshotMissing() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "system", null, "系统", "/system", "iam:view", 120),
                menu(2L, "product", null, "商品", "/product", "product:view", 20),
                menu(3L, "audit", null, "审计", "/audit", "audit:view", 130));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"),
                permission("product:view", "商品查看", "xuan-product", "product"),
                permission("audit:view", "审计查看", "xuan-audit", "audit"));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                entitlements(1L, "iam:view", "product:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 1L, "tenant_admin", Set.of("tenant_admin"), 9L, Set.of("product:view", "iam:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(9L, view.authVersion());
        assertEquals(List.of("iam:view", "product:view"), view.routePermissions());
        assertEquals(List.of("product", "system"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
        assertEquals(Map.of(), view.columnPermissions());
    }

    @Test
    void usesEmptySnapshotPermissionsInsteadOfStaleCurrentUserPermissions() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "system", null, "系统", "/system", "iam:view", 120));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository(
                snapshot(1L, 1001L, 10L, List.of(), List.of()));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                snapshotRepository,
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                entitlements(1L, "iam:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 1L, "tenant_admin", Set.of("tenant_admin"), 9L, Set.of("iam:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(10L, view.authVersion());
        assertEquals(List.of(), view.routePermissions());
        assertEquals(List.of(), view.buttonPermissions());
        assertEquals(List.of(), view.menus());
    }

    @Test
    void includesPermissionDerivedMenusWhenSnapshotMenuCodesAreStale() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "system", null, "系统", "/system", "iam:view", 120),
                menu(2L, "iam-role-management", 1L, "角色授权", "/system/iam/roles", "iam:view", 123),
                menu(3L, "iam-user-management", 1L, "用户授权", "/system/iam/users", "iam:view", 124));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository(
                snapshot(1L, 1001L, 11L, List.of("iam:view"), List.of("system", "iam-role-management")));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                snapshotRepository,
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                entitlements(1L, "iam:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 1L, "tenant_admin", Set.of("tenant_admin"), 11L, Set.of("iam:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(List.of("system"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
        assertEquals(
                List.of("iam-role-management", "iam-user-management"),
                view.menus().get(0).children().stream().map(IamCurrentMenuNodeView::code).toList());
    }

    @Test
    void filtersSnapshotMenuCodesByCurrentPermissions() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "inventory-root", null, "进销存", null, null, 20),
                menu(2L, "base-data", 1L, "基础资料", null, null, 10),
                menu(3L, "product-management", 2L, "商品管理", "/inventory/products", "product:view", 10),
                menu(4L, "document", null, "打印管理", "/document", "document:view", 90),
                menu(5L, "report", null, "报表中心", "/report", "query:view", 100));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("product:view", "商品查看", "xuan-product", "product-management"),
                permission("document:view", "打印查看", "xuan-document", "document"),
                permission("query:view", "报表查看", "xuan-query", "report"));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository(
                snapshot(1L, 1001L, 12L, List.of("product:view"), List.of(
                        "inventory-root",
                        "base-data",
                        "product-management",
                        "document",
                        "report")));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                snapshotRepository,
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                entitlements(1L, "product:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 1L, "tenant_user", Set.of("tenant_user"), 12L, Set.of("product:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(List.of("inventory-root"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
        assertEquals(List.of("base-data"), view.menus().get(0).children().stream().map(IamCurrentMenuNodeView::code).toList());
        assertEquals(
                List.of("product-management"),
                view.menus().get(0).children().get(0).children().stream().map(IamCurrentMenuNodeView::code).toList());
    }

    @Test
    void omitsPermissionlessGroupsWhenNoVisibleChildMenus() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "workbench", null, "工作台", "/workbench", null, 1),
                menu(2L, "system", null, "系统设置", null, null, 10),
                menu(3L, "tenant-management", 2L, "租户管理", "/system/tenants", "tenant:view", 10),
                menu(4L, "inventory-root", null, "进销存", null, null, 20),
                menu(5L, "product-management", 4L, "商品管理", "/inventory/products", "product:view", 10));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("tenant:view", "租户查看", "xuan-tenant", "tenant-management"),
                permission("product:view", "商品查看", "xuan-product", "product-management"));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                entitlements(1L, "product:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 1L, "tenant_user", Set.of("tenant_user"), 12L, Set.of("product:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(List.of("workbench", "inventory-root"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
        assertEquals(List.of("product-management"), view.menus().get(1).children().stream().map(IamCurrentMenuNodeView::code).toList());
    }

    @Test
    void grantsAllActivePermissionsToPlatformSuperAdminSnapshot() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "system", null, "系统", "/system", "iam:view", 120),
                menu(2L, "product", null, "商品", "/product", "product:view", 20));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"),
                permission("product:view", "商品查看", "xuan-product", "product"),
                permission("product:create", "商品新增", "xuan-product", "product"));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                new InMemoryTenantPermissionEntitlementRepository());

        CurrentUser currentUser = new CurrentUser(
                1L, 0L, "super_admin", Set.of("super_admin"), 1L, Set.of("*"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(List.of("*", "iam:view", "product:create", "product:view"), view.routePermissions());
        assertEquals(List.of("*", "iam:view", "product:create", "product:view"), view.buttonPermissions());
        assertEquals(List.of("product", "system"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
    }

    @Test
    void grantsAllActiveMenusToPlatformSuperAdminEvenWhenMenuPermissionCodeIsNotRegistered() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "system", null, "系统", "/system", "iam:view", 120),
                menu(2L, "legacy-report", null, "历史报表", "/legacy/report", "legacy-report:view", 130));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                new InMemoryTenantPermissionEntitlementRepository());

        CurrentUser currentUser = new CurrentUser(
                1L, 0L, "super_admin", Set.of("super_admin"), 1L, Set.of("*"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(List.of("*", "iam:view"), view.routePermissions());
        assertEquals(List.of("system", "legacy-report"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
    }

    @Test
    void grantsAllActivePermissionsToPlatformSuperadminAliasSnapshot() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "system", null, "系统", "/system", "iam:view", 120),
                menu(2L, "tenant-plan-management", null, "套餐管理", "/system/tenant-plans", "tenant-plan:view", 15));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"),
                permission("tenant-plan:view", "套餐查看", "xuan-tenant", "tenant-plan-management"),
                permission("tenant-plan:manage", "套餐管理", "xuan-tenant", "tenant-plan-management"));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryColumnPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                new InMemoryTenantPermissionEntitlementRepository());

        CurrentUser currentUser = new CurrentUser(
                1L, 0L, "superadmin", Set.of(), 1L, Set.of());

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(List.of("*", "iam:view", "tenant-plan:manage", "tenant-plan:view"), view.routePermissions());
        assertEquals(List.of("tenant-plan-management", "system"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
    }

    @Test
    void usesLiveRolePermissionsInsteadOfStaleAuthorizationSnapshotForTenantUsers() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "inventory-root", null, "进销存", null, null, 20),
                menu(2L, "base-data", 1L, "基础资料", null, null, 10),
                menu(3L, "product-management", 2L, "商品管理", "/inventory/products", "product:view", 10));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("product:view", "商品查看", "xuan-product", "product-management"),
                permission("product:create", "商品新增", "xuan-product", "product-management"),
                permission("product:update", "商品修改", "xuan-product", "product-management"),
                permission("product:delete", "商品删除", "xuan-product", "product-management"));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository(
                snapshot(5L, 1001L, 20L, List.of("product:create", "product:delete", "product:update", "product:view"), List.of(
                        "inventory-root",
                        "base-data",
                        "product-management")));
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        rolePermissionRepository.userPermissionCodes.put("5:1001", List.of("product:view"));
        rolePermissionRepository.userRoleIds.put("5:1001", List.of(10L));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                snapshotRepository,
                new InMemoryColumnPermissionRepository(),
                rolePermissionRepository,
                entitlements(5L, "product:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 5L, "tenant_admin", Set.of("tenant_admin"), 20L, Set.of("product:create", "product:update", "product:delete", "product:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(List.of("product:view"), view.buttonPermissions());
        assertEquals(List.of("product:view"), view.routePermissions());
        assertEquals(List.of("inventory-root"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
    }

    @Test
    void tenantCurrentAuthorizationIsTrimmedByTenantEntitlementPoolEvenWhenRolePermissionsAreStale() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "inventory-root", null, "进销存", null, null, 20),
                menu(2L, "base-data", 1L, "基础资料", null, null, 10),
                menu(3L, "product-management", 2L, "商品管理", "/inventory/products", "product:view", 10),
                menu(4L, "finance-root", null, "财务管理", "/finance", "finance:view", 30));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("product:view", "商品查看", "xuan-product", "product-management"),
                permission("finance:view", "财务查看", "xuan-finance", "finance-root"));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository(
                snapshot(5L, 1001L, 21L, List.of("finance:view", "product:view"), List.of(
                        "inventory-root",
                        "base-data",
                        "product-management",
                        "finance-root")));
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        rolePermissionRepository.userPermissionCodes.put("5:1001", List.of("finance:view", "product:view"));
        rolePermissionRepository.userRoleIds.put("5:1001", List.of(10L));
        InMemoryTenantPermissionEntitlementRepository entitlementRepository = new InMemoryTenantPermissionEntitlementRepository();
        entitlementRepository.permissionCodesByTenant.put(5L, List.of("product:view"));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                snapshotRepository,
                new InMemoryColumnPermissionRepository(),
                rolePermissionRepository,
                entitlementRepository);

        CurrentUser currentUser = new CurrentUser(
                1001L, 5L, "tenant_admin", Set.of("tenant_admin"), 21L, Set.of("*", "finance:view", "product:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(List.of("product:view"), view.routePermissions());
        assertEquals(List.of("product:view"), view.buttonPermissions());
        assertEquals(List.of("inventory-root"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
        assertEquals(List.of("base-data"), view.menus().getFirst().children().stream().map(IamCurrentMenuNodeView::code).toList());
    }

    @Test
    void mergesColumnPermissionsFromAllCurrentUserRolesWithWiderAccessWinning() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "tenant-management", null, "租户管理", "/system/tenants", "tenant:view", 10));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("tenant:view", "租户查看", "xuan-tenant", "tenant-management"));
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        rolePermissionRepository.userPermissionCodes.put("5:1001", List.of("tenant:view"));
        rolePermissionRepository.userRoleIds.put("5:1001", List.of(10L, 20L));
        InMemoryColumnPermissionRepository columnPermissionRepository = new InMemoryColumnPermissionRepository();
        columnPermissionRepository.columnPermissionsByRole.put(10L, Map.of(
                "tenant", Map.of(
                        "code", "VISIBLE",
                        "contactPhone", "MASKED",
                        "remark", "HIDDEN")));
        columnPermissionRepository.columnPermissionsByRole.put(20L, Map.of(
                "tenant", Map.of(
                        "contactPhone", "VISIBLE",
                        "remark", "MASKED")));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                columnPermissionRepository,
                rolePermissionRepository,
                entitlements(5L, "tenant:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 5L, "tenant_admin", Set.of("tenant_admin"), 22L, Set.of("tenant:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(Map.of(
                "tenant", Map.of(
                        "code", "VISIBLE",
                        "contactPhone", "VISIBLE",
                        "remark", "MASKED")), view.columnPermissions());
    }

    @Test
    void loadsTenantBoundedColumnPermissionsEvenWhenCurrentUserHasNoRoles() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
                menu(1L, "iam-user-management", null, "用户授权", "/system/iam/users", "iam-user:view", 10));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam-user:view", "用户授权查看", "xuan-iam", "iam-user-management"));
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        rolePermissionRepository.userPermissionCodes.put("7:1001", List.of("iam-user:view"));
        InMemoryColumnPermissionRepository columnPermissionRepository = new InMemoryColumnPermissionRepository();
        columnPermissionRepository.tenantColumnPermissions.put(7L, Map.of(
                "iam-user", Map.of(
                        "username", "VISIBLE",
                        "displayName", "VISIBLE",
                        "phone", "MASKED",
                        "email", "VISIBLE",
                        "authVersion", "HIDDEN",
                        "status", "HIDDEN")));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository,
                permissionRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                columnPermissionRepository,
                rolePermissionRepository,
                entitlements(7L, "iam-user:view"));

        CurrentUser currentUser = new CurrentUser(
                1001L, 7L, "tenant_viewer", Set.of("tenant_viewer"), 31L, Set.of("iam-user:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(Map.of(
                "iam-user", Map.of(
                        "username", "VISIBLE",
                        "displayName", "VISIBLE",
                        "phone", "MASKED",
                        "email", "VISIBLE",
                        "authVersion", "HIDDEN",
                        "status", "HIDDEN")), view.columnPermissions());
    }

    private static IamMenu menu(
            Long id,
            String code,
            Long parentId,
            String title,
            String path,
            String permissionCode,
            int sortNo) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-10T10:00:00+08:00");
        return new IamMenu(
                id,
                code,
                parentId,
                title,
                "menu." + code,
                path,
                "icon-" + code,
                permissionCode,
                sortNo,
                true,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static IamPermission permission(String code, String name, String serviceName, String menuCode) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-10T10:05:00+08:00");
        return new IamPermission(
                1L,
                code,
                name,
                serviceName,
                menuCode,
                name + "描述",
                true,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static IamAuthorizationSnapshot snapshot(
            Long tenantId,
            Long userId,
            Long authVersion,
            List<String> permissionCodes,
            List<String> menuCodes) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-10T10:10:00+08:00");
        return new IamAuthorizationSnapshot(
                1L,
                tenantId,
                userId,
                authVersion,
                List.of(10L),
                permissionCodes,
                menuCodes,
                Map.of("product-sku", Map.of("code", "VISIBLE", "name", "VISIBLE")),
                String.join(",", permissionCodes),
                null,
                now,
                "system",
                now,
                "system",
                now);
    }

    private static final class InMemoryColumnPermissionRepository implements IamColumnPermissionRepository {
        private final Map<Long, Map<String, Map<String, String>>> columnPermissionsByRole = new LinkedHashMap<>();
        private final Map<Long, Map<String, Map<String, String>>> tenantColumnPermissions = new LinkedHashMap<>();

        @Override
        public Map<String, Map<String, String>> findMergedColumnPermissionsByRoleIds(Long tenantId, List<Long> roleIds) {
            if (roleIds == null || roleIds.isEmpty()) {
                return tenantColumnPermissions.getOrDefault(tenantId, Map.of());
            }
            Map<String, Map<String, String>> merged = new LinkedHashMap<>();
            for (Long roleId : roleIds) {
                Map<String, Map<String, String>> rolePermissions = columnPermissionsByRole.getOrDefault(roleId, Map.of());
                rolePermissions.forEach((resourceKey, columns) -> {
                    Map<String, String> target = merged.computeIfAbsent(resourceKey, ignored -> new LinkedHashMap<>());
                    columns.forEach((columnKey, access) -> target.merge(columnKey, access, IamCurrentAuthorizationApplicationServiceTest::widerAccess));
                });
            }
            return merged;
        }
    }

    private static String widerAccess(String current, String next) {
        return rank(next) > rank(current) ? next : current;
    }

    private static int rank(String access) {
        return switch (access) {
            case "VISIBLE" -> 3;
            case "MASKED" -> 2;
            case "HIDDEN" -> 1;
            default -> 0;
        };
    }

    private static InMemoryTenantPermissionEntitlementRepository entitlements(Long tenantId, String... permissionCodes) {
        InMemoryTenantPermissionEntitlementRepository repository = new InMemoryTenantPermissionEntitlementRepository();
        repository.permissionCodesByTenant.put(tenantId, List.of(permissionCodes));
        return repository;
    }

    private static final class InMemoryMenuRepository implements IamMenuRepository {

        private final Map<String, IamMenu> store = new LinkedHashMap<>();

        private InMemoryMenuRepository(IamMenu... menus) {
            for (IamMenu menu : menus) {
                store.put(menu.code(), menu);
            }
        }

        @Override
        public Optional<IamMenu> findByCode(String code) {
            return Optional.ofNullable(store.get(code)).filter(IamMenu::active);
        }

        @Override
        public List<IamMenu> findActiveMenus() {
            return store.values().stream().filter(IamMenu::active).toList();
        }
    }

    private static final class InMemoryPermissionRepository implements IamPermissionRepository {

        private final Map<String, IamPermission> store = new LinkedHashMap<>();

        private InMemoryPermissionRepository(IamPermission... permissions) {
            for (IamPermission permission : permissions) {
                store.put(permission.code(), permission);
            }
        }

        @Override
        public Optional<IamPermission> findByCode(String code) {
            return Optional.ofNullable(store.get(code)).filter(IamPermission::active);
        }

        @Override
        public List<IamPermission> findActivePermissions() {
            return store.values().stream().filter(IamPermission::active).toList();
        }
    }

    private static final class InMemoryAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {

        private final Map<String, IamAuthorizationSnapshot> store = new LinkedHashMap<>();

        private InMemoryAuthorizationSnapshotRepository(IamAuthorizationSnapshot... snapshots) {
            for (IamAuthorizationSnapshot snapshot : snapshots) {
                store.put(snapshot.tenantId() + ":" + snapshot.userId(), snapshot);
            }
        }

        @Override
        public Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) {
            return Optional.ofNullable(store.get(tenantId + ":" + userId));
        }

        @Override
        public IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot) {
            store.put(snapshot.tenantId() + ":" + snapshot.userId(), snapshot);
            return snapshot;
        }
    }

    private static final class InMemoryRolePermissionRepository implements IamRolePermissionRepository {

        private final Map<String, List<String>> userPermissionCodes = new LinkedHashMap<>();
        private final Map<String, List<Long>> userRoleIds = new LinkedHashMap<>();

        @Override
        public List<String> findPermissionCodesByRoleId(Long tenantId, Long roleId) {
            return List.of();
        }

        @Override
        public void replaceRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds, String operator) {
        }

        @Override
        public void removeRolePermissionsOutsideTenantEntitlements(Long tenantId, String operator) {
        }

        @Override
        public List<Long> findUserIdsByRoleId(Long tenantId, Long roleId) {
            return List.of();
        }

        @Override
        public List<Long> findRoleIdsByUserId(Long tenantId, Long userId) {
            return userRoleIds.getOrDefault(tenantId + ":" + userId, List.of());
        }

        @Override
        public List<String> findPermissionCodesByUserId(Long tenantId, Long userId) {
            return userPermissionCodes.getOrDefault(tenantId + ":" + userId, List.of());
        }

        @Override
        public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds, String operator) {
        }
    }

    private static final class InMemoryTenantPermissionEntitlementRepository implements IamTenantPermissionEntitlementRepository {
        private final Map<Long, List<String>> permissionCodesByTenant = new LinkedHashMap<>();

        @Override
        public List<String> findPermissionCodesByTenantId(Long tenantId) {
            return permissionCodesByTenant.getOrDefault(tenantId, List.of());
        }

        @Override
        public List<Long> findTenantIdsByInitTemplateCode(String initTemplateCode) {
            return List.of();
        }

        @Override
        public void replaceTenantEntitlements(Long tenantId, String initTemplateCode, List<Long> permissionIds, long entitlementVersion, String operator) {
        }

        @Override
        public long nextEntitlementVersion(Long tenantId) {
            return 1;
        }
    }
}
