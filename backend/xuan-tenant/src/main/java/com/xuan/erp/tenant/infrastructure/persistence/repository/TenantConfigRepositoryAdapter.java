package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.TenantConfig;
import com.xuan.erp.tenant.domain.model.type.ConfigValueType;
import com.xuan.erp.tenant.domain.repository.TenantConfigRepository;
import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantConfigRecord;
import com.xuan.erp.tenant.infrastructure.persistence.mapper.TenantConfigPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TenantConfigRepositoryAdapter implements TenantConfigRepository {

    private final TenantConfigPersistenceMapper mapper;

    public TenantConfigRepositoryAdapter(TenantConfigPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<TenantConfig> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id)).map(this::toDomain);
    }

    @Override
    public Optional<TenantConfig> findActiveByTenantIdAndKey(Long tenantId, String configKey) {
        return Optional.ofNullable(mapper.findActiveByTenantIdAndKey(tenantId, configKey)).map(this::toDomain);
    }

    @Override
    public List<TenantConfig> findByTenantId(Long tenantId, long offset, long limit) {
        return mapper.findByTenantId(tenantId, offset, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByTenantId(Long tenantId) {
        return mapper.countByTenantId(tenantId);
    }

    @Override
    public List<TenantConfig> findPublicByTenantId(Long tenantId) {
        return mapper.findPublicByTenantId(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TenantConfig save(TenantConfig tenantConfig) {
        TenantConfigRecord record = toRecord(tenantConfig);
        if (tenantConfig.id() == null) {
            mapper.insert(record);
            return Optional.ofNullable(mapper.findActiveByTenantIdAndKey(tenantConfig.tenantId(), tenantConfig.configKey()))
                    .map(this::toDomain)
                    .orElse(tenantConfig);
        }
        mapper.update(record);
        return Optional.ofNullable(mapper.findByIdIncludingDeleted(tenantConfig.id()))
                .map(this::toDomain)
                .orElse(tenantConfig);
    }

    private TenantConfig toDomain(TenantConfigRecord record) {
        return new TenantConfig(
                record.id(),
                record.tenantId(),
                record.configKey(),
                record.configValue(),
                ConfigValueType.fromCode(record.valueType()),
                record.description(),
                record.publicConfig(),
                record.sensitive(),
                record.encrypted(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt(),
                record.deletedBy(),
                record.deleteReason(),
                record.deletedAt()
        );
    }

    private TenantConfigRecord toRecord(TenantConfig tenantConfig) {
        return new TenantConfigRecord(
                tenantConfig.id(),
                tenantConfig.tenantId(),
                tenantConfig.configKey(),
                tenantConfig.configValue(),
                tenantConfig.valueType().code(),
                tenantConfig.description(),
                tenantConfig.publicConfig(),
                tenantConfig.sensitive(),
                tenantConfig.encrypted(),
                tenantConfig.createdBy(),
                tenantConfig.createdAt(),
                tenantConfig.updatedBy(),
                tenantConfig.updatedAt(),
                tenantConfig.deletedBy(),
                tenantConfig.deleteReason(),
                tenantConfig.deletedAt()
        );
    }
}
