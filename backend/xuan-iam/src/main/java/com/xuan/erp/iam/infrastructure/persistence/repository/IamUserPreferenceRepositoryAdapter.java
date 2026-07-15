package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamUserPreference;
import com.xuan.erp.iam.domain.repository.IamUserPreferenceRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamUserPreferencePersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamUserPreferencePersistenceMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * IAM 用户通用偏好仓储适配器，负责通过 MyBatis 访问用户偏好表。
 */
@Repository
public class IamUserPreferenceRepositoryAdapter implements IamUserPreferenceRepository {

    private final IamUserPreferencePersistenceMapper mapper;

    public IamUserPreferenceRepositoryAdapter(IamUserPreferencePersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IamUserPreference> findByTenantIdAndUserIdAndPreferenceKey(
            Long tenantId,
            Long userId,
            String preferenceKey) {
        return Optional.ofNullable(mapper.findByTenantIdAndUserIdAndPreferenceKey(tenantId, userId, preferenceKey))
                .map(IamUserPreferencePersistenceAssembler::toDomain);
    }

    @Override
    public IamUserPreference save(IamUserPreference preference) {
        mapper.upsert(IamUserPreferencePersistenceAssembler.toRecord(preference));
        return findByTenantIdAndUserIdAndPreferenceKey(
                preference.tenantId(),
                preference.userId(),
                preference.preferenceKey()).orElseThrow();
    }
}
