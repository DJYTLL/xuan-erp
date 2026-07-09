package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamAuthorizationSnapshotPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamAuthorizationSnapshotRecord;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamAuthorizationSnapshotPersistenceMapper;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * IAM 授权快照仓储适配器，负责通过 Mapper 读写授权快照持久化数据。
 */
@Repository
public class IamAuthorizationSnapshotRepositoryAdapter implements IamAuthorizationSnapshotRepository {

    private final IamAuthorizationSnapshotPersistenceMapper mapper;

    public IamAuthorizationSnapshotRepositoryAdapter(IamAuthorizationSnapshotPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) {
        return Optional.ofNullable(mapper.findByTenantIdAndUserId(tenantId, userId))
                .map(IamAuthorizationSnapshotPersistenceAssembler::toDomain);
    }

    @Override
    public IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot) {
        OffsetDateTime builtAt = snapshot.builtAt() == null ? OffsetDateTime.now() : snapshot.builtAt();
        IamAuthorizationSnapshot normalized = new IamAuthorizationSnapshot(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.userId(),
                snapshot.authVersion(),
                snapshot.roleIds(),
                snapshot.permissionCodes(),
                snapshot.menuCodes(),
                snapshot.columnSettings(),
                snapshot.snapshotHash(),
                snapshot.expiresAt(),
                builtAt,
                snapshot.createdBy(),
                snapshot.createdAt(),
                snapshot.updatedBy(),
                snapshot.updatedAt());
        IamAuthorizationSnapshotRecord record = IamAuthorizationSnapshotPersistenceAssembler.toRecord(normalized);
        if (normalized.id() == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
        return findByTenantIdAndUserId(normalized.tenantId(), normalized.userId()).orElse(normalized);
    }
}
