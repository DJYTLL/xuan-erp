package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamUserCommand;
import com.xuan.erp.iam.application.command.DisableIamUserCommand;
import com.xuan.erp.iam.application.query.IamUserDetailView;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
/**
 * IAM 用户应用服务，负责租户内账号创建、查询和停用等用例编排。
 */
public class IamUserApplicationService {

    private final IamUserRepository userRepository;

    public IamUserApplicationService(IamUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public IamUserDetailView getUser(Long userId) {
        return toDetailView(requireUser(userId));
    }

    public List<IamUserDetailView> listUsers(Long tenantId) {
        requireTenantId(tenantId);
        return userRepository.findActiveUsers(tenantId).stream()
                .map(this::toDetailView)
                .toList();
    }

    public IamUserDetailView createUser(CreateIamUserCommand command) {
        requireTenantId(command.tenantId());
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
                requireText(command.passwordHash(), "密码哈希不能为空"),
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
                "system",
                now,
                "system",
                now,
                null,
                null,
                null));
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

    private IamUser requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("IAM_USER_NOT_FOUND", "IAM 用户不存在"));
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

    private void requireTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "租户 ID 不能为空");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return value.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String operator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }
}
