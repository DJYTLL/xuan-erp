package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamTenantInitPermissionTemplate;
import java.util.List;
import java.util.Optional;

/**
 * IAM 租户初始化权限模板仓储端口，隔离模板持久化读写细节。
 */
public interface IamTenantInitPermissionTemplateRepository {

    Optional<IamTenantInitPermissionTemplate> findById(Long id);

    Optional<IamTenantInitPermissionTemplate> findActiveByCode(String code);

    List<IamTenantInitPermissionTemplate> findActiveTemplates();

    IamTenantInitPermissionTemplate save(IamTenantInitPermissionTemplate template);

    void clearDefaultTemplate(String operator);
}
