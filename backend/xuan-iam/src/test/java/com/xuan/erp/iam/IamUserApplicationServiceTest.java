package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamUserCommand;
import com.xuan.erp.iam.application.command.DisableIamUserCommand;
import com.xuan.erp.iam.application.command.RebuildAuthorizationSnapshotCommand;
import com.xuan.erp.iam.application.command.SetIamUserRolesCommand;
import com.xuan.erp.iam.application.query.IamAuthorizationSnapshotView;
import com.xuan.erp.iam.application.query.IamUserRoleGrantView;
import com.xuan.erp.iam.application.query.IamUserDetailView;
import com.xuan.erp.iam.application.service.IamAuthorizationApplicationService;
import com.xuan.erp.iam.application.service.IamUserApplicationService;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamUserApplicationServiceTest {

    @Test
    void createsUserWithTenantScopedCaseSensitiveUsername() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        IamUserApplicationService service = service(userRepository);

        IamUserDetailView first = service.createUser(new CreateIamUserCommand(
                1001L,
                " Admin ",
                "{bcrypt}hash",
                "管理员",
                "admin@example.com",
                "13800000000",
                "首个账号"));
        IamUserDetailView second = service.createUser(new CreateIamUserCommand(
                1001L,
                "admin",
                "{bcrypt}hash2",
                "小写管理员",
                null,
                null,
                null));

        assertEquals("Admin", first.username());
        assertEquals("admin", second.username());
        assertEquals(0L, first.authVersion());
        assertTrue(first.enabled());
    }

    @Test
    void rejectsDuplicateUsernameInSameTenant() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        IamUserApplicationService service = service(userRepository);
        service.createUser(new CreateIamUserCommand(1001L, "admin", "{bcrypt}hash", "管理员", null, null, null));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createUser(new CreateIamUserCommand(1001L, "admin", "{bcrypt}another", "重复账号", null, null, null)));

        assertEquals("IAM_USERNAME_EXISTS", error.code());
    }

    @Test
    void disablesUserAndIncrementsAuthVersion() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        IamUserApplicationService service = service(userRepository);
        Long userId = service.createUser(new CreateIamUserCommand(1001L, "admin", "{bcrypt}hash", "管理员", null, null, null)).id();

        IamUserDetailView disabled = service.disableUser(userId, new DisableIamUserCommand("离职停用", "security-admin"));

        assertFalse(disabled.enabled());
        assertEquals(1L, disabled.authVersion());
        assertEquals("离职停用", userRepository.store.get(userId).deleteReason());
    }

    @Test
    void listsPlatformUsersWhenTenantIdIsZero() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        IamUserApplicationService service = service(userRepository);
        userRepository.save(user(1L, 0L, "super_admin", 3L));
        userRepository.save(user(2L, 1001L, "tenant_admin", 5L));

        List<IamUserDetailView> users = service.listUsers(0L);

        assertEquals(List.of("super_admin"), users.stream().map(IamUserDetailView::username).toList());
    }

    @Test
    void getsPlatformUserRolesWhenTenantIdIsZero() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        IamUserApplicationService service = new IamUserApplicationService(
                userRepository,
                new InMemoryRoleRepository(),
                rolePermissionRepository,
                new InMemoryAuthorizationSnapshotRepository());
        userRepository.save(user(1L, 0L, "super_admin", 2L));
        rolePermissionRepository.userRoleIds.put(1L, List.of(10L));

        IamUserRoleGrantView grant = service.getUserRoles(0L, 1L);

        assertEquals(0L, grant.tenantId());
        assertEquals(1L, grant.userId());
        assertEquals(List.of(10L), grant.roleIds());
    }

    @Test
    void rejectsReplacingPlatformUserRoles() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        IamUserApplicationService service = service(userRepository);
        userRepository.save(user(1L, 0L, "super_admin", 2L));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.replaceUserRoles(new SetIamUserRolesCommand(0L, 1L, List.of(10L), "security-admin")));

        assertEquals("IAM_PLATFORM_USER_ROLE_READ_ONLY", error.code());
    }

    @Test
    void replacesUserRolesAndRefreshesAuthorizationSnapshot() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamUserApplicationService service = new IamUserApplicationService(
                userRepository,
                roleRepository,
                rolePermissionRepository,
                snapshotRepository);
        Long userId = service.createUser(new CreateIamUserCommand(1001L, "buyer", "{bcrypt}hash", "采购员", null, null, null)).id();
        IamRole buyerRole = roleRepository.save(role(10L, 1001L, "buyer"));
        IamRole viewerRole = roleRepository.save(role(11L, 1001L, "viewer"));
        rolePermissionRepository.rolePermissionCodes.put(buyerRole.id(), List.of("procurement:view"));
        rolePermissionRepository.rolePermissionCodes.put(viewerRole.id(), List.of("product:view"));

        IamUserRoleGrantView grant = service.replaceUserRoles(new SetIamUserRolesCommand(
                1001L,
                userId,
                List.of(viewerRole.id(), buyerRole.id(), buyerRole.id()),
                "security-admin"));

        IamUser refreshedUser = userRepository.findById(userId).orElseThrow();
        IamAuthorizationSnapshot refreshedSnapshot = snapshotRepository.findByTenantIdAndUserId(1001L, userId).orElseThrow();
        assertEquals(List.of(buyerRole.id(), viewerRole.id()), grant.roleIds());
        assertEquals(List.of(buyerRole.id(), viewerRole.id()), rolePermissionRepository.userRoleIds.get(userId));
        assertEquals(1L, refreshedUser.authVersion());
        assertEquals(1L, refreshedSnapshot.authVersion());
        assertEquals(List.of(buyerRole.id(), viewerRole.id()), refreshedSnapshot.roleIds());
        assertEquals(List.of("procurement:view", "product:view"), refreshedSnapshot.permissionCodes());
    }

    @Test
    void rebuildsAuthorizationSnapshotWithStableSortedCodes() {
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamAuthorizationApplicationService service = new IamAuthorizationApplicationService(snapshotRepository);

        IamAuthorizationSnapshotView view = service.rebuildSnapshot(new RebuildAuthorizationSnapshotCommand(
                1001L,
                42L,
                7L,
                List.of(3L, 1L),
                List.of("iam:update", "iam:view", "iam:view"),
                List.of("system", "workbench", "system")));

        assertEquals(List.of(1L, 3L), view.roleIds());
        assertEquals(List.of("iam:update", "iam:view"), view.permissionCodes());
        assertEquals(List.of("system", "workbench"), view.menuCodes());
        assertEquals("iam:update,iam:view|system,workbench|1,3", view.snapshotHash());
    }

    private static final class InMemoryIamUserRepository implements IamUserRepository {
        private final Map<Long, IamUser> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<IamUser> findById(Long id) {
            return Optional.ofNullable(store.get(id)).filter(IamUser::active);
        }

        @Override
        public Optional<IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username) {
            return store.values().stream()
                    .filter(IamUser::active)
                    .filter(user -> user.tenantId().equals(tenantId))
                    .filter(user -> user.username().equals(username))
                    .findFirst();
        }

        @Override
        public List<IamUser> findActiveUsers(Long tenantId) {
            return store.values().stream()
                    .filter(IamUser::active)
                    .filter(user -> user.tenantId().equals(tenantId))
                    .sorted(Comparator.comparing(IamUser::id))
                    .toList();
        }

        @Override
        public IamUser save(IamUser user) {
            Long id = user.id() == null ? nextId++ : user.id();
            IamUser saved = new IamUser(
                    id,
                    user.tenantId(),
                    user.username(),
                    user.passwordHash(),
                    user.displayName(),
                    user.email(),
                    user.phone(),
                    user.avatarUrl(),
                    user.enabled(),
                    user.accountNonExpired(),
                    user.accountNonLocked(),
                    user.credentialsNonExpired(),
                    user.lastLoginAt(),
                    user.passwordChangedAt(),
                    user.failedLoginCount(),
                    user.lastFailedLoginAt(),
                    user.lockedUntil(),
                    user.mfaEnabled(),
                    user.authVersion(),
                    user.remark(),
                    user.createdBy(),
                    user.createdAt(),
                    user.updatedBy(),
                    user.updatedAt(),
                    user.deletedBy(),
                    user.deleteReason(),
                    user.deletedAt());
            store.put(id, saved);
            return saved;
        }
    }

    private static IamUserApplicationService service(InMemoryIamUserRepository userRepository) {
        return new IamUserApplicationService(
                userRepository,
                new InMemoryRoleRepository(),
                new InMemoryRolePermissionRepository(),
                new InMemoryAuthorizationSnapshotRepository());
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

    private static IamRole role(Long id, Long tenantId, String code) {
        OffsetDateTime now = OffsetDateTime.now();
        return new IamRole(
                id,
                tenantId,
                code,
                code,
                code,
                true,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static final class InMemoryRoleRepository implements IamRoleRepository {
        private final Map<Long, IamRole> store = new LinkedHashMap<>();

        @Override
        public Optional<IamRole> findById(Long id) {
            return Optional.ofNullable(store.get(id)).filter(IamRole::active);
        }

        @Override
        public Optional<IamRole> findActiveByTenantIdAndCode(Long tenantId, String code) {
            return store.values().stream()
                    .filter(IamRole::active)
                    .filter(role -> role.tenantId().equals(tenantId))
                    .filter(role -> role.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<IamRole> findActiveRoles(Long tenantId) {
            return store.values().stream()
                    .filter(IamRole::active)
                    .filter(role -> role.tenantId().equals(tenantId))
                    .sorted(Comparator.comparing(IamRole::id))
                    .toList();
        }

        @Override
        public IamRole save(IamRole role) {
            store.put(role.id(), role);
            return role;
        }
    }

    private static final class InMemoryRolePermissionRepository implements IamRolePermissionRepository {
        private final Map<Long, List<Long>> userRoleIds = new LinkedHashMap<>();
        private final Map<Long, List<String>> rolePermissionCodes = new LinkedHashMap<>();

        @Override
        public List<String> findPermissionCodesByRoleId(Long tenantId, Long roleId) {
            return rolePermissionCodes.getOrDefault(roleId, List.of());
        }

        @Override
        public void replaceRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds, String operator) {
        }

        @Override
        public List<Long> findUserIdsByRoleId(Long tenantId, Long roleId) {
            return userRoleIds.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(roleId))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
        }

        @Override
        public List<Long> findRoleIdsByUserId(Long tenantId, Long userId) {
            return userRoleIds.getOrDefault(userId, List.of());
        }

        @Override
        public List<String> findPermissionCodesByUserId(Long tenantId, Long userId) {
            return findRoleIdsByUserId(tenantId, userId).stream()
                    .flatMap(roleId -> rolePermissionCodes.getOrDefault(roleId, List.of()).stream())
                    .distinct()
                    .sorted()
                    .toList();
        }

        @Override
        public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds, String operator) {
            userRoleIds.put(userId, List.copyOf(roleIds));
        }
    }

    private static final class InMemoryAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {
        private final List<IamAuthorizationSnapshot> saved = new ArrayList<>();

        @Override
        public Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) {
            return saved.stream()
                    .filter(snapshot -> snapshot.tenantId().equals(tenantId))
                    .filter(snapshot -> snapshot.userId().equals(userId))
                    .reduce((first, second) -> second);
        }

        @Override
        public IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot) {
            IamAuthorizationSnapshot savedSnapshot = new IamAuthorizationSnapshot(
                    snapshot.id() == null ? (long) saved.size() + 1 : snapshot.id(),
                    snapshot.tenantId(),
                    snapshot.userId(),
                    snapshot.authVersion(),
                    snapshot.roleIds(),
                    snapshot.permissionCodes(),
                    snapshot.menuCodes(),
                    snapshot.columnSettings(),
                    snapshot.snapshotHash(),
                    snapshot.expiresAt(),
                    snapshot.builtAt() == null ? OffsetDateTime.now() : snapshot.builtAt(),
                    snapshot.createdBy(),
                    snapshot.createdAt(),
                    snapshot.updatedBy(),
                    snapshot.updatedAt());
            saved.add(savedSnapshot);
            return savedSnapshot;
        }
    }
}
