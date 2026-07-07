package com.xuan.erp.iam.application.service;

import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
/**
 * IAM 角色应用服务，负责租户内角色查询用例。
 */
public class IamRoleApplicationService {

    private final IamRoleRepository roleRepository;

    public IamRoleApplicationService(IamRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<IamRole> listRoles(Long tenantId) {
        return roleRepository.findActiveRoles(tenantId);
    }
}
