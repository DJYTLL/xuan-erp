package com.xuan.erp.iam;

import com.xuan.erp.iam.application.command.CreateIamUserCommand;
import com.xuan.erp.iam.application.command.DisableIamUserCommand;
import com.xuan.erp.iam.application.command.RebuildAuthorizationSnapshotCommand;
import com.xuan.erp.iam.application.query.IamAuthorizationSnapshotView;
import com.xuan.erp.iam.application.query.IamUserDetailView;
import com.xuan.erp.iam.application.service.IamAuthorizationApplicationService;
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
import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserRecord;
import com.xuan.erp.iam.infrastructure.persistence.repository.JdbcIamUserRepository;
import com.xuan.erp.iam.interfaces.assembler.IamAuthorizationAssembler;
import com.xuan.erp.iam.interfaces.assembler.IamUserAssembler;
import com.xuan.erp.iam.interfaces.controller.IamAuthorizationController;
import com.xuan.erp.iam.interfaces.controller.IamMenuController;
import com.xuan.erp.iam.interfaces.controller.IamPermissionController;
import com.xuan.erp.iam.interfaces.controller.IamRoleController;
import com.xuan.erp.iam.interfaces.controller.IamTenantBootstrapController;
import com.xuan.erp.iam.interfaces.controller.IamUserController;
import com.xuan.erp.iam.interfaces.dto.CreateIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.IamAuthorizationSnapshotResponse;
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
                IamRoleApplicationService.class,
                IamPermissionApplicationService.class,
                IamMenuApplicationService.class,
                IamAuthorizationApplicationService.class,
                IamTenantBootstrapApplicationService.class,
                CreateIamUserCommand.class,
                DisableIamUserCommand.class,
                RebuildAuthorizationSnapshotCommand.class,
                IamUserDetailView.class,
                IamAuthorizationSnapshotView.class,
                IamUserRecord.class,
                JdbcIamUserRepository.class,
                IamUserController.class,
                IamRoleController.class,
                IamPermissionController.class,
                IamMenuController.class,
                IamAuthorizationController.class,
                IamTenantBootstrapController.class,
                IamUserAssembler.class,
                IamAuthorizationAssembler.class,
                CreateIamUserRequest.class,
                IamUserResponse.class,
                IamAuthorizationSnapshotResponse.class
        );

        assertEquals(29, ports.size());
    }
}
