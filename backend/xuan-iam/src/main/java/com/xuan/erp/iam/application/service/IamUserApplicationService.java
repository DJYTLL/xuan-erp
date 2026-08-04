package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamUserCommand;
import com.xuan.erp.iam.application.command.DisableIamUserCommand;
import com.xuan.erp.iam.application.command.ResetIamUserPasswordCommand;
import com.xuan.erp.iam.application.command.SetIamUserRolesCommand;
import com.xuan.erp.iam.application.command.UpdateIamUserCommand;
import com.xuan.erp.iam.application.query.IamUserRoleGrantView;
import com.xuan.erp.iam.application.query.IamUserDetailView;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
/**
 * IAM 用户应用服务，负责租户内账号创建、查询和停用等用例编排。
 */
public class IamUserApplicationService {

    private static final String TENANT_ADMIN_USERNAME = "admin";

    private final IamUserRepository userRepository;
    private final IamRoleRepository roleRepository;
    private final IamRolePermissionRepository rolePermissionRepository;
    private final IamAuthorizationSnapshotRepository snapshotRepository;
    private final PasswordEncoder passwordEncoder;

    public IamUserApplicationService(
            IamUserRepository userRepository,
            IamRoleRepository roleRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamAuthorizationSnapshotRepository snapshotRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.snapshotRepository = snapshotRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public IamUserDetailView getUser(Long userId) {
        return toDetailView(requireUser(userId));
    }

    public List<IamUserDetailView> listUsers(Long tenantId) {
        requireReadableTenantId(tenantId);
        return userRepository.findActiveUsers(tenantId).stream()
                .map(this::toDetailView)
                .toList();
    }

    public IamUserDetailView createUser(CreateIamUserCommand command) {
        requireBusinessTenantId(command.tenantId());
        String username = IamUser.normalizeUsername(command.username());
        userRepository.findActiveByTenantIdAndUsername(command.tenantId(), username)
                .ifPresent(existing -> {
                    throw new BusinessException("IAM_USERNAME_EXISTS", "同一租户下用户名已存在");
                });
        OffsetDateTime now = OffsetDateTime.now();
        IamUser saved = userRepository.save(new IamUser(
                null,
                command.tenantId(),
                username,
                passwordEncoder.encode(requirePassword(command.initialPassword())),
                trim(command.displayName()),
                trim(command.email()),
                trim(command.phone()),
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
                0L,
                trim(command.remark()),
                operator(command.operator()),
                now,
                operator(command.operator()),
                now,
                null,
                null,
                null));
        return toDetailView(saved);
    }

    public IamUserDetailView updateUser(Long userId, UpdateIamUserCommand command) {
        IamUser user = requireUserInTenant(command.tenantId(), userId, false);
        OffsetDateTime now = OffsetDateTime.now();
        String operator = operator(command.operator());
        IamUser saved = userRepository.save(new IamUser(
                user.id(),
                user.tenantId(),
                user.username(),
                user.passwordHash(),
                trim(command.displayName()),
                trim(command.email()),
                trim(command.phone()),
                user.avatarUrl(),
                command.enabled() == null ? user.enabled() : command.enabled(),
                user.accountNonExpired(),
                user.accountNonLocked(),
                user.credentialsNonExpired(),
                user.lastLoginAt(),
                user.passwordChangedAt(),
                user.failedLoginCount(),
                user.lastFailedLoginAt(),
                user.lockedUntil(),
                user.mfaEnabled(),
                user.authVersion() + 1,
                trim(command.remark()),
                user.createdBy(),
                user.createdAt(),
                operator,
                now,
                user.deletedBy(),
                user.deleteReason(),
                user.deletedAt()));
        return toDetailView(saved);
    }

    public IamUserDetailView resetPassword(Long userId, ResetIamUserPasswordCommand command) {
        IamUser user = requireUserInTenant(command.tenantId(), userId, false);
        return resetPassword(user, command);
    }

    public IamUserDetailView resetTenantAdminPassword(Long tenantId, ResetIamUserPasswordCommand command) {
        requireBusinessTenantId(tenantId);
        if (!tenantId.equals(command.tenantId())) {
            throw new BusinessException("IAM_USER_TENANT_MISMATCH", "用户不属于指定租户");
        }
        IamUser adminUser = userRepository.findActiveByTenantIdAndUsername(tenantId, TENANT_ADMIN_USERNAME)
                .orElseThrow(() -> new BusinessException("IAM_TENANT_ADMIN_NOT_FOUND", "租户 admin 账号不存在"));
        return resetPassword(adminUser, command);
    }

    private IamUserDetailView resetPassword(IamUser user, ResetIamUserPasswordCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        String operator = operator(command.operator());
        IamUser saved = userRepository.save(new IamUser(
                user.id(),
                user.tenantId(),
                user.username(),
                passwordEncoder.encode(requirePassword(command.newPassword())),
                user.displayName(),
                user.email(),
                user.phone(),
                user.avatarUrl(),
                user.enabled(),
                user.accountNonExpired(),
                user.accountNonLocked(),
                true,
                user.lastLoginAt(),
                now,
                0,
                null,
                null,
                user.mfaEnabled(),
                user.authVersion() + 1,
                user.remark(),
                user.createdBy(),
                user.createdAt(),
                operator,
                now,
                user.deletedBy(),
                user.deleteReason(),
                user.deletedAt()));
        return toDetailView(saved);
    }

    public IamUserDetailView disableUser(Long userId, DisableIamUserCommand command) {
        IamUser user = requireUser(userId);
        String reason = requireText(command.reason(), "停用原因不能为空");
        OffsetDateTime now = OffsetDateTime.now();
        IamUser saved = userRepository.save(new IamUser(
                user.id(),
                user.tenantId(),
                user.username(),
                user.passwordHash(),
                user.displayName(),
                user.email(),
                user.phone(),
                user.avatarUrl(),
                false,
                user.accountNonExpired(),
                user.accountNonLocked(),
                user.credentialsNonExpired(),
                user.lastLoginAt(),
                user.passwordChangedAt(),
                user.failedLoginCount(),
                user.lastFailedLoginAt(),
                user.lockedUntil(),
                user.mfaEnabled(),
                user.authVersion() + 1,
                user.remark(),
                user.createdBy(),
                user.createdAt(),
                operator(command.operator()),
                now,
                operator(command.operator()),
                reason,
                now));
        return toDetailView(saved);
    }

    public IamUserRoleGrantView getUserRoles(Long tenantId, Long userId) {
        IamUser user = requireUserInTenant(tenantId, userId, true);
        return new IamUserRoleGrantView(
                user.tenantId(),
                user.id(),
                rolePermissionRepository.findRoleIdsByUserId(user.tenantId(), user.id()));
    }

    @Transactional
    public IamUserRoleGrantView replaceUserRoles(SetIamUserRolesCommand command) {
        requireBusinessTenantId(command.tenantId());
        IamUser user = requireUserInTenant(command.tenantId(), command.userId(), false);
        List<Long> roleIds = normalizeRoleIds(command.roleIds()).stream()
                .map(roleId -> requireRoleInTenant(user.tenantId(), roleId).id())
                .toList();
        String operator = operator(command.operator());
        rolePermissionRepository.replaceUserRoles(user.tenantId(), user.id(), roleIds, operator);
        refreshUserAuthorizationSnapshot(user, roleIds, operator);
        return new IamUserRoleGrantView(user.tenantId(), user.id(), roleIds);
    }

    private IamUser requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("IAM_USER_NOT_FOUND", "IAM 用户不存在"));
    }

    private IamUser requireUserInTenant(Long tenantId, Long userId, boolean allowPlatformTenant) {
        if (allowPlatformTenant) {
            requireReadableTenantId(tenantId);
        } else {
            requireBusinessTenantId(tenantId);
        }
        IamUser user = requireUser(userId);
        if (!user.tenantId().equals(tenantId)) {
            throw new BusinessException("IAM_USER_TENANT_MISMATCH", "用户不属于指定租户");
        }
        return user;
    }

    private IamRole requireRoleInTenant(Long tenantId, Long roleId) {
        IamRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("IAM_ROLE_NOT_FOUND", "角色不存在"));
        if (!role.tenantId().equals(tenantId)) {
            throw new BusinessException("IAM_ROLE_TENANT_MISMATCH", "角色不属于指定租户");
        }
        if (!role.enabled()) {
            throw new BusinessException("IAM_ROLE_DISABLED", "角色已停用");
        }
        return role;
    }

    private void refreshUserAuthorizationSnapshot(IamUser user, List<Long> roleIds, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        IamAuthorizationSnapshot existing = snapshotRepository
                .findByTenantIdAndUserId(user.tenantId(), user.id())
                .orElse(null);
        long nextAuthVersion = Math.max(user.authVersion(), existing == null ? user.authVersion() : existing.authVersion()) + 1;
        List<String> permissionCodes = rolePermissionRepository.findPermissionCodesByUserId(user.tenantId(), user.id());
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
                user.failedLoginCount(),
                user.lastFailedLoginAt(),
                user.lockedUntil(),
                user.mfaEnabled(),
                nextAuthVersion,
                user.remark(),
                user.createdBy(),
                user.createdAt(),
                operator,
                now,
                user.deletedBy(),
                user.deleteReason(),
                user.deletedAt()));
        snapshotRepository.save(new IamAuthorizationSnapshot(
                existing == null ? null : existing.id(),
                user.tenantId(),
                user.id(),
                nextAuthVersion,
                roleIds,
                permissionCodes,
                List.of(),
                existing == null ? Map.of() : existing.columnSettings(),
                IamAuthorizationSnapshotHash.from(roleIds, permissionCodes),
                existing == null ? null : existing.expiresAt(),
                now,
                existing == null ? operator : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                operator,
                now));
    }

    private IamUserDetailView toDetailView(IamUser user) {
        return new IamUserDetailView(
                user.id(),
                user.tenantId(),
                user.username(),
                user.displayName(),
                user.email(),
                user.phone(),
                user.enabled(),
                user.accountNonLocked(),
                user.authVersion(),
                user.remark(),
                user.createdAt(),
                user.updatedAt());
    }

    private void requireReadableTenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "租户 ID 不能为空");
        }
    }

    private void requireBusinessTenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "租户 ID 不能为空");
        }
        if (tenantId == 0) {
            throw new BusinessException("IAM_PLATFORM_USER_ROLE_READ_ONLY", "平台级用户角色当前仅支持查看");
        }
    }

    private List<Long> normalizeRoleIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && value > 0)
                .collect(LinkedHashSet<Long>::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .stream()
                .sorted()
                .toList();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return value.trim();
    }

    private String requirePassword(String value) {
        String password = requireText(value, "密码不能为空");
        if (password.length() < 6) {
            throw new BusinessException("IAM_PASSWORD_TOO_WEAK", "密码长度不能少于 6 位");
        }
        return password;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String operator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

}
