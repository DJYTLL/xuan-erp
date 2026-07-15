package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamMenuCommand;
import com.xuan.erp.iam.application.command.CreateIamPermissionCommand;
import com.xuan.erp.iam.application.command.CreateIamRoleCommand;
import com.xuan.erp.iam.application.command.SetIamRolePermissionsCommand;
import com.xuan.erp.iam.application.query.IamRolePermissionGrantView;
import com.xuan.erp.iam.application.service.IamMenuApplicationService;
import com.xuan.erp.iam.application.service.IamPermissionApplicationService;
import com.xuan.erp.iam.application.service.IamRoleApplicationService;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamUser;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamAdminManagementApplicationServiceTest {

    @Test
    void createsMenuUnderExistingParentAndRejectsDuplicateCode() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository();
        IamMenuApplicationService service = new IamMenuApplicationService(menuRepository);
        IamMenu system = service.createMenu(new CreateIamMenuCommand(
                "system",
                null,
                "系统设置",
                "menu.system",
                "/system",
                "Settings",
                "iam:view",
                120));

        IamMenu roleMenu = service.createMenu(new CreateIamMenuCommand(
                "iam-role-management",
                system.id(),
                "角色管理",
                "menu.iamRoles",
                "/system/iam/roles",
                "ShieldCheck",
                "iam:view",
                122));

        assertEquals(system.id(), roleMenu.parentId());
        assertEquals("iam-role-management", roleMenu.code());
        assertEquals(122, roleMenu.sortNo());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createMenu(new CreateIamMenuCommand(
                        " iam-role-management ",
                        system.id(),
                        "重复角色管理",
                        "menu.iamRoles",
                        "/system/iam/roles-copy",
                        "ShieldCheck",
                        "iam:view",
                        123)));
        assertEquals("IAM_MENU_CODE_EXISTS", error.code());
    }

    @Test
    void createsPermissionAndRejectsDuplicateCode() {
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        IamPermissionApplicationService service = new IamPermissionApplicationService(permissionRepository);

        IamPermission permission = service.createPermission(new CreateIamPermissionCommand(
                "iam:role:update",
                "角色修改",
                "xuan-iam",
                "iam-role-management",
                "修改角色资料和授权"));

        assertEquals("iam:role:update", permission.code());
        assertTrue(permission.enabled());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createPermission(new CreateIamPermissionCommand(
                        "iam:role:update",
                        "重复角色修改",
                        "xuan-iam",
                        "iam-role-management",
                        "重复权限")));
        assertEquals("IAM_PERMISSION_CODE_EXISTS", error.code());
    }

    @Test
    void replacesRolePermissionsWithResolvedPermissionIds() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository);
        IamRole role = service.createRole(new CreateIamRoleCommand(1001L, "tenant_admin", "租户管理员", "管理租户配置"));
        permissionRepository.save(permission("iam:view"));
        permissionRepository.save(permission("iam:update"));

        IamRolePermissionGrantView grant = service.replaceRolePermissions(new SetIamRolePermissionsCommand(
                1001L,
                role.id(),
                List.of("iam:update", "iam:view", "iam:view"),
                "security-admin"));

        assertEquals(List.of("iam:update", "iam:view"), grant.permissionCodes());
        assertEquals(List.of(1L, 2L), rolePermissionRepository.grantedPermissionIds);
        assertEquals("security-admin", rolePermissionRepository.operator);
    }

    @Test
    void refreshesAffectedUserAuthorizationSnapshotWhenRolePermissionsChange() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository);
        IamRole role = service.createRole(new CreateIamRoleCommand(1001L, "buyer", "采购员", "采购业务"));
        IamUser user = userRepository.save(user(10L, 1001L, "buyer01", 4L));
        rolePermissionRepository.userRoleIds.put(user.id(), List.of(role.id()));
        rolePermissionRepository.roleUserIds.put(role.id(), List.of(user.id()));
        permissionRepository.save(permission("procurement:view"));
        permissionRepository.save(permission("product:view"));

        service.replaceRolePermissions(new SetIamRolePermissionsCommand(
                1001L,
                role.id(),
                List.of("procurement:view", "product:view"),
                "security-admin"));

        IamUser refreshedUser = userRepository.findById(user.id()).orElseThrow();
        IamAuthorizationSnapshot refreshedSnapshot = snapshotRepository
                .findByTenantIdAndUserId(1001L, user.id())
                .orElseThrow();
        assertEquals(5L, refreshedUser.authVersion());
        assertEquals(5L, refreshedSnapshot.authVersion());
        assertEquals(List.of(role.id()), refreshedSnapshot.roleIds());
        assertEquals(List.of("procurement:view", "product:view"), refreshedSnapshot.permissionCodes());
        assertEquals(List.of(), refreshedSnapshot.menuCodes());
    }

    private static IamPermission permission(String code) {
        return new IamPermission(
                null,
                code,
                code,
                "xuan-iam",
                "system",
                code,
                true,
                "system",
                OffsetDateTime.now(),
                "system",
                OffsetDateTime.now(),
                null,
                null,
                null);
    }

    private static IamUser user(Long id, Long tenantId, String username, long authVersion) {
        OffsetDateTime now = OffsetDateTime.now();
        return new IamUser(
                id,
                tenantId,
                username,
                "{noop}password",
                username,
                null,
                null,
                null,
                true,
                true,
                true,
                true,
                null,
                now,
                0,
                null,
                null,
                false,
                authVersion,
                null,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static final class InMemoryMenuRepository implements IamMenuRepository {
        private final Map<Long, IamMenu> byId = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<IamMenu> findById(Long id) {
            return Optional.ofNullable(byId.get(id)).filter(IamMenu::active);
        }

        @Override
        public Optional<IamMenu> findByCode(String code) {
            return byId.values().stream()
                    .filter(IamMenu::active)
                    .filter(menu -> menu.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<IamMenu> findActiveMenus() {
            return byId.values().stream()
                    .filter(IamMenu::active)
                    .sorted(Comparator.comparingInt(IamMenu::sortNo).thenComparing(IamMenu::code))
                    .toList();
        }

        @Override
        public IamMenu save(IamMenu menu) {
            Long id = menu.id() == null ? nextId++ : menu.id();
            IamMenu saved = new IamMenu(
                    id,
                    menu.code(),
                    menu.parentId(),
                    menu.title(),
                    menu.i18nKey(),
                    menu.path(),
                    menu.icon(),
                    menu.permissionCode(),
                    menu.sortNo(),
                    menu.enabled(),
                    menu.createdBy(),
                    menu.createdAt(),
                    menu.updatedBy(),
                    menu.updatedAt(),
                    menu.deletedBy(),
                    menu.deleteReason(),
                    menu.deletedAt());
            byId.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryPermissionRepository implements IamPermissionRepository {
        private final Map<Long, IamPermission> byId = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<IamPermission> findById(Long id) {
            return Optional.ofNullable(byId.get(id)).filter(IamPermission::active);
        }

        @Override
        public Optional<IamPermission> findByCode(String code) {
            return byId.values().stream()
                    .filter(IamPermission::active)
                    .filter(permission -> permission.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<IamPermission> findActivePermissions() {
            return new ArrayList<>(byId.values());
        }

        @Override
        public IamPermission save(IamPermission permission) {
            Long id = permission.id() == null ? nextId++ : permission.id();
            IamPermission saved = new IamPermission(
                    id,
                    permission.code(),
                    permission.name(),
                    permission.serviceName(),
                    permission.menuCode(),
                    permission.description(),
                    permission.enabled(),
                    permission.createdBy(),
                    permission.createdAt(),
                    permission.updatedBy(),
                    permission.updatedAt(),
                    permission.deletedBy(),
                    permission.deleteReason(),
                    permission.deletedAt());
            byId.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryRoleRepository implements IamRoleRepository {
        private final Map<Long, IamRole> byId = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<IamRole> findById(Long id) {
            return Optional.ofNullable(byId.get(id)).filter(IamRole::active);
        }

        @Override
        public Optional<IamRole> findActiveByTenantIdAndCode(Long tenantId, String code) {
            return byId.values().stream()
                    .filter(IamRole::active)
                    .filter(role -> role.tenantId().equals(tenantId))
                    .filter(role -> role.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<IamRole> findActiveRoles(Long tenantId) {
            return byId.values().stream()
                    .filter(IamRole::active)
                    .filter(role -> role.tenantId().equals(tenantId))
                    .toList();
        }

        @Override
        public IamRole save(IamRole role) {
            Long id = role.id() == null ? nextId++ : role.id();
            IamRole saved = new IamRole(
                    id,
                    role.tenantId(),
                    role.code(),
                    role.name(),
                    role.description(),
                    role.enabled(),
                    role.createdBy(),
                    role.createdAt(),
                    role.updatedBy(),
                    role.updatedAt(),
                    role.deletedBy(),
                    role.deleteReason(),
                    role.deletedAt());
            byId.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryRolePermissionRepository implements IamRolePermissionRepository {
        private List<Long> grantedPermissionIds = List.of();
        private String operator;
        private final Map<Long, List<Long>> roleUserIds = new LinkedHashMap<>();
        private final Map<Long, List<Long>> userRoleIds = new LinkedHashMap<>();

        @Override
        public List<String> findPermissionCodesByRoleId(Long tenantId, Long roleId) {
            return List.of();
        }

        @Override
        public void replaceRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds, String operator) {
            this.grantedPermissionIds = List.copyOf(permissionIds);
            this.operator = operator;
        }

        @Override
        public List<Long> findUserIdsByRoleId(Long tenantId, Long roleId) {
            return roleUserIds.getOrDefault(roleId, List.of());
        }

        @Override
        public List<Long> findRoleIdsByUserId(Long tenantId, Long userId) {
            return userRoleIds.getOrDefault(userId, List.of());
        }

        @Override
        public List<String> findPermissionCodesByUserId(Long tenantId, Long userId) {
            return grantedPermissionIds.stream()
                    .map(id -> id == 1L ? "procurement:view" : "product:view")
                    .sorted()
                    .toList();
        }

        @Override
        public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds, String operator) {
            userRoleIds.put(userId, List.copyOf(roleIds));
        }
    }

    private static final class InMemoryUserRepository implements IamUserRepository {
        private final Map<Long, IamUser> byId = new LinkedHashMap<>();

        @Override
        public Optional<IamUser> findById(Long id) {
            return Optional.ofNullable(byId.get(id)).filter(IamUser::active);
        }

        @Override
        public Optional<IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username) {
            return byId.values().stream()
                    .filter(IamUser::active)
                    .filter(user -> user.tenantId().equals(tenantId))
                    .filter(user -> user.username().equals(username))
                    .findFirst();
        }

        @Override
        public List<IamUser> findActiveUsers(Long tenantId) {
            return byId.values().stream()
                    .filter(IamUser::active)
                    .filter(user -> user.tenantId().equals(tenantId))
                    .toList();
        }

        @Override
        public IamUser save(IamUser user) {
            byId.put(user.id(), user);
            return user;
        }
    }

    private static final class InMemoryAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {
        private final Map<String, IamAuthorizationSnapshot> byTenantAndUser = new LinkedHashMap<>();

        @Override
        public Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) {
            return Optional.ofNullable(byTenantAndUser.get(tenantId + ":" + userId));
        }

        @Override
        public IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot) {
            byTenantAndUser.put(snapshot.tenantId() + ":" + snapshot.userId(), snapshot);
            return snapshot;
        }
    }
}
