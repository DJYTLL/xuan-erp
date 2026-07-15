package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamPermissionCommand;
import com.xuan.erp.iam.application.command.UpdateIamPermissionCommand;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
/**
 * IAM 权限应用服务，负责全局权限清单查询用例。
 */
public class IamPermissionApplicationService {

    private final IamPermissionRepository permissionRepository;

    public IamPermissionApplicationService(IamPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<IamPermission> listPermissions() {
        return permissionRepository.findActivePermissions();
    }

    public IamPermission createPermission(CreateIamPermissionCommand command) {
        String code = requireText(command.code(), "权限编码不能为空");
        if (permissionRepository.findByCode(code).isPresent()) {
            throw new BusinessException("IAM_PERMISSION_CODE_EXISTS", "权限编码已存在");
        }
        OffsetDateTime now = OffsetDateTime.now();
        return permissionRepository.save(new IamPermission(
                null,
                code,
                requireText(command.name(), "权限名称不能为空"),
                requireText(command.serviceName(), "来源服务不能为空"),
                trimToNull(command.menuCode()),
                trimToNull(command.description()),
                true,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null));
    }

    public IamPermission updatePermission(Long id, UpdateIamPermissionCommand command) {
        IamPermission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("IAM_PERMISSION_NOT_FOUND", "权限不存在"));
        OffsetDateTime now = OffsetDateTime.now();
        return permissionRepository.save(new IamPermission(
                existing.id(),
                existing.code(),
                requireText(command.name(), "权限名称不能为空"),
                requireText(command.serviceName(), "来源服务不能为空"),
                trimToNull(command.menuCode()),
                trimToNull(command.description()),
                command.enabled() == null ? existing.enabled() : command.enabled(),
                existing.createdBy(),
                existing.createdAt(),
                "system",
                now,
                null,
                null,
                null));
    }

    public IamPermission setPermissionEnabled(Long id, boolean enabled) {
        IamPermission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("IAM_PERMISSION_NOT_FOUND", "权限不存在"));
        OffsetDateTime now = OffsetDateTime.now();
        return permissionRepository.save(new IamPermission(
                existing.id(),
                existing.code(),
                existing.name(),
                existing.serviceName(),
                existing.menuCode(),
                existing.description(),
                enabled,
                existing.createdBy(),
                existing.createdAt(),
                "system",
                now,
                null,
                null,
                null));
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
