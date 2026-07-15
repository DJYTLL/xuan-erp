package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamPermissionPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamPermissionPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * IAM 权限仓储适配器，负责通过 Mapper 读取全局权限定义。
 */
@Repository
public class IamPermissionRepositoryAdapter implements IamPermissionRepository {

    private final IamPermissionPersistenceMapper mapper;

    public IamPermissionRepositoryAdapter(IamPermissionPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IamPermission> findByCode(String code) {
        return Optional.ofNullable(mapper.findByCode(code))
                .map(IamPermissionPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<IamPermission> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id))
                .map(IamPermissionPersistenceAssembler::toDomain);
    }

    @Override
    public List<IamPermission> findActivePermissions() {
        return mapper.findActivePermissions().stream()
                .map(IamPermissionPersistenceAssembler::toDomain)
                .toList();
    }

    @Override
    public IamPermission save(IamPermission permission) {
        if (permission.id() == null) {
            mapper.insert(IamPermissionPersistenceAssembler.toRecord(permission));
            return findByCode(permission.code()).orElseThrow();
        }
        mapper.update(IamPermissionPersistenceAssembler.toRecord(permission));
        return findById(permission.id()).orElseThrow();
    }
}
