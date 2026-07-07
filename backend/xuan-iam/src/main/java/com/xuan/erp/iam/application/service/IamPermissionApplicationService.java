package com.xuan.erp.iam.application.service;

import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
/**
 * IAM 权限应用服务，负责全局权限清单查询用例。
 */
public class IamPermissionApplicationService {

    private final IamPermissionRepository permissionRepository;

    public IamPermissionApplicationService(IamPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<IamPermission> listPermissions() {
        return permissionRepository.findActivePermissions();
    }
}
