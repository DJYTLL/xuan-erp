package com.xuan.erp.iam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamAdminControllerContractTest {

    private static final Path CONTROLLER_DIR = Path.of("src/main/java/com/xuan/erp/iam/interfaces/controller");
    private static final Path DTO_DIR = Path.of("src/main/java/com/xuan/erp/iam/interfaces/dto");
    private static final Path CONFIG_DIR = Path.of("src/main/java/com/xuan/erp/iam/infrastructure/config");

    @Test
    void iamControllersUseXuanPermissionExpressionForWildcardSupport() throws IOException {
        try (Stream<Path> controllerPaths = Files.list(CONTROLLER_DIR)) {
            controllerPaths
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .forEach(path -> {
                        String source = readUnchecked(path);
                        assertFalse(source.contains("hasAuthority("),
                                path.getFileName() + " 不能直接使用 Spring hasAuthority，避免 * 通配权限失效");
                        assertFalse(source.contains("hasAnyAuthority("),
                                path.getFileName() + " 不能直接使用 Spring hasAnyAuthority，避免 * 通配权限失效");
                    });
        }
    }

    @Test
    void iamSecurityConfigurationProvidesLocalXuanPermissionExpression() throws IOException {
        String securityConfiguration = Files.readString(CONFIG_DIR.resolve("IamSecurityConfiguration.java"));

        assertTrue(securityConfiguration.contains("@Bean(\"xuanPermission\")"),
                "IAM 自己作为权限事实源，必须提供本地 xuanPermission 表达式，不能依赖远程快照自动配置");
        assertTrue(securityConfiguration.contains("@ConditionalOnMissingBean(name = \"xuanPermission\")"));
        assertTrue(securityConfiguration.contains("new XuanPermissionExpression"));
        assertTrue(securityConfiguration.contains("new PermissionSnapshot"));
        assertTrue(securityConfiguration.contains("currentUser.permissions()"),
                "IAM 本地 xuanPermission 必须直接使用 token 中的当前权限集合，保证 * 通配在方法级权限中生效");
    }

    @Test
    void menuPermissionAndRoleControllersExposeAdminWriteEndpoints() throws IOException {
        String menuController = Files.readString(CONTROLLER_DIR.resolve("IamMenuController.java"));
        String permissionController = Files.readString(CONTROLLER_DIR.resolve("IamPermissionController.java"));
        String roleController = Files.readString(CONTROLLER_DIR.resolve("IamRoleController.java"));
        String userController = Files.readString(CONTROLLER_DIR.resolve("IamUserController.java"));
        String initTemplateController = Files.readString(CONTROLLER_DIR.resolve("IamTenantInitTemplateController.java"));

        assertTrue(menuController.contains("@PostMapping"), "菜单管理必须支持新增菜单");
        assertTrue(menuController.contains("@PutMapping(\"/{menuId}\")"), "菜单管理必须支持修改菜单");
        assertTrue(menuController.contains("@PostMapping(\"/{menuId}/enable\")"), "菜单管理必须支持启用菜单");
        assertTrue(menuController.contains("@PostMapping(\"/{menuId}/disable\")"), "菜单管理必须支持停用菜单");

        assertTrue(permissionController.contains("@PostMapping"), "权限管理必须支持新增权限");
        assertTrue(permissionController.contains("@PutMapping(\"/{permissionId}\")"), "权限管理必须支持修改权限");
        assertTrue(permissionController.contains("@PostMapping(\"/{permissionId}/enable\")"), "权限管理必须支持启用权限");
        assertTrue(permissionController.contains("@PostMapping(\"/{permissionId}/disable\")"), "权限管理必须支持停用权限");

        assertTrue(roleController.contains("@PostMapping"), "角色管理必须支持新增角色");
        assertTrue(roleController.contains("@PutMapping(\"/{roleId}\")"), "角色管理必须支持修改角色");
        assertTrue(roleController.contains("@GetMapping(\"/{roleId}/permissions\")"), "角色管理必须支持查询角色权限");
        assertTrue(roleController.contains("@PutMapping(\"/{roleId}/permissions\")"), "角色管理必须支持保存角色权限");

        assertTrue(userController.contains("@GetMapping(\"/{userId}/roles\")"), "用户管理必须支持查询用户角色");
        assertTrue(userController.contains("@PutMapping(\"/{userId}/roles\")"), "用户管理必须支持保存用户角色");
        assertTrue(userController.contains("@PutMapping(\"/{userId}\")"), "用户管理必须支持修改用户资料");
        assertTrue(userController.contains("@PostMapping(\"/{userId}/reset-password\")"), "用户管理必须支持重置用户密码");
        assertTrue(userController.contains("@PostMapping(\"/tenants/{tenantId}/admin/reset-password\")"),
                "租户管理必须支持按租户重置 admin 管理员密码");

        assertTrue(initTemplateController.contains("@GetMapping"), "初始化模板必须支持查询模板列表");
        assertTrue(initTemplateController.contains("@PostMapping"), "初始化模板必须支持新增模板");
        assertTrue(initTemplateController.contains("@PutMapping(\"/{templateId}\")"), "初始化模板必须支持修改模板");
        assertTrue(initTemplateController.contains("@PutMapping(\"/{templateId}/permissions\")"), "初始化模板必须支持保存模板权限");
    }

    @Test
    void iamAdminControllersUsePageScopedPermissionCodes() throws IOException {
        String menuController = Files.readString(CONTROLLER_DIR.resolve("IamMenuController.java"));
        String permissionController = Files.readString(CONTROLLER_DIR.resolve("IamPermissionController.java"));
        String roleController = Files.readString(CONTROLLER_DIR.resolve("IamRoleController.java"));
        String userController = Files.readString(CONTROLLER_DIR.resolve("IamUserController.java"));
        String initTemplateController = Files.readString(CONTROLLER_DIR.resolve("IamTenantInitTemplateController.java"));

        assertTrue(menuController.contains("@GetMapping(\"/options\")"), "菜单选项应提供专用只读接口，不能复用菜单管理列表接口");
        assertTrue(menuController.contains("@xuanPermission.has('iam-menu:view')"), "菜单管理列表接口必须只绑定菜单管理查看权限");
        assertTrue(menuController.contains("@xuanPermission.hasAny("), "菜单选项接口应允许相关配置页只读复用");
        assertTrue(menuController.contains("'iam-column-permission:view'"));
        assertTrue(menuController.contains("'iam-role-column-permission:view'"));
        assertTrue(menuController.contains("'iam-permission:view'"));
        assertTrue(menuController.contains("'iam-init-template:view'"));
        assertTrue(menuController.contains("@xuanPermission.has('iam-menu:create')"));
        assertTrue(menuController.contains("@xuanPermission.has('iam-menu:update')"));

        assertTrue(permissionController.contains("@xuanPermission.has('iam-permission:view')"));
        assertTrue(permissionController.contains("@xuanPermission.has('iam-permission:create')"));
        assertTrue(permissionController.contains("@xuanPermission.has('iam-permission:update')"));

        assertTrue(roleController.contains("@xuanPermission.has('iam-role:view')"));
        assertTrue(roleController.contains("@xuanPermission.has('iam-role:create')"));
        assertTrue(roleController.contains("@xuanPermission.has('iam-role:update')"));

        assertTrue(userController.contains("@xuanPermission.has('iam-user:view')"));
        assertTrue(userController.contains("@xuanPermission.has('iam-user:create')"));
        assertTrue(userController.contains("@xuanPermission.has('iam-user:update')"));
        assertTrue(userController.contains("@xuanPermission.has('iam-user:reset-password')"));
        assertTrue(userController.contains("@xuanPermission.has('tenant:admin-password:reset')"));
        assertTrue(userController.contains("@xuanPermission.has('iam-user:delete')"));

        assertTrue(initTemplateController.contains("@xuanPermission.has('iam-init-template:view')"));
        assertTrue(initTemplateController.contains("@xuanPermission.has('iam-init-template:create')"));
        assertTrue(initTemplateController.contains("@xuanPermission.has('iam-init-template:update')"));

        String combined = menuController + permissionController + roleController + userController + initTemplateController;
        assertFalse(combined.contains("hasAuthority("), "IAM 管理 Controller 不能直接使用 Spring hasAuthority，避免 * 通配权限失效");
        assertFalse(combined.contains("hasAnyAuthority("), "IAM 管理 Controller 不能直接使用 Spring hasAnyAuthority，避免 * 通配权限失效");
        assertFalse(combined.contains("hasAuthority('iam:view')"), "IAM 管理页面不能继续共用 iam:view");
        assertFalse(combined.contains("hasAuthority('iam:create')"), "IAM 管理页面不能继续共用 iam:create");
        assertFalse(combined.contains("hasAuthority('iam:update')"), "IAM 管理页面不能继续共用 iam:update");
        assertFalse(combined.contains("hasAuthority('iam:delete')"), "IAM 管理页面不能继续共用 iam:delete");
    }

    @Test
    void tenantBootstrapHttpContractAcceptsIamInitTemplateCode() throws IOException {
        String bootstrapController = Files.readString(CONTROLLER_DIR.resolve("IamTenantBootstrapController.java"));
        String bootstrapRequest = Files.readString(DTO_DIR.resolve("IamTenantBootstrapRequest.java"));

        assertTrue(bootstrapRequest.contains("String iamInitTemplateCode"), "HTTP 初始化请求必须能携带套餐绑定的 IAM 初始化模板编码");
        assertTrue(bootstrapRequest.contains("String permissionHash"), "HTTP 初始化请求必须携带 Tenant 计算出的套餐权限指纹");
        assertTrue(bootstrapController.contains("request == null ? null : request.iamInitTemplateCode()"),
                "Controller 必须把 iamInitTemplateCode 传入 bootstrap 应用服务，避免 Feign 同步只能使用默认模板");
        assertTrue(bootstrapController.contains("request == null ? null : request.permissionHash()"),
                "Controller 必须把 permissionHash 传入 bootstrap 应用服务，避免 IAM lastSyncedPermissionHash 漂移");
        assertTrue(bootstrapController.contains("@GetMapping(\"/{tenantId}/permission-sync-state\")"),
                "Controller 必须提供 Tenant 对比 IAM lastSyncedPermissionHash 的只读接口");
        assertTrue(bootstrapController.contains("lastSyncedPermissionHash(tenantId)"),
                "Controller 必须从 IAM 同步状态仓储读取 lastSyncedPermissionHash");
    }

    @Test
    void columnPermissionControllerExposesManagementEndpointsWithSwaggerAndPermissions() throws IOException {
        Path controllerPath = CONTROLLER_DIR.resolve("IamColumnPermissionController.java");
        assertTrue(Files.exists(controllerPath), "必须提供 IAM 列权限模板管理 Controller");

        String controller = Files.readString(controllerPath);

        assertTrue(controller.contains("@Tag"), "列权限管理 Controller 必须有 Swagger @Tag");
        assertTrue(controller.contains("@Operation"), "列权限管理 Controller 每个端点必须有 Swagger @Operation");
        assertTrue(controller.contains("@Parameter"), "列权限管理 Controller 路径和查询参数必须有 Swagger @Parameter");
        assertTrue(controller.contains("@RequestMapping(\"/api/iam/column-permissions\")"));
        assertTrue(controller.contains("@GetMapping(\"/resources\")"));
        assertTrue(controller.contains("@GetMapping(\"/templates\")"));
        assertTrue(controller.contains("@PostMapping(\"/templates\")"));
        assertTrue(controller.contains("@PutMapping(\"/templates/{templateId}\")"));
        assertTrue(controller.contains("@PostMapping(\"/templates/{templateId}/enable\")"));
        assertTrue(controller.contains("@PostMapping(\"/templates/{templateId}/disable\")"));
        assertTrue(controller.contains("@GetMapping(\"/templates/{templateId}/items\")"));
        assertTrue(controller.contains("@PutMapping(\"/templates/{templateId}/items\")"));
        assertTrue(controller.contains("@GetMapping(\"/tenants/{tenantId}/templates\")"));
        assertTrue(controller.contains("@PutMapping(\"/tenants/{tenantId}/templates\")"));
        assertTrue(controller.contains("@GetMapping(\"/roles/{roleId}/column-permissions\")"),
                "角色列权限必须提供独立规则查询接口，不能只绑定租户级模板");
        assertTrue(controller.contains("@PutMapping(\"/roles/{roleId}/column-permissions\")"),
                "角色列权限必须提供独立规则保存接口，不能只绑定租户级模板");
        assertTrue(controller.contains("@xuanPermission.has('iam-role-column-permission:view')"),
                "角色列权限查看必须使用独立页面权限码");
        assertTrue(controller.contains("@xuanPermission.has('iam-role-column-permission:update')"),
                "角色列权限保存必须使用独立页面权限码");
        assertTrue(controller.contains("@xuanPermission.hasAny('iam-column-permission:view', 'iam-role-column-permission:view')"),
                "角色列权限页复用的只读模板/字段接口必须允许 iam-role-column-permission:view 访问");
        assertTrue(controller.contains("@xuanPermission.has('iam-column-permission:view')"));
        assertTrue(controller.contains("@xuanPermission.has('iam-column-permission:create')"));
        assertTrue(controller.contains("@xuanPermission.has('iam-column-permission:update')"));
        assertFalse(controller.contains("hasAuthority("), "列权限 Controller 不能直接使用 Spring hasAuthority，避免 * 通配权限失效");
        assertFalse(controller.contains("hasAnyAuthority("), "列权限 Controller 不能直接使用 Spring hasAnyAuthority，避免 * 通配权限失效");
    }

    @Test
    void stateActionPermissionControllerExposesManagementEndpointsWithSwaggerAndPermissions() throws IOException {
        Path controllerPath = CONTROLLER_DIR.resolve("IamStateActionRuleController.java");
        assertTrue(Files.exists(controllerPath), "必须提供 IAM 状态动作权限管理 Controller");

        String controller = Files.readString(controllerPath);

        assertTrue(controller.contains("@Tag"), "状态动作权限 Controller 必须有 Swagger @Tag");
        assertTrue(controller.contains("@Operation"), "状态动作权限 Controller 每个端点必须有 Swagger @Operation");
        assertTrue(controller.contains("@Parameter"), "状态动作权限 Controller 路径和查询参数必须有 Swagger @Parameter");
        assertTrue(controller.contains("@RequestMapping(\"/api/iam/state-action\")"));
        assertTrue(controller.contains("@GetMapping(\"/resources/states\")"));
        assertTrue(controller.contains("@PostMapping(\"/resources/states\")"));
        assertTrue(controller.contains("@PutMapping(\"/resources/states/{stateId}\")"));
        assertTrue(controller.contains("@PostMapping(\"/resources/states/{stateId}/enable\")"));
        assertTrue(controller.contains("@PostMapping(\"/resources/states/{stateId}/disable\")"));
        assertTrue(controller.contains("@GetMapping(\"/resources/actions\")"));
        assertTrue(controller.contains("@PostMapping(\"/resources/actions\")"));
        assertTrue(controller.contains("@PutMapping(\"/resources/actions/{actionId}\")"));
        assertTrue(controller.contains("@PostMapping(\"/resources/actions/{actionId}/enable\")"));
        assertTrue(controller.contains("@PostMapping(\"/resources/actions/{actionId}/disable\")"));
        assertTrue(controller.contains("@GetMapping(\"/roles/{roleId}/rules\")"));
        assertTrue(controller.contains("@PutMapping(\"/roles/{roleId}/rules\")"));
        assertTrue(controller.contains("@xuanPermission.has('iam-state-action:view')"));
        assertTrue(controller.contains("@xuanPermission.has('iam-state-action:create')"));
        assertTrue(controller.contains("@xuanPermission.has('iam-state-action:update')"));
        assertFalse(controller.contains("hasAuthority("), "状态动作权限 Controller 不能直接使用 Spring hasAuthority，避免 * 通配权限失效");
        assertFalse(controller.contains("hasAnyAuthority("), "状态动作权限 Controller 不能直接使用 Spring hasAnyAuthority，避免 * 通配权限失效");
    }

    @Test
    void stateActionPermissionPersistenceIsWiredForManagementAndCurrentSnapshot() throws IOException {
        Path mapperPath = Path.of("src/main/java/com/xuan/erp/iam/infrastructure/persistence/mapper")
                .resolve("IamStateActionRulePersistenceMapper.java");
        Path adapterPath = Path.of("src/main/java/com/xuan/erp/iam/infrastructure/persistence/repository")
                .resolve("IamStateActionRuleRepositoryAdapter.java");
        Path xmlPath = Path.of("src/main/resources/mapper/iam")
                .resolve("IamStateActionRulePersistenceMapper.xml");

        assertTrue(Files.exists(mapperPath), "必须提供状态动作权限 MyBatis Mapper");
        assertTrue(Files.exists(adapterPath), "必须提供状态动作权限仓储 Adapter");
        assertTrue(Files.exists(xmlPath), "必须提供状态动作权限 MyBatis XML");

        String adapter = Files.readString(adapterPath);
        String xml = Files.readString(xmlPath);

        assertTrue(adapter.contains("implements IamStateActionRuleRepository, IamStateActionRuleManagementRepository"),
                "状态动作查询和管理可以共用一个 Adapter，但必须同时实现两个端口");
        assertTrue(xml.contains("id=\"findMergedStateActionRulesByRoleIds\""),
                "当前权限快照必须能按用户角色合并状态动作规则");
        assertTrue(xml.contains("iam_role_state_action_rule"));
        assertTrue(xml.contains("iam_resource_state"));
        assertTrue(xml.contains("iam_resource_action"));
        assertTrue(xml.contains("state.is_enabled = true"));
        assertTrue(xml.contains("action.is_enabled = true"));
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("读取 Controller 源码失败：" + path, exception);
        }
    }
}
