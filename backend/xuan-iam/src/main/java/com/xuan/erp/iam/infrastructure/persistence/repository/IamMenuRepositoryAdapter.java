package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamMenuPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamMenuPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * IAM 菜单仓储适配器，负责通过 Mapper 读取全局菜单目录。
 */
@Repository
public class IamMenuRepositoryAdapter implements IamMenuRepository {

    private final IamMenuPersistenceMapper mapper;

    public IamMenuRepositoryAdapter(IamMenuPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IamMenu> findByCode(String code) {
        return Optional.ofNullable(mapper.findByCode(code))
                .map(IamMenuPersistenceAssembler::toDomain);
    }

    @Override
    public List<IamMenu> findActiveMenus() {
        return mapper.findActiveMenus().stream()
                .map(IamMenuPersistenceAssembler::toDomain)
                .toList();
    }
}
