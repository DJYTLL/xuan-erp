package com.xuan.erp.iam;

import com.xuan.erp.iam.application.command.CreateIamUserCommand;
import com.xuan.erp.iam.application.command.DisableIamUserCommand;
import com.xuan.erp.iam.application.command.LoginIamUserCommand;
import com.xuan.erp.iam.application.command.RebuildAuthorizationSnapshotCommand;
import com.xuan.erp.iam.application.port.IamAccessTokenIssuer;
import com.xuan.erp.iam.application.port.IamIssuedAccessToken;
import com.xuan.erp.iam.application.query.IamAuthorizationSnapshotView;
import com.xuan.erp.iam.application.query.IamCurrentMenuNodeView;
import com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView;
import com.xuan.erp.iam.application.query.IamLoginView;
import com.xuan.erp.iam.application.query.IamUserDetailView;
import com.xuan.erp.iam.application.service.IamAuthenticationApplicationService;
import com.xuan.erp.iam.application.service.IamAuthorizationApplicationService;
import com.xuan.erp.iam.application.service.IamCurrentAuthorizationApplicationService;
import com.xuan.erp.iam.application.service.IamMenuApplicationService;
import com.xuan.erp.iam.application.service.IamPermissionApplicationService;
import com.xuan.erp.iam.application.service.IamRoleApplicationService;
import com.xuan.erp.iam.application.service.IamTenantBootstrapApplicationService;
import com.xuan.erp.iam.application.service.IamUserApplicationService;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.model.IamRefreshToken;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamRoleColumnSetting;
import com.xuan.erp.iam.domain.model.IamRolePermission;
import com.xuan.erp.iam.domain.model.IamTenantBootstrapTask;
import com.xuan.erp.iam.domain.model.IamTenantColumnSetting;
import com.xuan.erp.iam.domain.model.IamTenantMenu;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.model.IamUserRole;
import com.xuan.erp.iam.domain.model.IamUserTableSetting;
import com.xuan.erp.iam.domain.model.type.IamBootstrapTaskStatus;
import com.xuan.erp.iam.domain.model.type.IamMembershipStatus;
import com.xuan.erp.iam.domain.model.type.IamUserType;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamAuthorizationSnapshotPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamMenuPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamPermissionPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamUserPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamAuthorizationSnapshotRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamMenuRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamPermissionRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamRoleRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamTenantBootstrapResultRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserRecord;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamAuthorizationSnapshotPersistenceMapper;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamMenuPersistenceMapper;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamPermissionPersistenceMapper;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamRolePersistenceMapper;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamTenantBootstrapMapper;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamUserPersistenceMapper;
import com.xuan.erp.iam.infrastructure.persistence.repository.IamAuthorizationSnapshotRepositoryAdapter;
import com.xuan.erp.iam.infrastructure.persistence.repository.IamMenuRepositoryAdapter;
import com.xuan.erp.iam.infrastructure.persistence.repository.IamPermissionRepositoryAdapter;
import com.xuan.erp.iam.infrastructure.persistence.repository.IamRoleRepositoryAdapter;
import com.xuan.erp.iam.infrastructure.persistence.repository.IamTenantBootstrapGatewayAdapter;
import com.xuan.erp.iam.infrastructure.persistence.repository.IamUserRepositoryAdapter;
import com.xuan.erp.iam.infrastructure.security.IamBearerTokenAuthenticationFilter;
import com.xuan.erp.iam.interfaces.assembler.IamAuthorizationAssembler;
import com.xuan.erp.iam.interfaces.assembler.IamAuthenticationAssembler;
import com.xuan.erp.iam.interfaces.assembler.IamCurrentAuthorizationAssembler;
import com.xuan.erp.iam.interfaces.assembler.IamUserAssembler;
import com.xuan.erp.iam.interfaces.controller.IamAuthorizationController;
import com.xuan.erp.iam.interfaces.controller.IamAuthenticationController;
import com.xuan.erp.iam.interfaces.controller.IamMenuController;
import com.xuan.erp.iam.interfaces.controller.IamPermissionController;
import com.xuan.erp.iam.interfaces.controller.IamRoleController;
import com.xuan.erp.iam.interfaces.controller.IamTenantBootstrapController;
import com.xuan.erp.iam.interfaces.controller.IamUserController;
import com.xuan.erp.iam.interfaces.dto.CreateIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.IamAuthorizationSnapshotResponse;
import com.xuan.erp.iam.interfaces.dto.IamCurrentMenuNodeResponse;
import com.xuan.erp.iam.interfaces.dto.IamCurrentPermissionSnapshotResponse;
import com.xuan.erp.iam.interfaces.dto.IamCurrentUserResponse;
import com.xuan.erp.iam.interfaces.dto.IamLoginRequest;
import com.xuan.erp.iam.interfaces.dto.IamLoginResponse;
import com.xuan.erp.iam.interfaces.dto.IamUserResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IamDddSkeletonTest {

    @Test
    void exposesDomainModelsForCoreIamTables() {
        List<Class<?>> models = List.of(
                IamUser.class,
                IamRole.class,
                IamPermission.class,
                IamMenu.class,
                IamUserRole.class,
                IamRolePermission.class,
                IamAuthorizationSnapshot.class,
                IamTenantMenu.class,
                IamTenantColumnSetting.class,
                IamRoleColumnSetting.class,
                IamUserTableSetting.class,
                IamRefreshToken.class,
                IamTenantBootstrapTask.class
        );

        assertEquals(13, models.size());
    }

    @Test
    void exposesIamSchemaStatusTypes() {
        assertEquals(IamUserType.TENANT_USER, IamUserType.fromCode("tenant_user"));
        assertEquals(IamMembershipStatus.SUSPENDED, IamMembershipStatus.fromCode("SUSPENDED"));
        assertEquals(IamBootstrapTaskStatus.SUCCEEDED, IamBootstrapTaskStatus.fromCode("succeeded"));
    }

    @Test
    void exposesFourLayerIamPorts() {
        List<Class<?>> ports = List.of(
                IamUserRepository.class,
                IamRoleRepository.class,
                IamPermissionRepository.class,
                IamMenuRepository.class,
                IamAuthorizationSnapshotRepository.class,
                IamUserApplicationService.class,
                IamAuthenticationApplicationService.class,
                IamRoleApplicationService.class,
                IamPermissionApplicationService.class,
                IamMenuApplicationService.class,
                IamCurrentAuthorizationApplicationService.class,
                IamAuthorizationApplicationService.class,
                IamTenantBootstrapApplicationService.class,
                CreateIamUserCommand.class,
                DisableIamUserCommand.class,
                LoginIamUserCommand.class,
                RebuildAuthorizationSnapshotCommand.class,
                IamAccessTokenIssuer.class,
                IamIssuedAccessToken.class,
                IamUserDetailView.class,
                IamLoginView.class,
                IamCurrentMenuNodeView.class,
                IamCurrentPermissionSnapshotView.class,
                IamAuthorizationSnapshotView.class,
                IamUserRecord.class,
                IamRoleRecord.class,
                IamPermissionRecord.class,
                IamMenuRecord.class,
                IamAuthorizationSnapshotRecord.class,
                IamTenantBootstrapResultRecord.class,
                IamUserPersistenceAssembler.class,
                IamPermissionPersistenceAssembler.class,
                IamMenuPersistenceAssembler.class,
                IamAuthorizationSnapshotPersistenceAssembler.class,
                IamUserPersistenceMapper.class,
                IamRolePersistenceMapper.class,
                IamPermissionPersistenceMapper.class,
                IamMenuPersistenceMapper.class,
                IamAuthorizationSnapshotPersistenceMapper.class,
                IamTenantBootstrapMapper.class,
                IamUserRepositoryAdapter.class,
                IamRoleRepositoryAdapter.class,
                IamPermissionRepositoryAdapter.class,
                IamMenuRepositoryAdapter.class,
                IamAuthorizationSnapshotRepositoryAdapter.class,
                IamTenantBootstrapGatewayAdapter.class,
                IamUserController.class,
                IamAuthenticationController.class,
                IamRoleController.class,
                IamPermissionController.class,
                IamMenuController.class,
                IamAuthorizationController.class,
                IamTenantBootstrapController.class,
                IamUserAssembler.class,
                IamAuthenticationAssembler.class,
                IamCurrentAuthorizationAssembler.class,
                IamAuthorizationAssembler.class,
                CreateIamUserRequest.class,
                IamLoginRequest.class,
                IamLoginResponse.class,
                IamCurrentMenuNodeResponse.class,
                IamCurrentPermissionSnapshotResponse.class,
                IamCurrentUserResponse.class,
                IamUserResponse.class,
                IamAuthorizationSnapshotResponse.class,
                IamBearerTokenAuthenticationFilter.class
        );

        assertEquals(66, ports.size());
    }
}
