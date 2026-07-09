package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.command.LoginIamUserCommand;
import com.xuan.erp.iam.application.port.IamAccessTokenIssuer;
import com.xuan.erp.iam.application.port.IamIssuedAccessToken;
import com.xuan.erp.iam.application.query.IamLoginView;
import com.xuan.erp.iam.application.service.IamAuthenticationApplicationService;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamAuthenticationApplicationServiceTest {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void authenticatesUserAndReturnsIssuedAccessToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 2, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        snapshotRepository.store.put("1001:1", snapshot(1001L, 1L, 7L, List.of("iam:view", "iam:update")));
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                passwordEncoder,
                accessTokenIssuer);

        IamLoginView view = service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!"));

        assertEquals("issued-access-token", view.accessToken());
        assertEquals(OffsetDateTime.parse("2026-07-08T10:15:30+08:00"), view.accessTokenExpiresAt());
        assertEquals(1L, view.currentUser().userId());
        assertEquals(1001L, view.currentUser().tenantId());
        assertEquals("admin", view.currentUser().username());
        assertEquals(Set.of("iam:view", "iam:update"), view.currentUser().permissions());
        assertEquals(7L, view.currentUser().authVersion());
        assertNotNull(userRepository.store.get(1L).lastLoginAt());
        assertEquals(0, userRepository.store.get(1L).failedLoginCount());
        assertEquals("admin", accessTokenIssuer.lastCurrentUser.username());
    }

    @Test
    void authenticatesPlatformSuperAdminWithTenantZero() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 0L, "super_admin", passwordEncoder.encode("123456"), true, 0, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        snapshotRepository.store.put("0:1", snapshot(0L, 1L, 1L, List.of("iam:view", "tenant:manage")));
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                passwordEncoder,
                accessTokenIssuer);

        IamLoginView view = service.login(new LoginIamUserCommand(0L, "super_admin", "123456"));

        assertEquals(0L, view.currentUser().tenantId());
        assertEquals("super_admin", view.currentUser().username());
        assertEquals(Set.of("super_admin"), view.currentUser().roles());
        assertEquals(Set.of("iam:view", "tenant:manage"), view.currentUser().permissions());
    }

    @Test
    void rejectsInvalidPasswordAndUpdatesFailedLoginState() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 1, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                passwordEncoder,
                new StubIamAccessTokenIssuer());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "admin", "wrong-password")));

        assertEquals("IAM_INVALID_CREDENTIALS", error.code());
        assertEquals(2, userRepository.store.get(1L).failedLoginCount());
        assertNotNull(userRepository.store.get(1L).lastFailedLoginAt());
    }

    @Test
    void rejectsDisabledUserBeforeIssuingToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), false, 0, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                passwordEncoder,
                accessTokenIssuer);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!")));

        assertEquals("IAM_USER_DISABLED", error.code());
        assertTrue(accessTokenIssuer.lastCurrentUser == null);
    }

    private static IamUser user(
            Long id,
            Long tenantId,
            String username,
            String passwordHash,
            boolean enabled,
            int failedLoginCount,
            OffsetDateTime lastLoginAt,
            OffsetDateTime lastFailedLoginAt) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-08T09:00:00+08:00");
        return new IamUser(
                id,
                tenantId,
                username,
                passwordHash,
                "管理员",
                null,
                null,
                null,
                enabled,
                true,
                true,
                true,
                lastLoginAt,
                now,
                failedLoginCount,
                lastFailedLoginAt,
                null,
                false,
                3L,
                null,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static IamAuthorizationSnapshot snapshot(Long tenantId, Long userId, Long authVersion, List<String> permissions) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-08T09:05:00+08:00");
        return new IamAuthorizationSnapshot(
                1L,
                tenantId,
                userId,
                authVersion,
                List.of(),
                permissions,
                List.of("system"),
                Map.of(),
                String.join(",", permissions),
                null,
                now,
                "system",
                now,
                "system",
                now);
    }

    private static final class InMemoryIamUserRepository implements IamUserRepository {

        private final Map<Long, IamUser> store = new LinkedHashMap<>();

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
                    .toList();
        }

        @Override
        public IamUser save(IamUser user) {
            store.put(user.id(), user);
            return user;
        }
    }

    private static final class InMemoryAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {

        private final Map<String, IamAuthorizationSnapshot> store = new LinkedHashMap<>();

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

    private static final class StubIamAccessTokenIssuer implements IamAccessTokenIssuer {

        private CurrentUser lastCurrentUser;

        @Override
        public IamIssuedAccessToken issue(CurrentUser currentUser) {
            this.lastCurrentUser = currentUser;
            return new IamIssuedAccessToken(
                    "issued-access-token",
                    OffsetDateTime.parse("2026-07-08T10:15:30+08:00"));
        }

        @Override
        public Map<String, Object> publicJwkSet() {
            return Map.of();
        }
    }
}
