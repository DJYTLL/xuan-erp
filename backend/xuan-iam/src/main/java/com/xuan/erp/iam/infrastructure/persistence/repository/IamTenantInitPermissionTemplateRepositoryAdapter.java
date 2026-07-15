package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamTenantInitPermissionTemplate;
import com.xuan.erp.iam.domain.repository.IamTenantInitPermissionTemplateRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamTenantInitPermissionTemplatePersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamTenantInitPermissionTemplatePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * IAM 租户初始化权限模板仓储适配器，基于 MyBatis Mapper 实现模板持久化。
 */
@Repository
public class IamTenantInitPermissionTemplateRepositoryAdapter implements IamTenantInitPermissionTemplateRepository {

    private final IamTenantInitPermissionTemplatePersistenceMapper mapper;

    public IamTenantInitPermissionTemplateRepositoryAdapter(IamTenantInitPermissionTemplatePersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IamTenantInitPermissionTemplate> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id))
                .map(IamTenantInitPermissionTemplatePersistenceAssembler::toDomain);
    }

    @Override
    public Optional<IamTenantInitPermissionTemplate> findActiveByCode(String code) {
        return Optional.ofNullable(mapper.findActiveByCode(code))
                .map(IamTenantInitPermissionTemplatePersistenceAssembler::toDomain);
    }

    @Override
    public List<IamTenantInitPermissionTemplate> findActiveTemplates() {
        return mapper.findActiveTemplates().stream()
                .map(IamTenantInitPermissionTemplatePersistenceAssembler::toDomain)
                .toList();
    }

    @Override
    public IamTenantInitPermissionTemplate save(IamTenantInitPermissionTemplate template) {
        if (template.id() == null) {
            mapper.insert(IamTenantInitPermissionTemplatePersistenceAssembler.toRecord(template));
            return findActiveByCode(template.code()).orElseThrow();
        }
        mapper.update(IamTenantInitPermissionTemplatePersistenceAssembler.toRecord(template));
        return findById(template.id()).orElseThrow();
    }

    @Override
    public void clearDefaultTemplate(String operator) {
        mapper.clearDefaultTemplate(operator);
    }
}
