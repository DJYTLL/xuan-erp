package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.audit.AuditWriteEvent;
import com.xuan.erp.common.audit.AuditWriteOutcome;
import com.xuan.erp.common.audit.AuditWriteSubmission;
import com.xuan.erp.common.audit.SafeAuditWritePublisher;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.command.LoginIamUserCommand;
import com.xuan.erp.iam.application.command.RefreshIamTokenCommand;
import com.xuan.erp.iam.application.command.RevokeIamRefreshTokenCommand;
import com.xuan.erp.iam.application.port.IamAccessTokenIssuer;
import com.xuan.erp.iam.application.port.IamIssuedAccessToken;
import com.xuan.erp.iam.application.port.IamRefreshTokenGenerator;
import com.xuan.erp.iam.application.port.IamTenantStatusGateway;
import com.xuan.erp.iam.application.query.IamLoginView;
import com.xuan.erp.iam.application.query.IamTenantStatusView;
import com.xuan.erp.iam.application.service.IamAuthenticationApplicationService;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamRefreshToken;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamRefreshTokenRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import com.xuan.erp.iam.infrastructure.config.IamAuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
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
    private final IamAuthProperties authProperties = new IamAuthProperties();

    @Test
    void authenticatesUserAndReturnsIssuedAccessToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 2, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        snapshotRepository.store.put("1001:1", snapshot(1001L, 1L, 7L, List.of("iam:view", "iam:update")));
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                refreshTokenRepository,
                passwordEncoder,
                accessTokenIssuer,
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);

        IamLoginView view = service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!"));

        assertEquals("issued-access-token", view.accessToken());
        assertEquals(OffsetDateTime.parse("2026-07-08T10:15:30+08:00"), view.accessTokenExpiresAt());
        assertEquals("refresh-token-1", view.refreshToken());
        assertNotNull(view.refreshTokenExpiresAt());
        assertEquals(1L, view.currentUser().userId());
        assertEquals(1001L, view.currentUser().tenantId());
        assertEquals("admin", view.currentUser().username());
        assertEquals(Set.of("iam:view", "iam:update"), view.currentUser().permissions());
        assertEquals(7L, view.currentUser().authVersion());
        assertNotNull(userRepository.store.get(1L).lastLoginAt());
        assertEquals(0, userRepository.store.get(1L).failedLoginCount());
        assertEquals("admin", accessTokenIssuer.lastCurrentUser.username());
        IamRefreshToken storedToken = refreshTokenRepository.store.values().stream().findFirst().orElseThrow();
        assertEquals(1001L, storedToken.tenantId());
        assertEquals(1L, storedToken.userId());
        assertEquals(64, storedToken.tokenHash().length());
        assertTrue(!"refresh-token-1".equals(storedToken.tokenHash()));
        assertEquals(storedToken.expiresAt(), view.refreshTokenExpiresAt());
        assertNotNull(storedToken.tokenFamilyId());
        assertLastAudit(auditPublisher, "iam:auth:login-success", AuditWriteOutcome.SUCCESS, "admin", 1L);
    }

    @Test
    void rejectsLoginWhenTenantStatusDoesNotAllowLogin() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 0, null, null));
        StubTenantStatusGateway tenantStatusGateway = new StubTenantStatusGateway();
        tenantStatusGateway.store.put(1001L, new IamTenantStatusView(
                1001L,
                "acme",
                "DISABLED",
                false,
                "租户已停用",
                null));
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                accessTokenIssuer,
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher,
                tenantStatusGateway);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!")));

        assertEquals("IAM_TENANT_LOGIN_DISABLED", error.code());
        assertTrue(accessTokenIssuer.lastCurrentUser == null);
        assertEquals(1001L, tenantStatusGateway.lastTenantId);
        assertLastAudit(auditPublisher, "iam:auth:login-failed", AuditWriteOutcome.FAILED, "admin", 1L);
        assertEquals("IAM_TENANT_LOGIN_DISABLED", auditPublisher.events.getLast().errorCode());
    }

    @Test
    void authenticatesProvisionedTenantWhenTenantServiceAllowsLogin() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 0, null, null));
        StubTenantStatusGateway tenantStatusGateway = new StubTenantStatusGateway();
        tenantStatusGateway.store.put(1001L, new IamTenantStatusView(
                1001L,
                "acme",
                "PROVISIONED",
                true,
                null,
                null));
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                accessTokenIssuer,
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher,
                tenantStatusGateway);

        IamLoginView view = service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!"));

        assertEquals("issued-access-token", view.accessToken());
        assertEquals("PROVISIONED", tenantStatusGateway.store.get(1001L).status());
        assertEquals(1001L, tenantStatusGateway.lastTenantId);
        assertLastAudit(auditPublisher, "iam:auth:login-success", AuditWriteOutcome.SUCCESS, "admin", 1L);
    }

    @Test
    void rejectsLoginWhenTenantStatusIsMissing() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 0, null, null));
        StubTenantStatusGateway tenantStatusGateway = new StubTenantStatusGateway();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher,
                tenantStatusGateway);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!")));

        assertEquals("IAM_TENANT_NOT_FOUND", error.code());
        assertLastAudit(auditPublisher, "iam:auth:login-failed", AuditWriteOutcome.FAILED, "admin", 1L);
        assertEquals("IAM_TENANT_NOT_FOUND", auditPublisher.events.getLast().errorCode());
    }

    @Test
    void refreshRotatesRefreshTokenAndRevokesPreviousToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 0, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        snapshotRepository.store.put("1001:1", snapshot(1001L, 1L, 8L, List.of("iam:view")));
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        StubRefreshTokenGenerator refreshTokenGenerator = new StubRefreshTokenGenerator("refresh-token-1", "refresh-token-2");
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                refreshTokenRepository,
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                refreshTokenGenerator,
                authProperties,
                auditPublisher);
        IamLoginView loginView = service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!"));

        IamLoginView refreshedView = service.refresh(new RefreshIamTokenCommand(loginView.refreshToken()));

        assertEquals("refresh-token-2", refreshedView.refreshToken());
        assertEquals(2, refreshTokenRepository.store.size());
        IamRefreshToken oldToken = refreshTokenRepository.findByTokenHash(sha256Hex("refresh-token-1")).orElseThrow();
        IamRefreshToken newToken = refreshTokenRepository.findByTokenHash(sha256Hex("refresh-token-2")).orElseThrow();
        assertNotNull(oldToken.revokedAt());
        assertNotNull(oldToken.lastUsedAt());
        assertEquals(newToken.tokenHash(), oldToken.replacedByTokenHash());
        assertEquals(oldToken.tokenFamilyId(), newToken.tokenFamilyId());
        assertTrue(newToken.revokedAt() == null);
        assertEquals(8L, refreshedView.currentUser().authVersion());
        assertLastAudit(auditPublisher, "iam:auth:refresh-success", AuditWriteOutcome.SUCCESS, "admin", 1L, "IamRefreshToken");
        assertEquals("/api/iam/auth/refresh", auditPublisher.events.getLast().path());
    }

    @Test
    void rejectsReusedRefreshTokenAfterRotation() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 0, null, null));
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                refreshTokenRepository,
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                new StubRefreshTokenGenerator("refresh-token-1", "refresh-token-2"),
                authProperties,
                auditPublisher);
        IamLoginView loginView = service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!"));
        service.refresh(new RefreshIamTokenCommand(loginView.refreshToken()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.refresh(new RefreshIamTokenCommand("refresh-token-1")));

        assertEquals("IAM_REFRESH_TOKEN_REVOKED", error.code());
        assertLastAudit(auditPublisher, "iam:auth:refresh-replay-detected", AuditWriteOutcome.FAILED, "admin", 1L, "IamRefreshToken");
        assertEquals("/api/iam/auth/refresh", auditPublisher.events.getLast().path());
        assertEquals("IAM_REFRESH_TOKEN_REVOKED", auditPublisher.events.getLast().errorCode());
    }

    @Test
    void rejectsExpiredRefreshToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 0, null, null));
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        refreshTokenRepository.save(refreshToken(
                1001L,
                1L,
                sha256Hex("expired-refresh-token"),
                "family-1",
                OffsetDateTime.parse("2020-01-01T00:00:00+08:00"),
                null,
                null));
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                refreshTokenRepository,
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                new StubRefreshTokenGenerator("refresh-token-2"),
                authProperties,
                auditPublisher);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.refresh(new RefreshIamTokenCommand("expired-refresh-token")));

        assertEquals("IAM_REFRESH_TOKEN_EXPIRED", error.code());
        assertLastAudit(auditPublisher, "iam:auth:refresh-failed", AuditWriteOutcome.FAILED, "admin", 1L, "IamRefreshToken");
        assertEquals("/api/iam/auth/refresh", auditPublisher.events.getLast().path());
        assertEquals("IAM_REFRESH_TOKEN_EXPIRED", auditPublisher.events.getLast().errorCode());
    }

    @Test
    void logoutRevokesRefreshTokenAndPreventsFurtherRefresh() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 0, null, null));
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                refreshTokenRepository,
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                new StubRefreshTokenGenerator("refresh-token-1", "refresh-token-2"),
                authProperties,
                auditPublisher);
        IamLoginView loginView = service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!"));

        service.logout(new RevokeIamRefreshTokenCommand(loginView.refreshToken()));

        IamRefreshToken revokedToken = refreshTokenRepository.findByTokenHash(sha256Hex("refresh-token-1")).orElseThrow();
        assertNotNull(revokedToken.revokedAt());
        assertNotNull(revokedToken.lastUsedAt());
        assertTrue(revokedToken.replacedByTokenHash() == null);
        assertEquals("iam-logout", revokedToken.updatedBy());
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.refresh(new RefreshIamTokenCommand(loginView.refreshToken())));
        assertEquals("IAM_REFRESH_TOKEN_REVOKED", error.code());
        assertLastAudit(auditPublisher, "iam:auth:refresh-replay-detected", AuditWriteOutcome.FAILED, "admin", 1L, "IamRefreshToken");
    }

    @Test
    void logoutIsIdempotentForUnknownOrAlreadyRevokedRefreshToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 0, null, null));
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                refreshTokenRepository,
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);
        IamLoginView loginView = service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!"));

        service.logout(new RevokeIamRefreshTokenCommand("unknown-refresh-token"));
        service.logout(new RevokeIamRefreshTokenCommand(loginView.refreshToken()));
        service.logout(new RevokeIamRefreshTokenCommand(loginView.refreshToken()));

        IamRefreshToken revokedToken = refreshTokenRepository.findByTokenHash(sha256Hex("refresh-token-1")).orElseThrow();
        assertNotNull(revokedToken.revokedAt());
        assertEquals("iam-logout", revokedToken.updatedBy());
    }

    @Test
    void authenticatesPlatformSuperAdminWithTenantZero() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 0L, "super_admin", passwordEncoder.encode("123456"), true, 0, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        snapshotRepository.store.put("0:1", snapshot(0L, 1L, 1L, List.of("iam:view", "tenant:update")));
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                accessTokenIssuer,
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);

        IamLoginView view = service.login(new LoginIamUserCommand(0L, "super_admin", "123456"));

        assertEquals(0L, view.currentUser().tenantId());
        assertEquals("super_admin", view.currentUser().username());
        assertEquals(Set.of("super_admin"), view.currentUser().roles());
        assertEquals(Set.of("*", "iam:view", "tenant:update"), view.currentUser().permissions());
        assertLastAudit(auditPublisher, "iam:auth:login-success", AuditWriteOutcome.SUCCESS, "super_admin", 1L);
    }

    @Test
    void authenticatesPlatformSuperadminAliasWithTenantZero() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 0L, "superadmin", passwordEncoder.encode("123456"), true, 0, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        snapshotRepository.store.put("0:1", snapshot(0L, 1L, 1L, List.of("iam:view", "tenant-plan:manage")));
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                accessTokenIssuer,
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);

        IamLoginView view = service.login(new LoginIamUserCommand(0L, "superadmin", "123456"));

        assertEquals(0L, view.currentUser().tenantId());
        assertEquals("superadmin", view.currentUser().username());
        assertEquals(Set.of("super_admin"), view.currentUser().roles());
        assertEquals(Set.of("*", "iam:view", "tenant-plan:manage"), view.currentUser().permissions());
        assertEquals(Set.of("super_admin"), accessTokenIssuer.lastCurrentUser.roles());
        assertTrue(accessTokenIssuer.lastCurrentUser.permissions().contains("*"));
    }

    @Test
    void rejectsInvalidPasswordAndUpdatesFailedLoginState() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), true, 1, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "admin", "wrong-password")));

        assertEquals("IAM_INVALID_CREDENTIALS", error.code());
        assertEquals(2, userRepository.store.get(1L).failedLoginCount());
        assertNotNull(userRepository.store.get(1L).lastFailedLoginAt());
        assertLastAudit(auditPublisher, "iam:auth:login-failed", AuditWriteOutcome.FAILED, "admin", 1L);
    }

    @Test
    void auditsUnknownUserLoginFailure() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                accessTokenIssuer,
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "missing", "Passw0rd!")));

        assertEquals("IAM_INVALID_CREDENTIALS", error.code());
        assertTrue(accessTokenIssuer.lastCurrentUser == null);
        assertLastAudit(auditPublisher, "iam:auth:login-failed", AuditWriteOutcome.FAILED, "missing", null);
    }

    @Test
    void rejectsDisabledUserBeforeIssuingToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, user(1L, 1001L, "admin", passwordEncoder.encode("Passw0rd!"), false, 0, null, null));
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        StubIamAccessTokenIssuer accessTokenIssuer = new StubIamAccessTokenIssuer();
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                snapshotRepository,
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                accessTokenIssuer,
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!")));

        assertEquals("IAM_USER_DISABLED", error.code());
        assertTrue(accessTokenIssuer.lastCurrentUser == null);
        assertLastAudit(auditPublisher, "iam:auth:user-disabled", AuditWriteOutcome.FAILED, "admin", 1L);
    }

    @Test
    void auditsLockedUserBeforeIssuingToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, userWithSecurityStatus(
                1L,
                1001L,
                "admin",
                passwordEncoder.encode("Passw0rd!"),
                true,
                true,
                false,
                true,
                OffsetDateTime.parse("2026-07-08T10:00:00+08:00")));
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!")));

        assertEquals("IAM_USER_LOCKED", error.code());
        assertLastAudit(auditPublisher, "iam:auth:account-locked", AuditWriteOutcome.FAILED, "admin", 1L);
    }

    @Test
    void auditsExpiredCredentialsBeforeIssuingToken() {
        InMemoryIamUserRepository userRepository = new InMemoryIamUserRepository();
        userRepository.store.put(1L, userWithSecurityStatus(
                1L,
                1001L,
                "admin",
                passwordEncoder.encode("Passw0rd!"),
                true,
                true,
                true,
                false,
                null));
        CapturingAuditWritePublisher auditPublisher = new CapturingAuditWritePublisher();
        IamAuthenticationApplicationService service = new IamAuthenticationApplicationService(
                userRepository,
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryRefreshTokenRepository(),
                passwordEncoder,
                new StubIamAccessTokenIssuer(),
                new StubRefreshTokenGenerator("refresh-token-1"),
                authProperties,
                auditPublisher);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginIamUserCommand(1001L, "admin", "Passw0rd!")));

        assertEquals("IAM_USER_CREDENTIALS_EXPIRED", error.code());
        assertLastAudit(auditPublisher, "iam:auth:credentials-expired", AuditWriteOutcome.FAILED, "admin", 1L);
    }

    private static void assertLastAudit(
            CapturingAuditWritePublisher auditPublisher,
            String action,
            AuditWriteOutcome outcome,
            String username,
            Long userId
    ) {
        assertLastAudit(auditPublisher, action, outcome, username, userId, "IamUser");
    }

    private static void assertLastAudit(
            CapturingAuditWritePublisher auditPublisher,
            String action,
            AuditWriteOutcome outcome,
            String username,
            Long userId,
            String entityType
    ) {
        AuditWriteEvent event = auditPublisher.events.getLast();
        assertEquals(action, event.action());
        assertEquals(outcome, event.status());
        assertEquals(username, event.actorUsername());
        assertEquals(userId, event.actorUserId());
        assertEquals(entityType, event.entityType());
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

    private static IamUser userWithSecurityStatus(
            Long id,
            Long tenantId,
            String username,
            String passwordHash,
            boolean enabled,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            OffsetDateTime lockedUntil) {
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
                accountNonExpired,
                accountNonLocked,
                credentialsNonExpired,
                null,
                now,
                0,
                null,
                lockedUntil,
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

    private static IamRefreshToken refreshToken(
            Long tenantId,
            Long userId,
            String tokenHash,
            String tokenFamilyId,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt,
            String replacedByTokenHash) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-08T09:05:00+08:00");
        return new IamRefreshToken(
                null,
                tenantId,
                userId,
                tokenHash,
                tokenFamilyId,
                expiresAt,
                revokedAt,
                replacedByTokenHash,
                null,
                tenantId,
                null,
                null,
                null,
                "iam-auth",
                now,
                "iam-auth",
                now);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
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

    private static final class InMemoryRefreshTokenRepository implements IamRefreshTokenRepository {

        private long nextId = 1L;
        private final Map<String, IamRefreshToken> store = new LinkedHashMap<>();

        @Override
        public Optional<IamRefreshToken> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(store.get(tokenHash));
        }

        @Override
        public IamRefreshToken save(IamRefreshToken token) {
            IamRefreshToken saved = token.id() == null ? new IamRefreshToken(
                    nextId++,
                    token.tenantId(),
                    token.userId(),
                    token.tokenHash(),
                    token.tokenFamilyId(),
                    token.expiresAt(),
                    token.revokedAt(),
                    token.replacedByTokenHash(),
                    token.lastUsedAt(),
                    token.audienceTenantId(),
                    token.deviceId(),
                    token.ipAddress(),
                    token.userAgent(),
                    token.createdBy(),
                    token.createdAt(),
                    token.updatedBy(),
                    token.updatedAt()) : token;
            store.put(saved.tokenHash(), saved);
            return saved;
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

    private static final class StubRefreshTokenGenerator implements IamRefreshTokenGenerator {

        private final Deque<String> tokens;

        private StubRefreshTokenGenerator(String... tokens) {
            this.tokens = new ArrayDeque<>(List.of(tokens));
        }

        @Override
        public String generate() {
            return tokens.removeFirst();
        }
    }

    private static final class StubTenantStatusGateway implements IamTenantStatusGateway {

        private final Map<Long, IamTenantStatusView> store = new LinkedHashMap<>();
        private Long lastTenantId;

        @Override
        public Optional<IamTenantStatusView> findTenantStatus(Long tenantId) {
            lastTenantId = tenantId;
            return Optional.ofNullable(store.get(tenantId));
        }
    }

    private static final class CapturingAuditWritePublisher extends SafeAuditWritePublisher {

        private final List<AuditWriteEvent> events = new ArrayList<>();

        private CapturingAuditWritePublisher() {
            super(event -> null, Runnable::run);
        }

        @Override
        public AuditWriteSubmission publish(AuditWriteEvent event) {
            events.add(event);
            return AuditWriteSubmission.accepted();
        }
    }
}
