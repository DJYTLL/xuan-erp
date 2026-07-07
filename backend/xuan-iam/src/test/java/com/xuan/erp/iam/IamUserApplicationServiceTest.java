package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamUserCommand;
import com.xuan.erp.iam.application.command.DisableIamUserCommand;
import com.xuan.erp.iam.application.command.RebuildAuthorizationSnapshotCommand;
import com.xuan.erp.iam.application.query.IamAuthorizationSnapshotView;
import com.xuan.erp.iam.application.query.IamUserDetailView;
import com.xuan.erp.iam.application.service.IamAuthorizationApplicationService;
import com.xuan.erp.iam.application.service.IamUserApplicationService;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
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
        IamUserApplicationService service = new IamUserApplicationService(userRepository);

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
        IamUserApplicationService service = new IamUserApplicationService(userRepository);
        service.createUser(new CreateIamUserCommand(1001L, "admin", "{bcrypt}hash", "管理员", null, null, null));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createUser(new CreateIamUserCommand(1001L, "admin", "{bcrypt}another", "重复账号", null, null, null)));

        assertEquals("IAM_USERNAME_EXISTS", error.code());
    }

    @Test
    void disablesUserAndIncrementsAuthVersion() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        IamUserApplicationService service = new IamUserApplicationService(userRepository);
        Long userId = service.createUser(new CreateIamUserCommand(1001L, "admin", "{bcrypt}hash", "管理员", null, null, null)).id();

        IamUserDetailView disabled = service.disableUser(userId, new DisableIamUserCommand("离职停用", "security-admin"));

        assertFalse(disabled.enabled());
        assertEquals(1L, disabled.authVersion());
        assertEquals("离职停用", userRepository.store.get(userId).deleteReason());
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
