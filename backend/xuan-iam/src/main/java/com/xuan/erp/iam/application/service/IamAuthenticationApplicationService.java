package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.audit.AuditWriteEvent;
import com.xuan.erp.common.audit.AuditWriteOutcome;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 认证应用服务，负责登录校验、登录态更新和访问令牌签发。
 */
@Service
public class IamAuthenticationApplicationService {

    private static final String ACTION_LOGIN_SUCCESS = "iam:auth:login-success";
    private static final String ACTION_LOGIN_FAILED = "iam:auth:login-failed";
    private static final String ACTION_USER_DISABLED = "iam:auth:user-disabled";
    private static final String ACTION_ACCOUNT_LOCKED = "iam:auth:account-locked";
    private static final String ACTION_CREDENTIALS_EXPIRED = "iam:auth:credentials-expired";
    private static final String ACTION_ACCOUNT_EXPIRED = "iam:auth:account-expired";
    private static final String ACTION_REFRESH_SUCCESS = "iam:auth:refresh-success";
    private static final String ACTION_REFRESH_FAILED = "iam:auth:refresh-failed";
    private static final String ACTION_REFRESH_REPLAY_DETECTED = "iam:auth:refresh-replay-detected";
    private static final String ACTION_LOGOUT_SUCCESS = "iam:auth:logout-success";
    private static final String ACTION_LOGOUT_IGNORED = "iam:auth:logout-ignored";
    private static final String ENTITY_TYPE_IAM_USER = "IamUser";
    private static final String ENTITY_TYPE_REFRESH_TOKEN = "IamRefreshToken";
    private static final String REFRESH_PATH = "/api/iam/auth/refresh";
    private static final String LOGOUT_PATH = "/api/iam/auth/logout";
    private static final Set<String> PLATFORM_SUPER_ADMIN_USERNAMES = Set.of("super_admin", "superadmin");
    private static final Set<String> PLATFORM_TENANT_CODES = Set.of("0", "platform", "super", "super_admin");
    private static final IamTenantStatusView PLATFORM_TENANT_STATUS = new IamTenantStatusView(
            0L,
            "platform",
            "平台租户",
            "ENABLED",
            true,
            null,
            null);

    private final IamUserRepository userRepository;
    private final IamAuthorizationSnapshotRepository authorizationSnapshotRepository;
    private final IamRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamAccessTokenIssuer accessTokenIssuer;
    private final IamRefreshTokenGenerator refreshTokenGenerator;
    private final IamAuthProperties authProperties;
    private final SafeAuditWritePublisher auditWritePublisher;
    private final IamTenantStatusGateway tenantStatusGateway;
    private final IamTenantBootstrapApplicationService tenantBootstrapApplicationService;

    public IamAuthenticationApplicationService(
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository authorizationSnapshotRepository,
            IamRefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            IamAccessTokenIssuer accessTokenIssuer,
            IamRefreshTokenGenerator refreshTokenGenerator,
            IamAuthProperties authProperties,
            SafeAuditWritePublisher auditWritePublisher) {
        this(
                userRepository,
                authorizationSnapshotRepository,
                refreshTokenRepository,
                passwordEncoder,
                accessTokenIssuer,
                refreshTokenGenerator,
                authProperties,
                auditWritePublisher,
                tenantId -> {
                    throw new BusinessException("IAM_TENANT_STATUS_UNAVAILABLE", "租户状态暂时无法确认，请稍后再试");
                },
                null);
    }

    @Autowired
    public IamAuthenticationApplicationService(
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository authorizationSnapshotRepository,
            IamRefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            IamAccessTokenIssuer accessTokenIssuer,
            IamRefreshTokenGenerator refreshTokenGenerator,
            IamAuthProperties authProperties,
            SafeAuditWritePublisher auditWritePublisher,
            IamTenantStatusGateway tenantStatusGateway,
            @org.springframework.lang.Nullable IamTenantBootstrapApplicationService tenantBootstrapApplicationService) {
        this.userRepository = userRepository;
        this.authorizationSnapshotRepository = authorizationSnapshotRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.authProperties = authProperties;
        this.auditWritePublisher = auditWritePublisher;
        this.tenantStatusGateway = tenantStatusGateway;
        this.tenantBootstrapApplicationService = tenantBootstrapApplicationService;
    }

    public IamAuthenticationApplicationService(
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository authorizationSnapshotRepository,
            IamRefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            IamAccessTokenIssuer accessTokenIssuer,
            IamRefreshTokenGenerator refreshTokenGenerator,
            IamAuthProperties authProperties,
            SafeAuditWritePublisher auditWritePublisher,
            IamTenantStatusGateway tenantStatusGateway) {
        this(
                userRepository,
                authorizationSnapshotRepository,
                refreshTokenRepository,
                passwordEncoder,
                accessTokenIssuer,
                refreshTokenGenerator,
                authProperties,
                auditWritePublisher,
                tenantStatusGateway,
                null);
    }

    @Transactional
    public IamLoginView login(LoginIamUserCommand command) {
        String username = IamUser.normalizeUsername(command.username());
        String password = requireText(command.password(), "密码不能为空");
        ResolvedLoginTenant loginTenant = resolveLoginTenant(command);
        IamUser user = userRepository.findActiveByTenantIdAndUsername(loginTenant.tenantId(), username)
                .orElse(null);
        if (user == null) {
            publishLoginAudit(loginTenant.tenantId(), username, null, ACTION_LOGIN_FAILED, AuditWriteOutcome.FAILED,
                    "IAM_INVALID_CREDENTIALS", "用户名或密码错误");
            throw new BusinessException("IAM_INVALID_CREDENTIALS", "用户名或密码错误");
        }
        validateTenantStatus(loginTenant, username, user);
        validateUserStatus(loginTenant.tenantId(), username, user);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            recordFailedLogin(user);
            publishLoginAudit(loginTenant.tenantId(), username, user, ACTION_LOGIN_FAILED, AuditWriteOutcome.FAILED,
                    "IAM_INVALID_CREDENTIALS", "用户名或密码错误");
            throw new BusinessException("IAM_INVALID_CREDENTIALS", "用户名或密码错误");
        }
        IamUser saved = recordSuccessfulLogin(user);
        CurrentUser currentUser = buildCurrentUser(saved);
        IamIssuedAccessToken issuedAccessToken = accessTokenIssuer.issue(currentUser);
        PreparedRefreshToken issuedRefreshToken = prepareRefreshToken(saved, null, OffsetDateTime.now());
        IamTenantStatusView tenantDisplay = resolveTenantDisplay(saved, loginTenant);
        refreshTokenRepository.save(issuedRefreshToken.token());
        publishLoginAudit(loginTenant.tenantId(), username, saved, ACTION_LOGIN_SUCCESS, AuditWriteOutcome.SUCCESS,
                null, null);
        return new IamLoginView(
                issuedAccessToken.accessToken(),
                issuedAccessToken.expiresAt(),
                issuedRefreshToken.rawToken(),
                issuedRefreshToken.token().expiresAt(),
                tenantDisplay.code(),
                tenantDisplay.name(),
                currentUser);
    }

    @Transactional
    public IamLoginView refresh(RefreshIamTokenCommand command) {
        String rawRefreshToken;
        try {
            rawRefreshToken = requireText(command.refreshToken(), "刷新令牌不能为空");
        } catch (BusinessException ex) {
            publishRefreshAudit(null, null, null, null, ACTION_REFRESH_FAILED, AuditWriteOutcome.FAILED,
                    ex.code(), ex.getMessage());
            throw ex;
        }
        OffsetDateTime now = OffsetDateTime.now();
        String tokenHash = hashRefreshToken(rawRefreshToken);
        IamRefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (existing == null) {
            publishRefreshAudit(null, null, null, null, ACTION_REFRESH_FAILED, AuditWriteOutcome.FAILED,
                    "IAM_REFRESH_TOKEN_INVALID", "刷新令牌无效");
            throw new BusinessException("IAM_REFRESH_TOKEN_INVALID", "刷新令牌无效");
        }
        IamUser auditUser = findRefreshTokenUser(existing).orElse(null);
        if (existing.revokedAt() != null) {
            publishRefreshAudit(existing.tenantId(), auditUser == null ? null : auditUser.username(), existing.userId(),
                    existing, ACTION_REFRESH_REPLAY_DETECTED, AuditWriteOutcome.FAILED,
                    "IAM_REFRESH_TOKEN_REVOKED", "刷新令牌已被撤销");
            throw new BusinessException("IAM_REFRESH_TOKEN_REVOKED", "刷新令牌已被撤销");
        }
        if (!existing.expiresAt().isAfter(now)) {
            publishRefreshAudit(existing.tenantId(), auditUser == null ? null : auditUser.username(), existing.userId(),
                    existing, ACTION_REFRESH_FAILED, AuditWriteOutcome.FAILED,
                    "IAM_REFRESH_TOKEN_EXPIRED", "刷新令牌已过期");
            throw new BusinessException("IAM_REFRESH_TOKEN_EXPIRED", "刷新令牌已过期");
        }
        IamUser user = auditUser;
        if (user == null) {
            publishRefreshAudit(existing.tenantId(), null, existing.userId(), existing,
                    ACTION_REFRESH_FAILED, AuditWriteOutcome.FAILED,
                    "IAM_REFRESH_TOKEN_USER_INVALID", "刷新令牌绑定用户无效");
            throw new BusinessException("IAM_REFRESH_TOKEN_USER_INVALID", "刷新令牌绑定用户无效");
        }
        try {
            validateUserStatusForRefresh(user);
            validateTenantStatusForRefresh(user);
        } catch (BusinessException ex) {
            publishRefreshAudit(existing.tenantId(), user.username(), user.id(), existing,
                    ACTION_REFRESH_FAILED, AuditWriteOutcome.FAILED, ex.code(), ex.getMessage());
            throw ex;
        }

        CurrentUser currentUser = buildCurrentUser(user);
        IamIssuedAccessToken issuedAccessToken = accessTokenIssuer.issue(currentUser);
        PreparedRefreshToken newRefreshToken = prepareRefreshToken(user, existing.tokenFamilyId(), now);
        IamTenantStatusView tenantDisplay = resolveTenantDisplay(user, new ResolvedLoginTenant(user.tenantId(), null));
        refreshTokenRepository.save(revokeRefreshToken(existing, newRefreshToken.token().tokenHash(), now));
        refreshTokenRepository.save(newRefreshToken.token());
        publishRefreshAudit(user.tenantId(), user.username(), user.id(), existing,
                ACTION_REFRESH_SUCCESS, AuditWriteOutcome.SUCCESS, null, null);
        return new IamLoginView(
                issuedAccessToken.accessToken(),
                issuedAccessToken.expiresAt(),
                newRefreshToken.rawToken(),
                newRefreshToken.token().expiresAt(),
                tenantDisplay.code(),
                tenantDisplay.name(),
                currentUser);
    }

    public void validateCurrentTenantStatus(CurrentUser currentUser) {
        if (currentUser == null || currentUser.tenantId() == null) {
            throw new BusinessException("IAM_UNAUTHORIZED", "当前请求未包含 IAM 登录上下文");
        }
        if (isPlatformSuperAdmin(currentUser)) {
            return;
        }
        validateTenantStatusOrThrow(currentUser.tenantId());
    }

    public IamTenantStatusView resolveCurrentTenantDisplay(CurrentUser currentUser) {
        if (currentUser == null || currentUser.tenantId() == null) {
            throw new BusinessException("IAM_UNAUTHORIZED", "当前请求未包含 IAM 登录上下文");
        }
        if (isPlatformSuperAdmin(currentUser)) {
            return PLATFORM_TENANT_STATUS;
        }
        return resolveTenantStatusById(currentUser.tenantId());
    }

    @Transactional
    public void logout(RevokeIamRefreshTokenCommand command) {
        String rawRefreshToken = requireText(command.refreshToken(), "刷新令牌不能为空");
        OffsetDateTime now = OffsetDateTime.now();
        String tokenHash = hashRefreshToken(rawRefreshToken);
        IamRefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (existing == null || existing.revokedAt() != null || !existing.expiresAt().isAfter(now)) {
            publishLogoutAudit(null, null, null, existing, ACTION_LOGOUT_IGNORED,
                    AuditWriteOutcome.SUCCESS, null, null);
            return;
        }
        IamUser auditUser = findRefreshTokenUser(existing).orElse(null);
        refreshTokenRepository.save(revokeRefreshTokenForLogout(existing, now));
        publishLogoutAudit(existing.tenantId(), auditUser == null ? null : auditUser.username(), existing.userId(), existing,
                ACTION_LOGOUT_SUCCESS, AuditWriteOutcome.SUCCESS, null, null);
    }

    private CurrentUser buildCurrentUser(IamUser user) {
        Set<String> roles = platformRoles(user);
        return authorizationSnapshotRepository.findByTenantIdAndUserId(user.tenantId(), user.id())
                .map(snapshot -> new CurrentUser(
                        user.id(),
                        user.tenantId(),
                        user.username(),
                        roles,
                        snapshot.authVersion(),
                        permissions(user, snapshot.permissionCodes())))
                .orElseGet(() -> new CurrentUser(
                        user.id(),
                        user.tenantId(),
                        user.username(),
                        roles,
                        user.authVersion(),
                        permissions(user, Set.of())));
    }

    private Set<String> platformRoles(IamUser user) {
        if (isPlatformSuperAdmin(user)) {
            return Set.of("super_admin");
        }
        return Set.of();
    }

    private Set<String> permissions(IamUser user, Iterable<String> permissionCodes) {
        Set<String> permissions = new LinkedHashSet<>();
        for (String permissionCode : permissionCodes) {
            if (permissionCode != null && !permissionCode.isBlank()) {
                permissions.add(permissionCode.trim());
            }
        }
        if (isPlatformSuperAdmin(user)) {
            permissions.add("*");
        }
        return Set.copyOf(permissions);
    }

    private boolean isPlatformSuperAdmin(IamUser user) {
        return Long.valueOf(0L).equals(user.tenantId()) && PLATFORM_SUPER_ADMIN_USERNAMES.contains(user.username());
    }

    private boolean isPlatformSuperAdmin(CurrentUser currentUser) {
        return Long.valueOf(0L).equals(currentUser.tenantId())
                && (currentUser.roles().contains("super_admin")
                || currentUser.permissions().contains("*")
                || PLATFORM_SUPER_ADMIN_USERNAMES.contains(currentUser.username()));
    }

    private IamTenantStatusView resolveTenantDisplay(IamUser user, ResolvedLoginTenant loginTenant) {
        if (isPlatformSuperAdmin(user)) {
            return PLATFORM_TENANT_STATUS;
        }
        if (loginTenant.status() != null) {
            return normalizeTenantDisplay(loginTenant.status(), user.tenantId());
        }
        return resolveTenantStatusById(user.tenantId());
    }

    private IamTenantStatusView resolveTenantStatusById(Long tenantId) {
        try {
            return normalizeTenantDisplay(tenantStatusGateway.findTenantStatus(tenantId).orElse(null), tenantId);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException("IAM_TENANT_STATUS_UNAVAILABLE", "租户状态暂时无法确认，请稍后再试");
        }
    }

    private IamTenantStatusView normalizeTenantDisplay(IamTenantStatusView tenantStatus, Long fallbackTenantId) {
        if (tenantStatus == null) {
            throw new BusinessException("IAM_TENANT_NOT_FOUND", "租户不存在");
        }
        String tenantCode = hasText(tenantStatus.code()) ? tenantStatus.code().trim() : String.valueOf(fallbackTenantId);
        String tenantName = hasText(tenantStatus.name()) ? tenantStatus.name().trim() : tenantCode;
        return new IamTenantStatusView(
                tenantStatus.tenantId() == null ? fallbackTenantId : tenantStatus.tenantId(),
                tenantCode,
                tenantName,
                tenantStatus.status(),
                tenantStatus.loginAllowed(),
                tenantStatus.loginDeniedReason(),
                tenantStatus.currentPlanExpiresAt(),
                tenantStatus.permissionHash(),
                tenantStatus.iamInitTemplateCode(),
                tenantStatus.columnPermissionTemplateCodes(),
                tenantStatus.defaultColumnPermissionTemplateCode());
    }

    private IamUser recordSuccessfulLogin(IamUser user) {
        OffsetDateTime now = OffsetDateTime.now();
        return userRepository.save(new IamUser(
                user.id(),
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
                now,
                user.passwordChangedAt(),
                0,
                null,
                user.lockedUntil(),
                user.mfaEnabled(),
                user.authVersion(),
                user.remark(),
                user.createdBy(),
                user.createdAt(),
                "iam-login",
                now,
                user.deletedBy(),
                user.deleteReason(),
                user.deletedAt()));
    }

    private void recordFailedLogin(IamUser user) {
        OffsetDateTime now = OffsetDateTime.now();
        userRepository.save(new IamUser(
                user.id(),
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
                user.failedLoginCount() + 1,
                now,
                user.lockedUntil(),
                user.mfaEnabled(),
                user.authVersion(),
                user.remark(),
                user.createdBy(),
                user.createdAt(),
                "iam-login",
                now,
                user.deletedBy(),
                user.deleteReason(),
                user.deletedAt()));
    }

    private void validateUserStatus(Long tenantId, String username, IamUser user) {
        if (!user.enabled()) {
            publishLoginAudit(tenantId, username, user, ACTION_USER_DISABLED, AuditWriteOutcome.FAILED,
                    "IAM_USER_DISABLED", "用户已被停用");
            throw new BusinessException("IAM_USER_DISABLED", "用户已被停用");
        }
        if (!user.accountNonExpired()) {
            publishLoginAudit(tenantId, username, user, ACTION_ACCOUNT_EXPIRED, AuditWriteOutcome.FAILED,
                    "IAM_USER_EXPIRED", "用户账号已过期");
            throw new BusinessException("IAM_USER_EXPIRED", "用户账号已过期");
        }
        if (!user.accountNonLocked() || (user.lockedUntil() != null && user.lockedUntil().isAfter(OffsetDateTime.now()))) {
            publishLoginAudit(tenantId, username, user, ACTION_ACCOUNT_LOCKED, AuditWriteOutcome.FAILED,
                    "IAM_USER_LOCKED", "用户账号已被锁定");
            throw new BusinessException("IAM_USER_LOCKED", "用户账号已被锁定");
        }
        if (!user.credentialsNonExpired()) {
            publishLoginAudit(tenantId, username, user, ACTION_CREDENTIALS_EXPIRED, AuditWriteOutcome.FAILED,
                    "IAM_USER_CREDENTIALS_EXPIRED", "用户凭证已过期");
            throw new BusinessException("IAM_USER_CREDENTIALS_EXPIRED", "用户凭证已过期");
        }
    }

    private void validateTenantStatus(ResolvedLoginTenant loginTenant, String username, IamUser user) {
        if (isPlatformSuperAdmin(user)) {
            return;
        }
        try {
            validateTenantStatusOrThrow(loginTenant);
        } catch (BusinessException ex) {
            publishLoginAudit(loginTenant.tenantId(), username, user, ACTION_LOGIN_FAILED, AuditWriteOutcome.FAILED,
                    ex.code(), ex.getMessage());
            throw ex;
        }
    }

    private void validateTenantStatusForRefresh(IamUser user) {
        if (isPlatformSuperAdmin(user)) {
            return;
        }
        validateTenantStatusOrThrow(user.tenantId());
    }

    private void validateTenantStatusOrThrow(Long tenantId) {
        validateTenantStatusOrThrow(new ResolvedLoginTenant(tenantId, null));
    }

    private void validateTenantStatusOrThrow(ResolvedLoginTenant loginTenant) {
        IamTenantStatusView tenantStatus;
        try {
            tenantStatus = loginTenant.status() == null
                    ? tenantStatusGateway.findTenantStatus(loginTenant.tenantId()).orElse(null)
                    : loginTenant.status();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException("IAM_TENANT_STATUS_UNAVAILABLE", "租户状态暂时无法确认，请稍后再试");
        }
        if (tenantStatus == null) {
            throw new BusinessException("IAM_TENANT_NOT_FOUND", "租户不存在");
        }
        if (!tenantStatus.loginAllowed()) {
            String message = hasText(tenantStatus.loginDeniedReason()) ? tenantStatus.loginDeniedReason() : "租户当前状态不允许登录";
            throw new BusinessException("IAM_TENANT_LOGIN_DISABLED", message);
        }
        ensureTenantPermissionSynced(tenantStatus);
    }

    private void ensureTenantPermissionSynced(IamTenantStatusView tenantStatus) {
        if (tenantBootstrapApplicationService == null || tenantStatus == null) {
            return;
        }
        tenantBootstrapApplicationService.ensureTenantPermissionSynced(
                tenantStatus.tenantId(),
                tenantStatus.iamInitTemplateCode(),
                tenantStatus.columnPermissionTemplateCodes(),
                tenantStatus.defaultColumnPermissionTemplateCode(),
                tenantStatus.permissionHash(),
                "iam-auth-auto-sync");
    }

    private ResolvedLoginTenant resolveLoginTenant(LoginIamUserCommand command) {
        String tenantCode = trimToNull(command.tenantCode());
        if (tenantCode != null) {
            String normalizedTenantCode = tenantCode.toLowerCase(java.util.Locale.ROOT);
            if (PLATFORM_TENANT_CODES.contains(normalizedTenantCode)) {
                return new ResolvedLoginTenant(0L, PLATFORM_TENANT_STATUS);
            }
            IamTenantStatusView tenantStatus;
            try {
                tenantStatus = tenantStatusGateway.findTenantStatusByCode(normalizedTenantCode).orElse(null);
            } catch (BusinessException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new BusinessException("IAM_TENANT_STATUS_UNAVAILABLE", "租户状态暂时无法确认，请稍后再试");
            }
            if (tenantStatus == null) {
                throw new BusinessException("IAM_TENANT_NOT_FOUND", "租户不存在");
            }
            requireTenantId(tenantStatus.tenantId(), "租户状态返回的租户 ID 无效");
            return new ResolvedLoginTenant(tenantStatus.tenantId(), tenantStatus);
        }
        requireTenantId(command.tenantId(), "租户编码或租户 ID 不能为空");
        return new ResolvedLoginTenant(command.tenantId(), null);
    }

    private void validateUserStatusForRefresh(IamUser user) {
        if (!user.enabled()) {
            throw new BusinessException("IAM_USER_DISABLED", "用户已被停用");
        }
        if (!user.accountNonExpired()) {
            throw new BusinessException("IAM_USER_EXPIRED", "用户账号已过期");
        }
        if (!user.accountNonLocked() || (user.lockedUntil() != null && user.lockedUntil().isAfter(OffsetDateTime.now()))) {
            throw new BusinessException("IAM_USER_LOCKED", "用户账号已被锁定");
        }
        if (!user.credentialsNonExpired()) {
            throw new BusinessException("IAM_USER_CREDENTIALS_EXPIRED", "用户凭证已过期");
        }
    }

    private java.util.Optional<IamUser> findRefreshTokenUser(IamRefreshToken token) {
        return userRepository.findById(token.userId())
                .filter(candidate -> candidate.tenantId().equals(token.tenantId()));
    }

    private PreparedRefreshToken prepareRefreshToken(IamUser user, String tokenFamilyId, OffsetDateTime now) {
        String rawToken = refreshTokenGenerator.generate();
        String tokenHash = hashRefreshToken(rawToken);
        String familyId = tokenFamilyId == null || tokenFamilyId.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : tokenFamilyId;
        OffsetDateTime expiresAt = now.plus(refreshTokenTtl());
        IamRefreshToken token = new IamRefreshToken(
                null,
                user.tenantId(),
                user.id(),
                tokenHash,
                familyId,
                expiresAt,
                null,
                null,
                null,
                user.tenantId(),
                null,
                null,
                null,
                "iam-auth",
                now,
                "iam-auth",
                now);
        return new PreparedRefreshToken(rawToken, token);
    }

    private IamRefreshToken revokeRefreshToken(IamRefreshToken token, String replacedByTokenHash, OffsetDateTime now) {
        return new IamRefreshToken(
                token.id(),
                token.tenantId(),
                token.userId(),
                token.tokenHash(),
                token.tokenFamilyId(),
                token.expiresAt(),
                now,
                replacedByTokenHash,
                now,
                token.audienceTenantId(),
                token.deviceId(),
                token.ipAddress(),
                token.userAgent(),
                token.createdBy(),
                token.createdAt(),
                "iam-refresh",
                now);
    }

    private IamRefreshToken revokeRefreshTokenForLogout(IamRefreshToken token, OffsetDateTime now) {
        return new IamRefreshToken(
                token.id(),
                token.tenantId(),
                token.userId(),
                token.tokenHash(),
                token.tokenFamilyId(),
                token.expiresAt(),
                now,
                token.replacedByTokenHash(),
                now,
                token.audienceTenantId(),
                token.deviceId(),
                token.ipAddress(),
                token.userAgent(),
                token.createdBy(),
                token.createdAt(),
                "iam-logout",
                now);
    }

    private Duration refreshTokenTtl() {
        Duration ttl = authProperties.getRefreshTokenTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("xuan.iam.auth.refresh-token-ttl 必须大于 0");
        }
        return ttl;
    }

    private String hashRefreshToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", ex);
        }
    }

    private void publishLoginAudit(
            Long tenantId,
            String username,
            IamUser user,
            String action,
            AuditWriteOutcome outcome,
            String errorCode,
            String errorMessage
    ) {
        auditWritePublisher.publish(new AuditWriteEvent(
                tenantId,
                username,
                user == null ? null : user.id(),
                action,
                ENTITY_TYPE_IAM_USER,
                user == null ? username : String.valueOf(user.id()),
                errorCode == null ? null : "reason=" + errorCode,
                outcome,
                null,
                null,
                null,
                null,
                "POST",
                "/api/iam/auth/login",
                null,
                errorCode,
                errorMessage,
                tenantId,
                null,
                false));
    }

    private void publishRefreshAudit(
            Long tenantId,
            String username,
            Long userId,
            IamRefreshToken refreshToken,
            String action,
            AuditWriteOutcome outcome,
            String errorCode,
            String errorMessage
    ) {
        auditWritePublisher.publish(new AuditWriteEvent(
                tenantId,
                username,
                userId,
                action,
                ENTITY_TYPE_REFRESH_TOKEN,
                refreshToken == null || refreshToken.id() == null ? null : String.valueOf(refreshToken.id()),
                refreshToken == null ? null : "tokenFamilyId=" + refreshToken.tokenFamilyId(),
                outcome,
                null,
                null,
                null,
                null,
                "POST",
                REFRESH_PATH,
                null,
                errorCode,
                errorMessage,
                tenantId,
                null,
                false));
    }

    private void publishLogoutAudit(
            Long tenantId,
            String username,
            Long userId,
            IamRefreshToken refreshToken,
            String action,
            AuditWriteOutcome outcome,
            String errorCode,
            String errorMessage
    ) {
        auditWritePublisher.publish(new AuditWriteEvent(
                tenantId,
                username,
                userId,
                action,
                ENTITY_TYPE_REFRESH_TOKEN,
                refreshToken == null || refreshToken.id() == null ? null : String.valueOf(refreshToken.id()),
                refreshToken == null ? null : "tokenFamilyId=" + refreshToken.tokenFamilyId(),
                outcome,
                null,
                null,
                null,
                null,
                "POST",
                LOGOUT_PATH,
                null,
                errorCode,
                errorMessage,
                tenantId,
                null,
                false));
    }

    private void requireTenantId(Long value, String message) {
        if (value == null || value < 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record PreparedRefreshToken(String rawToken, IamRefreshToken token) {
    }

    private record ResolvedLoginTenant(Long tenantId, IamTenantStatusView status) {
    }
}
