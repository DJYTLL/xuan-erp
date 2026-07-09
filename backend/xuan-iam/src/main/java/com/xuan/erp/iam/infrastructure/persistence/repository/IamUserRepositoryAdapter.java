package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamUserPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserRecord;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamUserPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * IAM 用户仓储适配器，负责协调领域仓储端口、MyBatis Mapper 与持久化装配器。
 */
@Repository
public class IamUserRepositoryAdapter implements IamUserRepository {

    private final IamUserPersistenceMapper mapper;

    public IamUserRepositoryAdapter(IamUserPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IamUser> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id))
                .map(IamUserPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username) {
        return Optional.ofNullable(mapper.findActiveByTenantIdAndUsername(tenantId, username))
                .map(IamUserPersistenceAssembler::toDomain);
    }

    @Override
    public List<IamUser> findActiveUsers(Long tenantId) {
        return mapper.findActiveUsers(tenantId).stream()
                .map(IamUserPersistenceAssembler::toDomain)
                .toList();
    }

    @Override
    public IamUser save(IamUser user) {
        IamUserRecord record = IamUserPersistenceAssembler.toRecord(user);
        if (user.id() == null) {
            mapper.insert(record);
            return findActiveByTenantIdAndUsername(user.tenantId(), user.username()).orElse(user);
        }
        mapper.update(record);
        return Optional.ofNullable(mapper.findByIdIncludingDeleted(user.id()))
                .map(IamUserPersistenceAssembler::toDomain)
                .orElse(user);
    }
}
