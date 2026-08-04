package com.xuan.erp.iam;

import com.xuan.erp.iam.application.port.IamTenantPlanUsageGateway;
import com.xuan.erp.iam.application.service.IamTenantPermissionEntitlementApplicationService;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamTenantPermissionEntitlementRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IamTenantPermissionEntitlementApplicationServiceTest {

    @Test
    void syncTemplateEntitlementsIncludesTenantPlanUsageFromTenantService() {
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission(1L, "product:view"),
                permission(2L, "tenant:view"));
        RecordingTenantEntitlementRepository entitlementRepository = new RecordingTenantEntitlementRepository();
        entitlementRepository.localTenantIds = List.of(1001L);
        RecordingRolePermissionRepository rolePermissionRepository = new RecordingRolePermissionRepository();
        IamTenantPlanUsageGateway tenantPlanUsageGateway = initTemplateCode -> List.of(1002L, 1001L, 1003L);
        IamTenantPermissionEntitlementApplicationService service = new IamTenantPermissionEntitlementApplicationService(
                permissionRepository,
                entitlementRepository,
                rolePermissionRepository,
                new EmptyUserRepository(),
                new NoOpAuthorizationSnapshotRepository(),
                tenantPlanUsageGateway);

        service.syncTemplateEntitlements("basic", List.of("tenant:view", "product:view"), "super_admin");

        assertEquals(List.of(1001L, 1002L, 1003L), entitlementRepository.replacedTenantIds);
        assertEquals(List.of("1001:basic:[1, 2]:1:super_admin",
                "1002:basic:[1, 2]:1:super_admin",
                "1003:basic:[1, 2]:1:super_admin"), entitlementRepository.replacements);
        assertEquals(List.of(1001L, 1002L, 1003L), rolePermissionRepository.prunedTenantIds);
    }

    @Test
    void replaceTenantEntitlementsGrantsAllTenantPermissionsToAdminRole() {
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission(1L, "product:view"),
                permission(2L, "iam:role:view"));
        RecordingTenantEntitlementRepository entitlementRepository = new RecordingTenantEntitlementRepository();
        RecordingRolePermissionRepository rolePermissionRepository = new RecordingRolePermissionRepository();
        IamTenantPermissionEntitlementApplicationService service = new IamTenantPermissionEntitlementApplicationService(
                permissionRepository,
                entitlementRepository,
                rolePermissionRepository,
                new InMemoryRoleRepository(role(501L, 1001L, "tenant_admin")),
                new EmptyUserRepository(),
                new NoOpAuthorizationSnapshotRepository());

        service.replaceTenantEntitlements(
                1001L,
                "basic",
                List.of("iam:role:view", "product:view"),
                "super_admin");

        assertEquals(List.of("1001:501:super_admin"), rolePermissionRepository.adminRoleGrants);
        assertEquals(List.of("1001:501:[1, 2]:super_admin"), rolePermissionRepository.replacements);
        assertEquals(List.of(1001L), rolePermissionRepository.prunedTenantIds);
    }

    @Test
    void replaceTenantEntitlementsCreatesTenantAdminRoleWhenMissing() {
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permission(1L, "product:view"));
        RecordingTenantEntitlementRepository entitlementRepository = new RecordingTenantEntitlementRepository();
        RecordingRolePermissionRepository rolePermissionRepository = new RecordingRolePermissionRepository();
        IamTenantPermissionEntitlementApplicationService service = new IamTenantPermissionEntitlementApplicationService(
                permissionRepository,
                entitlementRepository,
                rolePermissionRepository,
                new InMemoryRoleRepository(),
                new EmptyUserRepository(),
                new NoOpAuthorizationSnapshotRepository());

        service.replaceTenantEntitlements(1001L, "basic", List.of("product:view"), "super_admin");

        assertEquals(List.of("1001:9001:super_admin"), rolePermissionRepository.adminRoleGrants);
        assertEquals(List.of("1001:9001:[1]:super_admin"), rolePermissionRepository.replacements);
    }

    @Test
    void replaceTenantEntitlementsBuildsFixedLengthSnapshotHashWhenPermissionPoolIsLarge() {
        List<IamPermission> permissions = IntStream.rangeClosed(1, 80)
                .mapToObj(index -> permission((long) index, "module:permission:" + index + ":manage"))
                .toList();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
                permissions.toArray(IamPermission[]::new));
        RecordingTenantEntitlementRepository entitlementRepository = new RecordingTenantEntitlementRepository();
        RecordingRolePermissionRepository rolePermissionRepository = new RecordingRolePermissionRepository();
        rolePermissionRepository.userRoleIds = List.of(501L, 502L);
        rolePermissionRepository.userPermissionCodes = permissions.stream()
                .map(IamPermission::code)
                .toList();
        RecordingAuthorizationSnapshotRepository snapshotRepository = new RecordingAuthorizationSnapshotRepository();
        IamTenantPermissionEntitlementApplicationService service = new IamTenantPermissionEntitlementApplicationService(
                permissionRepository,
                entitlementRepository,
                rolePermissionRepository,
                new InMemoryRoleRepository(role(501L, 1001L, "tenant_admin")),
                new ActiveUserRepository(user(7001L, 1001L, 0L)),
                snapshotRepository);

        service.replaceTenantEntitlements(
                1001L,
                "basic",
                permissions.stream().map(IamPermission::code).toList(),
                "super_admin");

        assertEquals(1, snapshotRepository.saved.size());
        assertEquals(64, snapshotRepository.saved.getFirst().snapshotHash().length());
    }

    private static IamPermission permission(Long id, String code) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-17T00:00:00Z");
        return new IamPermission(id, code, code, "test", null, null, true,
                "system", now, "system", now, null, null, null);
    }

    private static IamRole role(Long id, Long tenantId, String code) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-17T00:00:00Z");
        return new IamRole(id, tenantId, code, code, "test", true,
                "system", now, "system", now, null, null, null);
    }

    private static IamUser user(Long id, Long tenantId, Long authVersion) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-17T00:00:00Z");
        return new IamUser(
                id,
                tenantId,
                "tenant_admin",
                "{noop}password",
                "租户管理员",
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

    private static final class InMemoryPermissionRepository implements IamPermissionRepository {
        private final Map<String, IamPermission> permissions = new LinkedHashMap<>();

        private InMemoryPermissionRepository(IamPermission... values) {
            for (IamPermission permission : values) {
                permissions.put(permission.code(), permission);
            }
        }

        @Override
        public Optional<IamPermission> findByCode(String code) {
            return Optional.ofNullable(permissions.get(code));
        }

        @Override
        public List<IamPermission> findActivePermissions() {
            return List.copyOf(permissions.values());
        }
    }

    private static final class RecordingTenantEntitlementRepository implements IamTenantPermissionEntitlementRepository {
        private List<Long> localTenantIds = List.of();
        private final List<Long> replacedTenantIds = new ArrayList<>();
        private final List<String> replacements = new ArrayList<>();

        @Override
        public List<String> findPermissionCodesByTenantId(Long tenantId) {
            return List.of();
        }

        @Override
        public List<Long> findTenantIdsByInitTemplateCode(String initTemplateCode) {
            return localTenantIds;
        }

        @Override
        public void replaceTenantEntitlements(
                Long tenantId,
                String initTemplateCode,
                List<Long> permissionIds,
                long entitlementVersion,
                String operator) {
            replacedTenantIds.add(tenantId);
            replacements.add(tenantId + ":" + initTemplateCode + ":" + permissionIds + ":" + entitlementVersion + ":" + operator);
        }

        @Override
        public long nextEntitlementVersion(Long tenantId) {
            return 1;
        }
    }

    private static final class RecordingRolePermissionRepository implements IamRolePermissionRepository {
        private final List<Long> prunedTenantIds = new ArrayList<>();
        private final List<String> replacements = new ArrayList<>();
        private final List<String> adminRoleGrants = new ArrayList<>();
        private List<Long> userRoleIds = List.of();
        private List<String> userPermissionCodes = List.of();

        @Override
        public List<String> findPermissionCodesByRoleId(Long tenantId, Long roleId) {
            return List.of();
        }

        @Override
        public void replaceRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds, String operator) {
            replacements.add(tenantId + ":" + roleId + ":" + permissionIds + ":" + operator);
        }

        @Override
        public void grantRoleToTenantAdmins(Long tenantId, Long roleId, String operator) {
            adminRoleGrants.add(tenantId + ":" + roleId + ":" + operator);
        }

        @Override
        public void removeRolePermissionsOutsideTenantEntitlements(Long tenantId, String operator) {
            prunedTenantIds.add(tenantId);
        }

        @Override
        public List<Long> findUserIdsByRoleId(Long tenantId, Long roleId) {
            return List.of();
        }

        @Override
        public List<Long> findRoleIdsByUserId(Long tenantId, Long userId) {
            return userRoleIds;
        }

        @Override
        public List<String> findPermissionCodesByUserId(Long tenantId, Long userId) {
            return userPermissionCodes;
        }

        @Override
        public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds, String operator) {
        }
    }

    private static final class InMemoryRoleRepository implements IamRoleRepository {
        private final Map<String, IamRole> roles = new LinkedHashMap<>();

        private InMemoryRoleRepository(IamRole... values) {
            for (IamRole role : values) {
                roles.put(role.tenantId() + ":" + role.code(), role);
            }
        }

        @Override
        public Optional<IamRole> findById(Long id) {
            return roles.values().stream()
                    .filter(role -> role.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<IamRole> findActiveByTenantIdAndCode(Long tenantId, String code) {
            return Optional.ofNullable(roles.get(tenantId + ":" + code))
                    .filter(IamRole::active);
        }

        @Override
        public List<IamRole> findActiveRoles(Long tenantId) {
            return roles.values().stream()
                    .filter(role -> role.tenantId().equals(tenantId))
                    .filter(IamRole::active)
                    .toList();
        }

        @Override
        public IamRole save(IamRole role) {
            IamRole saved = new IamRole(9001L, role.tenantId(), role.code(), role.name(), role.description(), role.enabled(),
                    role.createdBy(), role.createdAt(), role.updatedBy(), role.updatedAt(),
                    role.deletedBy(), role.deleteReason(), role.deletedAt());
            roles.put(saved.tenantId() + ":" + saved.code(), saved);
            return saved;
        }
    }

    private static final class EmptyUserRepository implements IamUserRepository {
        @Override
        public Optional<IamUser> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username) {
            return Optional.empty();
        }

        @Override
        public List<IamUser> findActiveUsers(Long tenantId) {
            return List.of();
        }

        @Override
        public IamUser save(IamUser user) {
            return user;
        }
    }

    private static final class ActiveUserRepository implements IamUserRepository {
        private final List<IamUser> users;

        private ActiveUserRepository(IamUser... users) {
            this.users = List.of(users);
        }

        @Override
        public Optional<IamUser> findById(Long id) {
            return users.stream()
                    .filter(user -> user.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username) {
            return users.stream()
                    .filter(user -> user.tenantId().equals(tenantId))
                    .filter(user -> user.username().equals(username))
                    .findFirst();
        }

        @Override
        public List<IamUser> findActiveUsers(Long tenantId) {
            return users.stream()
                    .filter(user -> user.tenantId().equals(tenantId))
                    .filter(IamUser::enabled)
                    .toList();
        }

        @Override
        public IamUser save(IamUser user) {
            return user;
        }
    }

    private static final class RecordingAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {
        private final List<IamAuthorizationSnapshot> saved = new ArrayList<>();

        @Override
        public Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) {
            return Optional.empty();
        }

        @Override
        public IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot) {
            saved.add(snapshot);
            return snapshot;
        }
    }

    private static final class NoOpAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {
        @Override
        public Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) {
            return Optional.empty();
        }

        @Override
        public IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot) {
            return snapshot;
        }
    }
}
