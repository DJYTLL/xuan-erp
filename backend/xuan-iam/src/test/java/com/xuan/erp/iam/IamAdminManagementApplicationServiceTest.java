package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamColumnPermissionTemplateCommand;
import com.xuan.erp.iam.application.command.CreateIamMenuCommand;
import com.xuan.erp.iam.application.command.CreateIamPermissionCommand;
import com.xuan.erp.iam.application.command.CreateIamRoleCommand;
import com.xuan.erp.iam.application.command.IamColumnPermissionTemplateItemCommand;
import com.xuan.erp.iam.application.command.IamRoleColumnPermissionRuleCommand;
import com.xuan.erp.iam.application.command.SetIamColumnPermissionTemplateItemsCommand;
import com.xuan.erp.iam.application.command.SetIamRoleColumnPermissionsCommand;
import com.xuan.erp.iam.application.command.SetIamRolePermissionsCommand;
import com.xuan.erp.iam.application.command.SetIamRoleColumnPermissionTemplateCommand;
import com.xuan.erp.iam.application.command.SetIamTenantColumnPermissionTemplatesCommand;
import com.xuan.erp.iam.application.command.UpdateIamRoleCommand;
import com.xuan.erp.iam.application.query.IamAssignablePermissionView;
import com.xuan.erp.iam.application.query.IamRolePermissionGrantView;
import com.xuan.erp.iam.application.service.IamColumnPermissionApplicationService;
import com.xuan.erp.iam.application.service.IamMenuApplicationService;
import com.xuan.erp.iam.application.service.IamPermissionApplicationService;
import com.xuan.erp.iam.application.service.IamRoleApplicationService;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplate;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplateItem;
import com.xuan.erp.iam.domain.model.IamTenantColumnPermissionTemplateAssignment;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.model.IamResourceColumn;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionRule;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionTemplateBinding;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionManagementRepository;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamTenantPermissionEntitlementRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamUser;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamAdminManagementApplicationServiceTest {

    @Test
    void createsMenuUnderExistingParentAndRejectsDuplicateCode() {
        InMemoryMenuRepository menuRepository = new InMemoryMenuRepository();
        IamMenuApplicationService service = new IamMenuApplicationService(menuRepository);
        IamMenu system = service.createMenu(new CreateIamMenuCommand(
                "system",
                null,
                "系统设置",
                "menu.system",
                "/system",
                "Settings",
                "iam:view",
                120));

        IamMenu roleMenu = service.createMenu(new CreateIamMenuCommand(
                "iam-role-management",
                system.id(),
                "角色管理",
                "menu.iamRoles",
                "/system/iam/roles",
                "ShieldCheck",
                "iam:view",
                122));

        assertEquals(system.id(), roleMenu.parentId());
        assertEquals("iam-role-management", roleMenu.code());
        assertEquals(122, roleMenu.sortNo());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createMenu(new CreateIamMenuCommand(
                        " iam-role-management ",
                        system.id(),
                        "重复角色管理",
                        "menu.iamRoles",
                        "/system/iam/roles-copy",
                        "ShieldCheck",
                        "iam:view",
                        123)));
        assertEquals("IAM_MENU_CODE_EXISTS", error.code());
    }

    @Test
    void createsPermissionAndRejectsDuplicateCode() {
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        IamPermissionApplicationService service = new IamPermissionApplicationService(permissionRepository);

        IamPermission permission = service.createPermission(new CreateIamPermissionCommand(
                "iam:role:update",
                "角色修改",
                "xuan-iam",
                "iam-role-management",
                "修改角色资料和授权"));

        assertEquals("iam:role:update", permission.code());
        assertTrue(permission.enabled());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createPermission(new CreateIamPermissionCommand(
                        "iam:role:update",
                        "重复角色修改",
                        "xuan-iam",
                        "iam-role-management",
                        "重复权限")));
        assertEquals("IAM_PERMISSION_CODE_EXISTS", error.code());
    }

    @Test
    void rejectsUpdatingPlatformRole() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        IamRole platformRole = roleRepository.save(new IamRole(
                1L, 0L, "super_admin", "平台超级管理员", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                new InMemoryPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryTenantPermissionEntitlementRepository());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.updateRole(platformRole.id(), new UpdateIamRoleCommand("平台管理员", "desc", true)));

        assertEquals("IAM_PLATFORM_ROLE_READ_ONLY", error.code());
    }

    @Test
    void rejectsDisablingPlatformRole() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        IamRole platformRole = roleRepository.save(new IamRole(
                1L, 0L, "super_admin", "平台超级管理员", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                new InMemoryPermissionRepository(),
                new InMemoryRolePermissionRepository(),
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryTenantPermissionEntitlementRepository());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.setRoleEnabled(platformRole.id(), false));

        assertEquals("IAM_PLATFORM_ROLE_READ_ONLY", error.code());
    }

    @Test
    void rejectsReplacingPlatformRolePermissions() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository(),
                new InMemoryTenantPermissionEntitlementRepository());
        IamRole platformRole = roleRepository.save(new IamRole(
                1L, 0L, "super_admin", "平台超级管理员", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        permissionRepository.save(permission("iam:view"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.replaceRolePermissions(new SetIamRolePermissionsCommand(
                        0L,
                        platformRole.id(),
                        List.of("iam:view"),
                        "security-admin")));

        assertEquals("IAM_PLATFORM_ROLE_READ_ONLY", error.code());
    }

    @Test
    void replacesRolePermissionsWithResolvedPermissionIds() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository,
                new InMemoryTenantPermissionEntitlementRepository());
        IamRole role = service.createRole(new CreateIamRoleCommand(1001L, "tenant_admin", "租户管理员", "管理租户配置"));
        permissionRepository.save(permission("iam:view"));
        permissionRepository.save(permission("iam:update"));
        InMemoryTenantPermissionEntitlementRepository entitlementRepository = new InMemoryTenantPermissionEntitlementRepository();
        entitlementRepository.permissionCodesByTenant.put(1001L, List.of("iam:view", "iam:update"));

        service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository,
                entitlementRepository);
        IamRolePermissionGrantView grant = service.replaceRolePermissions(new SetIamRolePermissionsCommand(
                1001L,
                role.id(),
                List.of("iam:update", "iam:view", "iam:view"),
                "security-admin"));

        assertEquals(List.of("iam:update", "iam:view"), grant.permissionCodes());
        assertEquals(List.of(1L, 2L), rolePermissionRepository.grantedPermissionIds);
        assertEquals("security-admin", rolePermissionRepository.operator);
    }

    @Test
    void refreshesAffectedUserAuthorizationSnapshotWhenRolePermissionsChange() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository,
                new InMemoryTenantPermissionEntitlementRepository());
        IamRole role = service.createRole(new CreateIamRoleCommand(1001L, "buyer", "采购员", "采购业务"));
        IamUser user = userRepository.save(user(10L, 1001L, "buyer01", 4L));
        rolePermissionRepository.userRoleIds.put(user.id(), List.of(role.id()));
        rolePermissionRepository.roleUserIds.put(role.id(), List.of(user.id()));
        permissionRepository.save(permission("procurement:view"));
        permissionRepository.save(permission("product:view"));
        InMemoryTenantPermissionEntitlementRepository entitlementRepository = new InMemoryTenantPermissionEntitlementRepository();
        entitlementRepository.permissionCodesByTenant.put(1001L, List.of("procurement:view", "product:view"));

        service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository,
                entitlementRepository);

        service.replaceRolePermissions(new SetIamRolePermissionsCommand(
                1001L,
                role.id(),
                List.of("procurement:view", "product:view"),
                "security-admin"));

        IamUser refreshedUser = userRepository.findById(user.id()).orElseThrow();
        IamAuthorizationSnapshot refreshedSnapshot = snapshotRepository
                .findByTenantIdAndUserId(1001L, user.id())
                .orElseThrow();
        assertEquals(5L, refreshedUser.authVersion());
        assertEquals(5L, refreshedSnapshot.authVersion());
        assertEquals(List.of(role.id()), refreshedSnapshot.roleIds());
        assertEquals(List.of("procurement:view", "product:view"), refreshedSnapshot.permissionCodes());
        assertEquals(List.of(), refreshedSnapshot.menuCodes());
    }

    @Test
    void rejectsRolePermissionOutsideTenantEntitlementPool() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        InMemoryTenantPermissionEntitlementRepository entitlementRepository = new InMemoryTenantPermissionEntitlementRepository();
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository,
                entitlementRepository);
        IamRole role = service.createRole(new CreateIamRoleCommand(1001L, "finance", "财务", "财务业务"));
        permissionRepository.save(permission("product:view"));
        permissionRepository.save(permission("finance:view"));
        entitlementRepository.permissionCodesByTenant.put(1001L, List.of("product:view"));

        BusinessException error = assertThrows(BusinessException.class, () -> service.replaceRolePermissions(
                new SetIamRolePermissionsCommand(1001L, role.id(), List.of("product:view", "finance:view"), "tenant-admin")));

        assertEquals("IAM_TENANT_PERMISSION_OUT_OF_SCOPE", error.code());
        assertEquals(List.of(), rolePermissionRepository.grantedPermissionIds);
    }

    @Test
    void getRolePermissionsReturnsTenantEntitlementPoolAsAvailablePermissions() {
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryTenantPermissionEntitlementRepository entitlementRepository = new InMemoryTenantPermissionEntitlementRepository();
        IamRoleApplicationService service = new IamRoleApplicationService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository(),
                entitlementRepository);
        IamRole role = service.createRole(new CreateIamRoleCommand(1001L, "buyer", "采购员", "采购业务"));
        permissionRepository.save(permission("product:view"));
        permissionRepository.save(permission("procurement:view"));
        permissionRepository.save(permission("finance:view"));
        rolePermissionRepository.rolePermissionCodes.put(role.id(), List.of("finance:view", "product:view"));
        entitlementRepository.permissionCodesByTenant.put(1001L, List.of("product:view", "procurement:view"));

        IamRolePermissionGrantView grant = service.getRolePermissions(1001L, role.id());

        assertEquals(List.of("product:view"), grant.permissionCodes());
        assertEquals(List.of("procurement:view", "product:view"), grant.availablePermissionCodes());
        assertEquals(List.of("procurement:view", "product:view"), grant.availablePermissions().stream()
                .map(IamAssignablePermissionView::code)
                .toList());
        assertEquals(List.of("procurement:view", "product:view"), grant.availablePermissions().stream()
                .map(IamAssignablePermissionView::name)
                .toList());
        assertTrue(grant.availablePermissions().stream().allMatch(IamAssignablePermissionView::enabled));
    }

    @Test
    void createsColumnPermissionTemplateAndRejectsDuplicateCodeInSameTenant() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        IamColumnPermissionApplicationService service = columnPermissionService(managementRepository);

        IamColumnPermissionTemplate template = service.createTemplate(new CreateIamColumnPermissionTemplateCommand(
                1001L,
                " tenant_readonly_masked ",
                "租户只读脱敏模板",
                "隐藏备注，联系电话脱敏",
                true,
                "security-admin"));

        assertEquals(1001L, template.tenantId());
        assertEquals("tenant_readonly_masked", template.code());
        assertTrue(template.enabled());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createTemplate(new CreateIamColumnPermissionTemplateCommand(
                        1001L,
                        "tenant_readonly_masked",
                        "重复模板",
                        null,
                        true,
                        "security-admin")));
        assertEquals("IAM_COLUMN_PERMISSION_TEMPLATE_CODE_EXISTS", error.code());
    }

    @Test
    void replacesColumnPermissionTemplateItemsAndRejectsInvalidAccessMode() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        managementRepository.templates.put(10L, new IamColumnPermissionTemplate(
                10L, 1001L, "tenant_readonly_masked", "租户只读脱敏模板", null, true));
        managementRepository.columns.put(1L, new IamResourceColumn(1L, "tenant", "contactPhone", "联系电话", "STRING", "PHONE", true, 10));
        managementRepository.columns.put(2L, new IamResourceColumn(2L, "tenant", "remark", "备注", "STRING", null, true, 20));
        IamColumnPermissionApplicationService service = columnPermissionService(managementRepository);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.replaceTemplateItems(new SetIamColumnPermissionTemplateItemsCommand(
                        10L,
                        List.of(new IamColumnPermissionTemplateItemCommand(1L, "WRITE")),
                        "security-admin")));
        assertEquals("IAM_COLUMN_PERMISSION_ACCESS_MODE_INVALID", error.code());

        List<IamColumnPermissionTemplateItem> items = service.replaceTemplateItems(new SetIamColumnPermissionTemplateItemsCommand(
                10L,
                List.of(
                        new IamColumnPermissionTemplateItemCommand(1L, "masked"),
                        new IamColumnPermissionTemplateItemCommand(2L, "HIDDEN")),
                "security-admin"));

        assertEquals(List.of("MASKED", "HIDDEN"), items.stream().map(IamColumnPermissionTemplateItem::accessMode).toList());
        assertEquals(List.of(1L, 2L), managementRepository.replacedResourceColumnIds);
        assertEquals("security-admin", managementRepository.operator);
    }

    @Test
    void bindsRoleToColumnPermissionTemplateAndRefreshesAffectedUserSnapshotColumnSettings() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        managementRepository.templates.put(8L, new IamColumnPermissionTemplate(
                8L, 0L, "tenant_readonly_masked", "租户只读脱敏模板", null, true));
        managementRepository.replaceTenantTemplateAssignments(1001L, List.of(8L), 8L, "security-admin");
        InMemoryColumnPermissionRepository runtimeRepository = new InMemoryColumnPermissionRepository();
        runtimeRepository.mergedRules = Map.of("tenant", Map.of("contactPhone", "MASKED", "remark", "HIDDEN"));
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        IamRole role = roleRepository.save(new IamRole(
                5L, 1001L, "tenant_reader", "租户只读", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamUser user = userRepository.save(user(10L, 1001L, "reader01", 3L));
        rolePermissionRepository.roleUserIds.put(role.id(), List.of(user.id()));
        rolePermissionRepository.userRoleIds.put(user.id(), List.of(role.id()));
        rolePermissionRepository.rolePermissionCodes.put(role.id(), List.of("tenant:view"));
        IamColumnPermissionApplicationService service = columnPermissionService(
                managementRepository,
                runtimeRepository,
                roleRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository);

        IamRoleColumnPermissionTemplateBinding binding = service.setRoleTemplateBinding(new SetIamRoleColumnPermissionTemplateCommand(
                1001L,
                role.id(),
                8L,
                "security-admin"));

        assertEquals(8L, binding.templateId());
        assertEquals(1001L, binding.tenantId());
        assertEquals(List.of(role.id()), snapshotRepository.findByTenantIdAndUserId(1001L, user.id()).orElseThrow().roleIds());
        assertEquals(Map.of("tenant", Map.of("contactPhone", "MASKED", "remark", "HIDDEN")),
                snapshotRepository.findByTenantIdAndUserId(1001L, user.id()).orElseThrow().columnSettings());
        assertEquals(4L, userRepository.findById(user.id()).orElseThrow().authVersion());
    }

    @Test
    void assignsColumnPermissionTemplatesToTenantAndRejectsRoleBindingOutsideTenantPool() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        managementRepository.templates.put(8L, new IamColumnPermissionTemplate(
                8L, 0L, "tenant_readonly_masked", "租户只读脱敏模板", null, true));
        managementRepository.templates.put(9L, new IamColumnPermissionTemplate(
                9L, 0L, "tenant_full_visible", "租户全字段模板", null, true));
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        IamRole role = roleRepository.save(new IamRole(
                5L, 1001L, "tenant_reader", "租户只读", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        IamColumnPermissionApplicationService service = columnPermissionService(
                managementRepository,
                new InMemoryColumnPermissionRepository(),
                roleRepository,
                new InMemoryRolePermissionRepository(),
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository());

        List<IamTenantColumnPermissionTemplateAssignment> assignments = service.replaceTenantTemplateAssignments(
                new SetIamTenantColumnPermissionTemplatesCommand(1001L, List.of(8L), 8L, "security-admin"));

        assertEquals(List.of(8L), assignments.stream().map(IamTenantColumnPermissionTemplateAssignment::templateId).toList());
        assertTrue(assignments.get(0).defaultTemplate());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.setRoleTemplateBinding(new SetIamRoleColumnPermissionTemplateCommand(
                        1001L,
                        role.id(),
                        9L,
                        "security-admin")));

        assertEquals("IAM_COLUMN_PERMISSION_TEMPLATE_NOT_ASSIGNED", error.code());
        assertEquals(Optional.empty(), managementRepository.findRoleTemplateBinding(1001L, role.id()));
    }

    @Test
    void assignsTenantColumnPermissionTemplatesByPlatformTemplateCodes() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        managementRepository.templates.put(8L, new IamColumnPermissionTemplate(
                8L, 0L, "tenant-basic", "租户基础列模板", null, true));
        managementRepository.templates.put(9L, new IamColumnPermissionTemplate(
                9L, 0L, "iam-user-basic", "用户基础列模板", null, true));
        IamColumnPermissionApplicationService service = columnPermissionService(
                managementRepository,
                new InMemoryColumnPermissionRepository(),
                new InMemoryRoleRepository(),
                new InMemoryRolePermissionRepository(),
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository());

        List<IamTenantColumnPermissionTemplateAssignment> assignments =
                service.replaceTenantTemplateAssignmentsByCodes(
                        1001L,
                        List.of(" tenant-basic ", "iam-user-basic", "tenant-basic"),
                        " tenant-basic ",
                        "tenant-plan-sync");

        assertEquals(List.of(8L, 9L), assignments.stream().map(IamTenantColumnPermissionTemplateAssignment::templateId).toList());
        assertEquals(List.of("tenant-basic", "iam-user-basic"), assignments.stream().map(IamTenantColumnPermissionTemplateAssignment::templateCode).toList());
        assertTrue(assignments.getFirst().defaultTemplate());
    }

    @Test
    void replacesRoleColumnPermissionRulesInsideTenantTemplateScopeAndRefreshesAffectedUserSnapshot() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        managementRepository.templates.put(8L, new IamColumnPermissionTemplate(
                8L, 0L, "tenant_readonly_masked", "租户只读脱敏模板", null, true));
        managementRepository.columns.put(1L, new IamResourceColumn(1L, "tenant", "contactPhone", "联系电话", "STRING", "PHONE", true, 10));
        managementRepository.columns.put(2L, new IamResourceColumn(2L, "tenant", "remark", "备注", "STRING", null, true, 20));
        managementRepository.templateItems.put(8L, List.of(
                new IamColumnPermissionTemplateItem(null, 8L, 1L, "tenant", "contactPhone", "联系电话", "MASKED"),
                new IamColumnPermissionTemplateItem(null, 8L, 2L, "tenant", "remark", "备注", "VISIBLE")));
        managementRepository.replaceTenantTemplateAssignments(1001L, List.of(8L), 8L, "security-admin");
        InMemoryColumnPermissionRepository runtimeRepository = new InMemoryColumnPermissionRepository();
        runtimeRepository.mergedRules = Map.of("tenant", Map.of("contactPhone", "MASKED", "remark", "HIDDEN"));
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        IamRole role = roleRepository.save(new IamRole(
                5L, 1001L, "tenant_reader", "租户只读", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        InMemoryRolePermissionRepository rolePermissionRepository = new InMemoryRolePermissionRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository();
        IamUser user = userRepository.save(user(10L, 1001L, "reader01", 3L));
        rolePermissionRepository.roleUserIds.put(role.id(), List.of(user.id()));
        rolePermissionRepository.userRoleIds.put(user.id(), List.of(role.id()));
        rolePermissionRepository.rolePermissionCodes.put(role.id(), List.of("tenant:view"));
        IamColumnPermissionApplicationService service = columnPermissionService(
                managementRepository,
                runtimeRepository,
                roleRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository);

        List<IamRoleColumnPermissionRule> rules = service.replaceRoleColumnPermissionRules(new SetIamRoleColumnPermissionsCommand(
                1001L,
                role.id(),
                List.of(
                        new IamRoleColumnPermissionRuleCommand(1L, "MASKED"),
                        new IamRoleColumnPermissionRuleCommand(2L, "HIDDEN")),
                "security-admin"));

        assertEquals(List.of("MASKED", "HIDDEN"), rules.stream().map(IamRoleColumnPermissionRule::accessMode).toList());
        assertEquals(List.of(1L, 2L), managementRepository.replacedRoleResourceColumnIds);
        assertEquals(Map.of("tenant", Map.of("contactPhone", "MASKED", "remark", "HIDDEN")),
                snapshotRepository.findByTenantIdAndUserId(1001L, user.id()).orElseThrow().columnSettings());
        assertEquals(4L, userRepository.findById(user.id()).orElseThrow().authVersion());
    }

    @Test
    void rejectsRoleColumnPermissionRuleOutsideTenantTemplateScope() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        managementRepository.templates.put(8L, new IamColumnPermissionTemplate(
                8L, 0L, "tenant_readonly_masked", "租户只读脱敏模板", null, true));
        managementRepository.columns.put(1L, new IamResourceColumn(1L, "tenant", "contactPhone", "联系电话", "STRING", "PHONE", true, 10));
        managementRepository.columns.put(2L, new IamResourceColumn(2L, "tenant", "remark", "备注", "STRING", null, true, 20));
        managementRepository.templateItems.put(8L, List.of(
                new IamColumnPermissionTemplateItem(null, 8L, 1L, "tenant", "contactPhone", "联系电话", "MASKED")));
        managementRepository.replaceTenantTemplateAssignments(1001L, List.of(8L), 8L, "security-admin");
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        IamRole role = roleRepository.save(new IamRole(
                5L, 1001L, "tenant_reader", "租户只读", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        IamColumnPermissionApplicationService service = columnPermissionService(
                managementRepository,
                new InMemoryColumnPermissionRepository(),
                roleRepository,
                new InMemoryRolePermissionRepository(),
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository());

        BusinessException wider = assertThrows(BusinessException.class,
                () -> service.replaceRoleColumnPermissionRules(new SetIamRoleColumnPermissionsCommand(
                        1001L,
                        role.id(),
                        List.of(new IamRoleColumnPermissionRuleCommand(1L, "VISIBLE")),
                        "security-admin")));
        BusinessException missing = assertThrows(BusinessException.class,
                () -> service.replaceRoleColumnPermissionRules(new SetIamRoleColumnPermissionsCommand(
                        1001L,
                        role.id(),
                        List.of(new IamRoleColumnPermissionRuleCommand(2L, "HIDDEN")),
                        "security-admin")));

        assertEquals("IAM_COLUMN_PERMISSION_RULE_OUT_OF_SCOPE", wider.code());
        assertEquals("IAM_COLUMN_PERMISSION_RULE_OUT_OF_SCOPE", missing.code());
        assertEquals(List.of(), managementRepository.replacedRoleResourceColumnIds);
    }

    @Test
    void getsColumnPermissionTemplateBindingForPlatformRole() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        managementRepository.templates.put(8L, new IamColumnPermissionTemplate(
                8L, 0L, "platform_readonly_masked", "平台只读脱敏模板", null, true));
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        IamRole role = roleRepository.save(new IamRole(
                1L, 0L, "super_admin", "平台超级管理员", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        managementRepository.replaceRoleTemplateBinding(0L, role.id(), 8L, "system");
        IamColumnPermissionApplicationService service = columnPermissionService(
                managementRepository,
                new InMemoryColumnPermissionRepository(),
                roleRepository,
                new InMemoryRolePermissionRepository(),
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository());

        IamRoleColumnPermissionTemplateBinding binding = service.getRoleTemplateBinding(0L, role.id());

        assertEquals(0L, binding.tenantId());
        assertEquals(role.id(), binding.roleId());
        assertEquals(8L, binding.templateId());
        assertEquals("platform_readonly_masked", binding.templateCode());
    }

    @Test
    void rejectsColumnPermissionTemplateBindingWhenRoleTenantDoesNotMatch() {
        InMemoryColumnPermissionManagementRepository managementRepository = new InMemoryColumnPermissionManagementRepository();
        managementRepository.templates.put(8L, new IamColumnPermissionTemplate(
                8L, 0L, "tenant_readonly_masked", "租户只读脱敏模板", null, true));
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        IamRole role = roleRepository.save(new IamRole(
                5L, 1001L, "tenant_reader", "租户只读", null, true,
                "system", OffsetDateTime.now(), "system", OffsetDateTime.now(), null, null, null));
        IamColumnPermissionApplicationService service = columnPermissionService(
                managementRepository,
                new InMemoryColumnPermissionRepository(),
                roleRepository,
                new InMemoryRolePermissionRepository(),
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.setRoleTemplateBinding(new SetIamRoleColumnPermissionTemplateCommand(
                        2002L,
                        role.id(),
                        8L,
                        "security-admin")));

        assertEquals("IAM_ROLE_TENANT_MISMATCH", error.code());
        assertEquals(Optional.empty(), managementRepository.findRoleTemplateBinding(2002L, role.id()));
    }

    private static IamPermission permission(String code) {
        return new IamPermission(
                null,
                code,
                code,
                "xuan-iam",
                "system",
                code,
                true,
                "system",
                OffsetDateTime.now(),
                "system",
                OffsetDateTime.now(),
                null,
                null,
                null);
    }

    private static IamUser user(Long id, Long tenantId, String username, long authVersion) {
        OffsetDateTime now = OffsetDateTime.now();
        return new IamUser(
                id,
                tenantId,
                username,
                "{noop}password",
                username,
                null,
                null,
                null,
                true,
                true,
                true,
                true,
                null,
                now,
                0,
                null,
                null,
                false,
                authVersion,
                null,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null);
    }

    private static IamColumnPermissionApplicationService columnPermissionService(
            InMemoryColumnPermissionManagementRepository managementRepository) {
        return columnPermissionService(
                managementRepository,
                new InMemoryColumnPermissionRepository(),
                new InMemoryRoleRepository(),
                new InMemoryRolePermissionRepository(),
                new InMemoryUserRepository(),
                new InMemoryAuthorizationSnapshotRepository());
    }

    private static IamColumnPermissionApplicationService columnPermissionService(
            InMemoryColumnPermissionManagementRepository managementRepository,
            InMemoryColumnPermissionRepository runtimeRepository,
            InMemoryRoleRepository roleRepository,
            InMemoryRolePermissionRepository rolePermissionRepository,
            InMemoryUserRepository userRepository,
            InMemoryAuthorizationSnapshotRepository snapshotRepository) {
        return new IamColumnPermissionApplicationService(
                managementRepository,
                runtimeRepository,
                roleRepository,
                rolePermissionRepository,
                userRepository,
                snapshotRepository);
    }

    private static final class InMemoryMenuRepository implements IamMenuRepository {
        private final Map<Long, IamMenu> byId = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<IamMenu> findById(Long id) {
            return Optional.ofNullable(byId.get(id)).filter(IamMenu::active);
        }

        @Override
        public Optional<IamMenu> findByCode(String code) {
            return byId.values().stream()
                    .filter(IamMenu::active)
                    .filter(menu -> menu.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<IamMenu> findActiveMenus() {
            return byId.values().stream()
                    .filter(IamMenu::active)
                    .sorted(Comparator.comparingInt(IamMenu::sortNo).thenComparing(IamMenu::code))
                    .toList();
        }

        @Override
        public IamMenu save(IamMenu menu) {
            Long id = menu.id() == null ? nextId++ : menu.id();
            IamMenu saved = new IamMenu(
                    id,
                    menu.code(),
                    menu.parentId(),
                    menu.title(),
                    menu.i18nKey(),
                    menu.path(),
                    menu.icon(),
                    menu.permissionCode(),
                    menu.sortNo(),
                    menu.enabled(),
                    menu.createdBy(),
                    menu.createdAt(),
                    menu.updatedBy(),
                    menu.updatedAt(),
                    menu.deletedBy(),
                    menu.deleteReason(),
                    menu.deletedAt());
            byId.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryPermissionRepository implements IamPermissionRepository {
        private final Map<Long, IamPermission> byId = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<IamPermission> findById(Long id) {
            return Optional.ofNullable(byId.get(id)).filter(IamPermission::active);
        }

        @Override
        public Optional<IamPermission> findByCode(String code) {
            return byId.values().stream()
                    .filter(IamPermission::active)
                    .filter(permission -> permission.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<IamPermission> findActivePermissions() {
            return new ArrayList<>(byId.values());
        }

        @Override
        public IamPermission save(IamPermission permission) {
            Long id = permission.id() == null ? nextId++ : permission.id();
            IamPermission saved = new IamPermission(
                    id,
                    permission.code(),
                    permission.name(),
                    permission.serviceName(),
                    permission.menuCode(),
                    permission.description(),
                    permission.enabled(),
                    permission.createdBy(),
                    permission.createdAt(),
                    permission.updatedBy(),
                    permission.updatedAt(),
                    permission.deletedBy(),
                    permission.deleteReason(),
                    permission.deletedAt());
            byId.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryRoleRepository implements IamRoleRepository {
        private final Map<Long, IamRole> byId = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<IamRole> findById(Long id) {
            return Optional.ofNullable(byId.get(id)).filter(IamRole::active);
        }

        @Override
        public Optional<IamRole> findActiveByTenantIdAndCode(Long tenantId, String code) {
            return byId.values().stream()
                    .filter(IamRole::active)
                    .filter(role -> role.tenantId().equals(tenantId))
                    .filter(role -> role.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<IamRole> findActiveRoles(Long tenantId) {
            return byId.values().stream()
                    .filter(IamRole::active)
                    .filter(role -> role.tenantId().equals(tenantId))
                    .toList();
        }

        @Override
        public IamRole save(IamRole role) {
            Long id = role.id() == null ? nextId++ : role.id();
            IamRole saved = new IamRole(
                    id,
                    role.tenantId(),
                    role.code(),
                    role.name(),
                    role.description(),
                    role.enabled(),
                    role.createdBy(),
                    role.createdAt(),
                    role.updatedBy(),
                    role.updatedAt(),
                    role.deletedBy(),
                    role.deleteReason(),
                    role.deletedAt());
            byId.put(id, saved);
            return saved;
        }
    }

    private static final class InMemoryRolePermissionRepository implements IamRolePermissionRepository {
        private List<Long> grantedPermissionIds = List.of();
        private String operator;
        private final Map<Long, List<Long>> roleUserIds = new LinkedHashMap<>();
        private final Map<Long, List<Long>> userRoleIds = new LinkedHashMap<>();
        private final Map<Long, List<String>> rolePermissionCodes = new LinkedHashMap<>();

        @Override
        public List<String> findPermissionCodesByRoleId(Long tenantId, Long roleId) {
            return rolePermissionCodes.getOrDefault(roleId, List.of());
        }

        @Override
        public void replaceRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds, String operator) {
            this.grantedPermissionIds = List.copyOf(permissionIds);
            this.operator = operator;
        }

        @Override
        public List<Long> findUserIdsByRoleId(Long tenantId, Long roleId) {
            return roleUserIds.getOrDefault(roleId, List.of());
        }

        @Override
        public List<Long> findRoleIdsByUserId(Long tenantId, Long userId) {
            return userRoleIds.getOrDefault(userId, List.of());
        }

        @Override
        public List<String> findPermissionCodesByUserId(Long tenantId, Long userId) {
            List<Long> roleIds = userRoleIds.getOrDefault(userId, List.of());
            if (!roleIds.isEmpty()) {
                List<String> permissionCodes = roleIds.stream()
                        .flatMap(roleId -> rolePermissionCodes.getOrDefault(roleId, List.of()).stream())
                        .distinct()
                        .sorted()
                        .toList();
                if (!permissionCodes.isEmpty()) {
                    return permissionCodes;
                }
            }
            return grantedPermissionIds.stream()
                    .map(id -> id == 1L ? "procurement:view" : "product:view")
                    .sorted()
                    .toList();
        }

        @Override
        public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds, String operator) {
            userRoleIds.put(userId, List.copyOf(roleIds));
        }

        @Override
        public void removeRolePermissionsOutsideTenantEntitlements(Long tenantId, String operator) {
        }
    }

    private static final class InMemoryTenantPermissionEntitlementRepository implements IamTenantPermissionEntitlementRepository {
        private final Map<Long, List<String>> permissionCodesByTenant = new LinkedHashMap<>();

        @Override
        public List<String> findPermissionCodesByTenantId(Long tenantId) {
            return permissionCodesByTenant.getOrDefault(tenantId, List.of()).stream()
                    .sorted()
                    .toList();
        }

        @Override
        public List<Long> findTenantIdsByInitTemplateCode(String initTemplateCode) {
            return List.of();
        }

        @Override
        public void replaceTenantEntitlements(Long tenantId, String initTemplateCode, List<Long> permissionIds, long entitlementVersion, String operator) {
        }

        @Override
        public long nextEntitlementVersion(Long tenantId) {
            return 1;
        }
    }

    private static final class InMemoryColumnPermissionManagementRepository implements IamColumnPermissionManagementRepository {
        private final Map<Long, IamColumnPermissionTemplate> templates = new LinkedHashMap<>();
        private final Map<Long, IamResourceColumn> columns = new LinkedHashMap<>();
        private final Map<Long, List<IamColumnPermissionTemplateItem>> templateItems = new LinkedHashMap<>();
        private final Map<String, List<IamRoleColumnPermissionRule>> roleRules = new LinkedHashMap<>();
        private final Map<String, IamRoleColumnPermissionTemplateBinding> bindings = new LinkedHashMap<>();
        private final Map<Long, List<Long>> tenantTemplateIds = new LinkedHashMap<>();
        private final Map<Long, Long> tenantDefaultTemplateIds = new LinkedHashMap<>();
        private List<Long> replacedResourceColumnIds = List.of();
        private List<Long> replacedRoleResourceColumnIds = List.of();
        private String operator;
        private long nextTemplateId = 1;

        @Override
        public List<IamResourceColumn> findEnabledResourceColumns() {
            return columns.values().stream()
                    .filter(IamResourceColumn::enabled)
                    .toList();
        }

        @Override
        public List<IamColumnPermissionTemplate> findActiveTemplates(Long tenantId, String keyword, Boolean enabled) {
            return templates.values().stream()
                    .filter(template -> tenantId == null || template.tenantId().equals(tenantId) || template.tenantId() == 0)
                    .filter(template -> enabled == null || template.enabled() == enabled)
                    .filter(template -> keyword == null || keyword.isBlank()
                            || template.code().contains(keyword.trim())
                            || template.name().contains(keyword.trim()))
                    .toList();
        }

        @Override
        public Optional<IamColumnPermissionTemplate> findTemplateById(Long templateId) {
            return Optional.ofNullable(templates.get(templateId));
        }

        @Override
        public Optional<IamColumnPermissionTemplate> findTemplateByTenantIdAndCode(Long tenantId, String code) {
            return templates.values().stream()
                    .filter(template -> template.tenantId().equals(tenantId))
                    .filter(template -> template.code().equals(code))
                    .findFirst();
        }

        @Override
        public IamColumnPermissionTemplate saveTemplate(IamColumnPermissionTemplate template, String operator) {
            long id = template.id() == null ? nextTemplateId++ : template.id();
            IamColumnPermissionTemplate saved = new IamColumnPermissionTemplate(
                    id,
                    template.tenantId(),
                    template.code(),
                    template.name(),
                    template.description(),
                    template.enabled());
            templates.put(id, saved);
            return saved;
        }

        @Override
        public void setTemplateEnabled(Long templateId, boolean enabled, String operator) {
            IamColumnPermissionTemplate template = templates.get(templateId);
            templates.put(templateId, new IamColumnPermissionTemplate(
                    template.id(),
                    template.tenantId(),
                    template.code(),
                    template.name(),
                    template.description(),
                    enabled));
        }

        @Override
        public List<IamColumnPermissionTemplateItem> findTemplateItems(Long templateId) {
            return templateItems.getOrDefault(templateId, List.of());
        }

        @Override
        public Optional<IamResourceColumn> findResourceColumnById(Long resourceColumnId) {
            return Optional.ofNullable(columns.get(resourceColumnId));
        }

        @Override
        public void replaceTemplateItems(Long templateId, List<IamColumnPermissionTemplateItem> items, String operator) {
            templateItems.put(templateId, List.copyOf(items));
            this.replacedResourceColumnIds = items.stream()
                    .map(IamColumnPermissionTemplateItem::resourceColumnId)
                    .toList();
            this.operator = operator;
        }

        @Override
        public List<IamTenantColumnPermissionTemplateAssignment> findTenantTemplateAssignments(Long tenantId) {
            Long defaultTemplateId = tenantDefaultTemplateIds.get(tenantId);
            return tenantTemplateIds.getOrDefault(tenantId, List.of()).stream()
                    .map(templates::get)
                    .filter(Objects::nonNull)
                    .map(template -> new IamTenantColumnPermissionTemplateAssignment(
                            tenantId,
                            template.id(),
                            template.code(),
                            template.name(),
                            template.description(),
                            template.id().equals(defaultTemplateId),
                            template.enabled()))
                    .toList();
        }

        @Override
        public boolean existsTenantTemplateAssignment(Long tenantId, Long templateId) {
            IamColumnPermissionTemplate template = templates.get(templateId);
            return template != null
                    && template.enabled()
                    && tenantTemplateIds.getOrDefault(tenantId, List.of()).contains(templateId);
        }

        @Override
        public void replaceTenantTemplateAssignments(Long tenantId, List<Long> templateIds, Long defaultTemplateId, String operator) {
            tenantTemplateIds.put(tenantId, List.copyOf(templateIds));
            if (defaultTemplateId == null) {
                tenantDefaultTemplateIds.remove(tenantId);
            } else {
                tenantDefaultTemplateIds.put(tenantId, defaultTemplateId);
            }
        }

        @Override
        public List<IamColumnPermissionTemplateItem> findTenantAssignableColumnRules(Long tenantId) {
            return tenantTemplateIds.getOrDefault(tenantId, List.of()).stream()
                    .flatMap(templateId -> templateItems.getOrDefault(templateId, List.of()).stream())
                    .toList();
        }

        @Override
        public List<IamRoleColumnPermissionRule> findRoleColumnPermissionRules(Long tenantId, Long roleId) {
            return roleRules.getOrDefault(tenantId + ":" + roleId, List.of());
        }

        @Override
        public void replaceRoleColumnPermissionRules(Long tenantId, Long roleId, List<IamRoleColumnPermissionRule> rules, String operator) {
            roleRules.put(tenantId + ":" + roleId, List.copyOf(rules));
            replacedRoleResourceColumnIds = rules.stream()
                    .map(IamRoleColumnPermissionRule::resourceColumnId)
                    .toList();
            this.operator = operator;
        }

        @Override
        public void disableRoleColumnPermissionRulesOutsideTenantAssignments(Long tenantId, String operator) {
            List<Long> assignableColumnIds = findTenantAssignableColumnRules(tenantId).stream()
                    .map(IamColumnPermissionTemplateItem::resourceColumnId)
                    .toList();
            roleRules.replaceAll((key, rules) -> key.startsWith(tenantId + ":")
                    ? rules.stream().filter(rule -> assignableColumnIds.contains(rule.resourceColumnId())).toList()
                    : rules);
        }

        @Override
        public Optional<IamRoleColumnPermissionTemplateBinding> findRoleTemplateBinding(Long tenantId, Long roleId) {
            return Optional.ofNullable(bindings.get(tenantId + ":" + roleId));
        }

        @Override
        public List<IamRoleColumnPermissionTemplateBinding> findRoleBindingsByTemplateId(Long templateId) {
            return bindings.values().stream()
                    .filter(binding -> binding.templateId().equals(templateId))
                    .toList();
        }

        @Override
        public void replaceRoleTemplateBinding(Long tenantId, Long roleId, Long templateId, String operator) {
            IamColumnPermissionTemplate template = templates.get(templateId);
            bindings.put(tenantId + ":" + roleId, new IamRoleColumnPermissionTemplateBinding(
                    tenantId,
                    roleId,
                    templateId,
                    template.code(),
                    template.name()));
        }

        @Override
        public void disableRoleTemplateBindingsOutsideTenantAssignments(Long tenantId, String operator) {
            List<Long> assignedTemplateIds = tenantTemplateIds.getOrDefault(tenantId, List.of());
            bindings.entrySet().removeIf(entry -> entry.getValue().tenantId().equals(tenantId)
                    && !assignedTemplateIds.contains(entry.getValue().templateId()));
        }
    }

    private static final class InMemoryColumnPermissionRepository implements IamColumnPermissionRepository {
        private Map<String, Map<String, String>> mergedRules = Map.of();

        @Override
        public Map<String, Map<String, String>> findMergedColumnPermissionsByRoleIds(Long tenantId, List<Long> roleIds) {
            return mergedRules;
        }
    }

    private static final class InMemoryUserRepository implements IamUserRepository {
        private final Map<Long, IamUser> byId = new LinkedHashMap<>();

        @Override
        public Optional<IamUser> findById(Long id) {
            return Optional.ofNullable(byId.get(id)).filter(IamUser::active);
        }

        @Override
        public Optional<IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username) {
            return byId.values().stream()
                    .filter(IamUser::active)
                    .filter(user -> user.tenantId().equals(tenantId))
                    .filter(user -> user.username().equals(username))
                    .findFirst();
        }

        @Override
        public List<IamUser> findActiveUsers(Long tenantId) {
            return byId.values().stream()
                    .filter(IamUser::active)
                    .filter(user -> user.tenantId().equals(tenantId))
                    .toList();
        }

        @Override
        public IamUser save(IamUser user) {
            byId.put(user.id(), user);
            return user;
        }
    }

    private static final class InMemoryAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {
        private final Map<String, IamAuthorizationSnapshot> byTenantAndUser = new LinkedHashMap<>();

        @Override
        public Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) {
            return Optional.ofNullable(byTenantAndUser.get(tenantId + ":" + userId));
        }

        @Override
        public IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot) {
            byTenantAndUser.put(snapshot.tenantId() + ":" + snapshot.userId(), snapshot);
            return snapshot;
        }
    }
}
