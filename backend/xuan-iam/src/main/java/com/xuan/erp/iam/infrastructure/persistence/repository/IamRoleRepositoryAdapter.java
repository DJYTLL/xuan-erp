package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamRolePersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamRolePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * IAM 角色仓储适配器，负责通过 Mapper 读取租户内角色数据。
 */
@Repository
public class IamRoleRepositoryAdapter implements IamRoleRepository {

    private final IamRolePersistenceMapper mapper;

    public IamRoleRepositoryAdapter(IamRolePersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IamRole> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id))
                .map(IamRolePersistenceAssembler::toDomain);
    }

    @Override
    public Optional<IamRole> findActiveByTenantIdAndCode(Long tenantId, String code) {
        return Optional.ofNullable(mapper.findActiveByTenantIdAndCode(tenantId, code))
                .map(IamRolePersistenceAssembler::toDomain);
    }

    @Override
    public List<IamRole> findActiveRoles(Long tenantId) {
        return mapper.findActiveRoles(tenantId).stream()
                .map(IamRolePersistenceAssembler::toDomain)
                .toList();
    }

    @Override
    public IamRole save(IamRole role) {
        if (role.id() == null) {
            mapper.insert(IamRolePersistenceAssembler.toRecord(role));
            return findActiveByTenantIdAndCode(role.tenantId(), role.code()).orElseThrow();
        }
        mapper.update(IamRolePersistenceAssembler.toRecord(role));
        return findById(role.id()).orElseThrow();
    }
}
