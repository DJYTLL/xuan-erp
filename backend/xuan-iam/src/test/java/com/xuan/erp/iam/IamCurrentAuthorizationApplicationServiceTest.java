package com.xuan.erp.iam;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.query.IamCurrentMenuNodeView;
import com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView;
import com.xuan.erp.iam.application.service.IamCurrentAuthorizationApplicationService;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
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
                menu(1L, "product", null, "商品", "/product", "product:read", 20),
                menu(3L, "product-sku", 1L, "商品档案", "/product/sku", "product:read", 10));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"),
                permission("product:read", "商品查看", "xuan-product", "product"));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository(
                snapshot(1L, 1001L, 7L, List.of("iam:view", "product:read"), List.of("system", "product", "product-sku")));

        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository, permissionRepository, snapshotRepository);

        CurrentUser currentUser = new CurrentUser(
                1001L, 1L, "tenant_admin", Set.of("tenant_admin"), 7L, Set.of("iam:view", "product:read"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(7L, view.authVersion());
        assertEquals(List.of("iam:view", "product:read"), view.routePermissions());
        assertEquals(List.of("iam:view", "product:read"), view.buttonPermissions());
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
                menu(2L, "product", null, "商品", "/product", "product:read", 20),
                menu(3L, "audit", null, "审计", "/audit", "audit:read", 130));
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission("iam:view", "权限查看", "xuan-iam", "system"),
                permission("product:read", "商品查看", "xuan-product", "product"),
                permission("audit:read", "审计查看", "xuan-audit", "audit"));
        IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
                menuRepository, permissionRepository, new InMemoryAuthorizationSnapshotRepository());

        CurrentUser currentUser = new CurrentUser(
                1001L, 1L, "tenant_admin", Set.of("tenant_admin"), 9L, Set.of("product:read", "iam:view"));

        IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

        assertEquals(9L, view.authVersion());
        assertEquals(List.of("iam:view", "product:read"), view.routePermissions());
        assertEquals(List.of("product", "system"), view.menus().stream().map(IamCurrentMenuNodeView::code).toList());
        assertEquals(Map.of(), view.columnPermissions());
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
                Map.of("product-sku", List.of("code", "name")),
                String.join(",", permissionCodes),
                null,
                now,
                "system",
                now,
                "system",
                now);
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
}
