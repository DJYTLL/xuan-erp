package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.command.LoginIamUserCommand;
import com.xuan.erp.iam.application.port.IamAccessTokenIssuer;
import com.xuan.erp.iam.application.port.IamIssuedAccessToken;
import com.xuan.erp.iam.application.query.IamLoginView;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * IAM 认证应用服务，负责登录校验、登录态更新和访问令牌签发。
 */
@Service
public class IamAuthenticationApplicationService {

    private final IamUserRepository userRepository;
    private final IamAuthorizationSnapshotRepository authorizationSnapshotRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamAccessTokenIssuer accessTokenIssuer;

    public IamAuthenticationApplicationService(
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository authorizationSnapshotRepository,
            PasswordEncoder passwordEncoder,
            IamAccessTokenIssuer accessTokenIssuer) {
        this.userRepository = userRepository;
        this.authorizationSnapshotRepository = authorizationSnapshotRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    public IamLoginView login(LoginIamUserCommand command) {
        requireTenantId(command.tenantId(), "租户 ID 不能为空");
        String username = IamUser.normalizeUsername(command.username());
        String password = requireText(command.password(), "密码不能为空");
        IamUser user = userRepository.findActiveByTenantIdAndUsername(command.tenantId(), username)
                .orElseThrow(() -> new BusinessException("IAM_INVALID_CREDENTIALS", "用户名或密码错误"));
        validateUserStatus(user);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            recordFailedLogin(user);
            throw new BusinessException("IAM_INVALID_CREDENTIALS", "用户名或密码错误");
        }
        IamUser saved = recordSuccessfulLogin(user);
        CurrentUser currentUser = buildCurrentUser(saved);
        IamIssuedAccessToken issuedAccessToken = accessTokenIssuer.issue(currentUser);
        return new IamLoginView(issuedAccessToken.accessToken(), issuedAccessToken.expiresAt(), currentUser);
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
                        Set.copyOf(snapshot.permissionCodes())))
                .orElseGet(() -> new CurrentUser(
                        user.id(),
                        user.tenantId(),
                        user.username(),
                        roles,
                        user.authVersion(),
                        Set.of()));
    }

    private Set<String> platformRoles(IamUser user) {
        if (Long.valueOf(0L).equals(user.tenantId()) && "super_admin".equals(user.username())) {
            return Set.of("super_admin");
        }
        return Set.of();
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

    private void validateUserStatus(IamUser user) {
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
}
