package com.xuan.erp.tenant.application.service;

import com.xuan.erp.common.api.PageResult;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.CreateTenantConfigCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantConfigCommand;
import com.xuan.erp.tenant.application.query.TenantConfigDetailView;
import com.xuan.erp.tenant.domain.model.TenantConfig;
import com.xuan.erp.tenant.domain.model.type.ConfigValueType;
import com.xuan.erp.tenant.domain.repository.TenantConfigRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 租户配置应用服务，负责租户配置的专属领域规则、分页查询和公开配置边界。
 */
@Service
public class TenantConfigApplicationService {

    private final TenantConfigRepository repository;

    public TenantConfigApplicationService(TenantConfigRepository repository) {
        this.repository = repository;
    }

    public PageResult<TenantConfigDetailView> listConfigs(Long tenantId, long pageNum, long pageSize) {
        long normalizedPageNum = Math.max(pageNum, 1);
        long normalizedPageSize = Math.max(pageSize, 1);
        long offset = (normalizedPageNum - 1) * normalizedPageSize;
        List<TenantConfigDetailView> records = repository.findByTenantId(requiredTenantId(tenantId), offset, normalizedPageSize).stream()
                .map(this::toDetailView)
                .toList();
        return new PageResult<>(records, repository.countByTenantId(tenantId), normalizedPageNum, normalizedPageSize);
    }

    public List<TenantConfigDetailView> listPublicConfigs(Long tenantId) {
        return repository.findPublicByTenantId(requiredTenantId(tenantId)).stream()
                .map(this::toDetailView)
                .toList();
    }

    public TenantConfigDetailView getConfig(Long id) {
        return toDetailView(requireConfig(id));
    }

    public TenantConfigDetailView createConfig(CreateTenantConfigCommand command) {
        repository.findActiveByTenantIdAndKey(requiredTenantId(command.tenantId()), requireText(command.configKey(), "配置键不能为空"))
                .ifPresent(existing -> {
                    throw new BusinessException("TENANT_CONFIG_KEY_EXISTS", "配置键已存在");
                });
        OffsetDateTime now = OffsetDateTime.now();
        TenantConfig saved = repository.save(validateBoundary(new TenantConfig(
                null,
                command.tenantId(),
                command.configKey().trim(),
                requireText(command.configValue(), "配置值不能为空"),
                parseValueType(command.valueType()),
                command.description(),
                command.publicConfig(),
                command.sensitive(),
                command.encrypted(),
                "system",
                now,
                "system",
                now,
                null,
                null,
                null
        )));
        return toDetailView(saved);
    }

    public TenantConfigDetailView updateConfig(Long id, UpdateTenantConfigCommand command) {
        TenantConfig existing = requireConfig(id);
        return updateConfig(existing, command);
    }

    public TenantConfigDetailView updateConfig(Long tenantId, String configKey, UpdateTenantConfigCommand command) {
        TenantConfig existing = repository.findActiveByTenantIdAndKey(requiredTenantId(tenantId), requireText(configKey, "配置键不能为空"))
                .orElseThrow(() -> new BusinessException("TENANT_CONFIG_NOT_FOUND", "租户配置不存在"));
        return updateConfig(existing, command);
    }

    private TenantConfigDetailView updateConfig(TenantConfig existing, UpdateTenantConfigCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        TenantConfig saved = repository.save(validateBoundary(new TenantConfig(
                existing.id(),
                existing.tenantId(),
                existing.configKey(),
                requireText(command.configValue(), "配置值不能为空"),
                parseValueType(command.valueType()),
                command.description(),
                command.publicConfig(),
                command.sensitive(),
                command.encrypted(),
                existing.createdBy(),
                existing.createdAt(),
                "system",
                now,
                existing.deletedBy(),
                existing.deleteReason(),
                existing.deletedAt()
        )));
        return toDetailView(saved);
    }

    public void deleteConfig(Long id, String reason, String operator) {
        TenantConfig existing = requireConfig(id);
        repository.save(new TenantConfig(
                existing.id(),
                existing.tenantId(),
                existing.configKey(),
                existing.configValue(),
                existing.valueType(),
                existing.description(),
                existing.publicConfig(),
                existing.sensitive(),
                existing.encrypted(),
                existing.createdBy(),
                existing.createdAt(),
                defaultOperator(operator),
                OffsetDateTime.now(),
                defaultOperator(operator),
                requireText(reason, "删除原因不能为空"),
                OffsetDateTime.now()
        ));
    }

    private TenantConfig requireConfig(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("TENANT_CONFIG_NOT_FOUND", "租户配置不存在"));
    }

    private TenantConfigDetailView toDetailView(TenantConfig config) {
        return new TenantConfigDetailView(
                config.id(),
                config.tenantId(),
                config.configKey(),
                config.displayValue(),
                config.valueType(),
                config.description(),
                config.publicConfig(),
                config.sensitive(),
                config.encrypted()
        );
    }

    private Long requiredTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("TENANT_CONFIG_INVALID_ARGUMENT", "租户 ID 不能为空");
        }
        return tenantId;
    }

    private ConfigValueType parseValueType(String valueType) {
        try {
            return ConfigValueType.fromCode(requireText(valueType, "值类型不能为空"));
        } catch (IllegalArgumentException error) {
            throw new BusinessException("TENANT_CONFIG_INVALID_ARGUMENT", "不支持的配置值类型");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("TENANT_CONFIG_INVALID_ARGUMENT", message);
        }
        return value.trim();
    }

    private String defaultOperator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    private TenantConfig validateBoundary(TenantConfig config) {
        try {
            return config.validateBoundary();
        } catch (IllegalStateException error) {
            throw new BusinessException("TENANT_CONFIG_BOUNDARY_INVALID", "公开配置不能标记为敏感配置");
        }
    }
}
