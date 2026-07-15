package com.xuan.erp.iam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamMigrationContractTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void v1CreatesFifteenBaselineTables() throws IOException {
        String v1 = Files.readString(MIGRATION_DIR.resolve("V1__init_iam_database.sql"));

        long tableCount = v1.lines()
                .filter(line -> line.startsWith("CREATE TABLE IF NOT EXISTS "))
                .count();

        assertEquals(15, tableCount);
    }

    @Test
    void v2AddsIamTenantBootstrappedOutboxContract() throws IOException {
        Path v2Path = MIGRATION_DIR.resolve("V2__add_iam_tenant_bootstrapped_outbox.sql");
        assertTrue(Files.exists(v2Path), "必须通过 V2 新增 IAM outbox，不能改写 V1 历史迁移");

        String v2 = Files.readString(v2Path);

        assertTrue(v2.contains("CREATE TABLE IF NOT EXISTS iam_outbox_event"));
        assertTrue(v2.contains("CREATE OR REPLACE FUNCTION bootstrap_iam_tenant"));
        assertTrue(v2.contains("IamTenantBootstrapped"));
        assertTrue(v2.contains("iam.tenant.bootstrapped"));
        assertTrue(v2.contains("ON CONFLICT (event_id) DO NOTHING"));
    }

    @Test
    void v3ExtendsBootstrapWithTenantAdminAndPlatformSuperAdmin() throws IOException {
        Path v3Path = MIGRATION_DIR.resolve("V3__extend_iam_tenant_bootstrap_admin_account.sql");
        assertTrue(Files.exists(v3Path), "必须通过 V3 新增登录账号初始化，不能改写 V1/V2 历史迁移");

        String v3 = Files.readString(v3Path);

        assertTrue(v3.contains("p_admin_username"));
        assertTrue(v3.contains("p_admin_password_hash"));
        assertTrue(v3.contains("'tenant_admin'"));
        assertTrue(v3.contains("'super_admin'"));
        assertTrue(v3.contains("tenant_id, username"));
        assertTrue(v3.contains("iam_authorization_snapshot"));
        assertTrue(v3.contains("PLATFORM_ADMIN"));
        assertTrue(v3.contains("tenant_id = 0"));
    }

    @Test
    void v4AlignsPermissionsWithStandardTemplateAndGrantsSuperAdminAllPermissions() throws IOException {
        Path v4Path = MIGRATION_DIR.resolve("V4__align_iam_permissions_with_standard_template.sql");
        assertTrue(Files.exists(v4Path), "必须通过 V4 追加权限统一迁移，不能改写 V1/V2/V3 历史迁移");

        String v4 = Files.readString(v4Path);

        assertTrue(v4.contains("'product:view'"));
        assertTrue(v4.contains("'product:create'"));
        assertTrue(v4.contains("'product:update'"));
        assertTrue(v4.contains("'product:delete'"));
        assertTrue(v4.contains("'procurement:view'"));
        assertTrue(v4.contains("'procurement:create'"));
        assertTrue(v4.contains("'procurement:update'"));
        assertTrue(v4.contains("'procurement:delete'"));
        assertTrue(v4.contains("jsonb_build_array('*')"));
        assertTrue(v4.contains("super_admin:all-permissions"));
        assertTrue(v4.contains("permission_code = 'product:view'"));
        assertTrue(v4.contains("permission_code = 'procurement:view'"));
        assertFalse(v4.contains("snapshot.deleted_at"), "iam_authorization_snapshot 表没有 deleted_at 字段，V4 不能引用 snapshot.deleted_at");
    }

    @Test
    void v5AddsIamAdminManagementMenusWithoutRewritingHistory() throws IOException {
        Path v5Path = MIGRATION_DIR.resolve("V5__add_iam_admin_management_menus.sql");
        assertTrue(Files.exists(v5Path), "必须通过 V5 追加 IAM 后台管理菜单，不能改写 V1-V4 历史迁移");

        String v5 = Files.readString(v5Path);

        assertTrue(v5.contains("'iam-menu-management'"));
        assertTrue(v5.contains("'iam-permission-management'"));
        assertTrue(v5.contains("'iam-role-management'"));
        assertTrue(v5.contains("/system/iam/menus"));
        assertTrue(v5.contains("/system/iam/permissions"));
        assertTrue(v5.contains("/system/iam/roles"));
        assertTrue(v5.contains("super_admin:iam-admin-management-menus"));
        assertTrue(v5.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
    }

    @Test
    void v6AddsIamUserPreferenceTableWithoutRewritingHistory() throws IOException {
        Path v6Path = MIGRATION_DIR.resolve("V6__add_iam_user_preference.sql");
        assertTrue(Files.exists(v6Path), "必须通过 V6 追加用户偏好表，不能改写 V1-V5 历史迁移");

        String v6 = Files.readString(v6Path);

        assertTrue(v6.contains("CREATE TABLE IF NOT EXISTS iam_user_preference"));
        assertTrue(v6.contains("uk_iam_user_preference_user_key"));
        assertTrue(v6.contains("preference_key"));
        assertTrue(v6.contains("preference_json JSONB"));
        assertTrue(v6.contains("CREATE INDEX IF NOT EXISTS idx_iam_user_preference_user"));
    }

    @Test
    void v9AddsAuditObservabilityMenusAndPermissionsWithoutRewritingHistory() throws IOException {
        Path v9Path = MIGRATION_DIR.resolve("V9__add_audit_observability_menus.sql");
        assertTrue(Files.exists(v9Path), "必须通过 V9 追加审计观测菜单，不能改写 V1-V8 历史迁移");

        String v9 = Files.readString(v9Path);

        assertTrue(v9.contains("'audit:interface-cost:view'"));
        assertTrue(v9.contains("'audit:sql-ranking:view'"));
        assertTrue(v9.contains("'audit-interface-costs'"));
        assertTrue(v9.contains("'audit-sql-rankings'"));
        assertTrue(v9.contains("/system/audit/interface-costs"));
        assertTrue(v9.contains("/system/audit/sql-rankings"));
        assertTrue(v9.contains("super_admin:audit-observability-menus"));
        assertTrue(v9.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
    }

    @Test
    void v10AddsAuditLogQueryMenuWithoutRewritingHistory() throws IOException {
        Path v10Path = MIGRATION_DIR.resolve("V10__add_audit_log_query_menu.sql");
        assertTrue(Files.exists(v10Path), "必须通过 V10 追加审计日志查询菜单，不能改写 V1-V9 历史迁移");

        String v10 = Files.readString(v10Path);

        assertTrue(v10.contains("'audit:log:view'"));
        assertTrue(v10.contains("'audit-logs'"));
        assertTrue(v10.contains("/system/audit/logs"));
        assertTrue(v10.contains("'menu.auditLogs'"));
        assertTrue(v10.contains("'ScrollText'"));
        assertTrue(v10.contains("super_admin:audit-log-query-menu"));
        assertTrue(v10.contains("CREATE OR REPLACE FUNCTION bootstrap_iam_tenant"));
        assertTrue(v10.contains("'audit-logs', 'audit-interface-costs', 'audit-sql-rankings'"));
        assertTrue(v10.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
    }

    @Test
    void v11AddsTenantProvisionCallbackPermissionWithoutRewritingHistory() throws IOException {
        Path v11Path = MIGRATION_DIR.resolve("V11__add_tenant_provision_callback_permission.sql");
        assertTrue(Files.exists(v11Path), "必须通过 V11 追加租户初始化回调权限，不能改写 V1-V10 历史迁移");

        String v11 = Files.readString(v11Path);

        assertTrue(v11.contains("'tenant-provision:callback'"));
        assertTrue(v11.contains("'xuan-tenant'"));
        assertTrue(v11.contains("iam_permission"));
        assertFalse(v11.contains("iam_role_permission"));
        assertFalse(v11.contains("iam_authorization_snapshot"));
        assertFalse(v11.contains("super_admin:tenant-provision-callback"));
    }

    @Test
    void v12AddsTenantInitPermissionTemplatesWithoutRewritingHistory() throws IOException {
        Path v12Path = MIGRATION_DIR.resolve("V12__add_iam_tenant_init_permission_templates.sql");
        assertTrue(Files.exists(v12Path), "必须通过 V12 追加租户初始化权限模板，不能改写 V1-V11 历史迁移");

        String v12 = Files.readString(v12Path);

        assertTrue(v12.contains("CREATE TABLE IF NOT EXISTS iam_tenant_init_permission_template"));
        assertTrue(v12.contains("permission_codes jsonb"));
        assertTrue(v12.contains("'basic'"));
        assertTrue(v12.contains("'standard'"));
        assertTrue(v12.contains("'full'"));
        assertTrue(v12.contains("'iam-init-template-management'"));
        assertTrue(v12.contains("/system/iam/init-templates"));
    }

    @Test
    void v13AddsSystemSettingMenuGroupsWithoutRewritingHistory() throws IOException {
        Path v13Path = MIGRATION_DIR.resolve("V13__add_system_setting_menu_groups.sql");
        assertTrue(Files.exists(v13Path), "必须通过 V13 追加系统设置分层菜单，不能改写 V1-V12 历史迁移");

        String v13 = Files.readString(v13Path);

        assertTrue(v13.contains("'system-permission-center'"));
        assertTrue(v13.contains("'权限中心'"));
        assertTrue(v13.contains("'system-tenant-center'"));
        assertTrue(v13.contains("'租户中心'"));
        assertTrue(v13.contains("'system-audit-center'"));
        assertTrue(v13.contains("'审计中心'"));
        assertTrue(v13.contains("'iam-menu-management', 'system-permission-center', 10"));
        assertTrue(v13.contains("'iam-permission-management', 'system-permission-center', 20"));
        assertTrue(v13.contains("'iam-role-management', 'system-permission-center', 30"));
        assertTrue(v13.contains("'iam-user-management', 'system-permission-center', 40"));
        assertTrue(v13.contains("'tenant-management', 'system-tenant-center', 10"));
        assertTrue(v13.contains("'iam-init-template-management', 'system-tenant-center', 20"));
        assertTrue(v13.contains("'audit-logs', 'system-audit-center', 10"));
        assertTrue(v13.contains("'audit-interface-costs', 'system-audit-center', 20"));
        assertTrue(v13.contains("'audit-sql-rankings', 'system-audit-center', 30"));
        assertTrue(v13.contains("CREATE OR REPLACE FUNCTION bootstrap_iam_tenant"));
    }

    @Test
    void v14AddsTenantPlanManagementMenuWithoutRewritingHistory() throws IOException {
        Path v14Path = MIGRATION_DIR.resolve("V14__add_tenant_plan_management_menu.sql");
        assertTrue(Files.exists(v14Path), "必须通过 V14 追加套餐管理菜单，不能改写 V1-V13 历史迁移");

        String v14 = Files.readString(v14Path);

        assertTrue(v14.contains("'tenant-plan-management'"));
        assertTrue(v14.contains("'套餐管理'"));
        assertTrue(v14.contains("'route.tenantPlans'"));
        assertTrue(v14.contains("/system/tenant-plans"));
        assertTrue(v14.contains("'system-tenant-center'"));
        assertTrue(v14.contains("'tenant:view'"));
        assertTrue(v14.contains("CREATE OR REPLACE FUNCTION bootstrap_iam_tenant"));
    }

    @Test
    void v15RegistersAllMicroservicePermissionsInIamWithoutRewritingHistory() throws IOException {
        Path v15Path = MIGRATION_DIR.resolve("V15__register_all_service_base_permissions.sql");
        assertTrue(Files.exists(v15Path), "必须通过 V15 追加全微服务权限登记，不能改写 V1-V14 历史迁移");

        String v15 = Files.readString(v15Path);

        for (String serviceName : new String[] {
                "xuan-gateway",
                "xuan-iam",
                "xuan-tenant",
                "xuan-audit",
                "xuan-product",
                "xuan-party",
                "xuan-warehouse",
                "xuan-sales",
                "xuan-procurement",
                "xuan-inventory",
                "xuan-manufacturing",
                "xuan-finance",
                "xuan-document",
                "xuan-query"
        }) {
            assertTrue(v15.contains("'" + serviceName + "'"), serviceName + " 必须登记到 iam_permission.service_name");
        }

        assertTrue(v15.contains("'gateway:view'"));
        assertTrue(v15.contains("'gateway-config:manage'"));
        assertTrue(v15.contains("'iam:enable'"));
        assertTrue(v15.contains("'iam:disable'"));
        assertTrue(v15.contains("'tenant:enable'"));
        assertTrue(v15.contains("'tenant:disable'"));
        assertTrue(v15.contains("'product:enable'"));
        assertTrue(v15.contains("'product:disable'"));
        assertTrue(v15.contains("'party:enable'"));
        assertTrue(v15.contains("'party:disable'"));
        assertTrue(v15.contains("'warehouse:enable'"));
        assertTrue(v15.contains("'warehouse:disable'"));
        assertTrue(v15.contains("'inventory:enable'"));
        assertTrue(v15.contains("'inventory:disable'"));
        assertTrue(v15.contains("'sales:enable'"));
        assertTrue(v15.contains("'sales:disable'"));
        assertTrue(v15.contains("'procurement:enable'"));
        assertTrue(v15.contains("'procurement:disable'"));
        assertTrue(v15.contains("'finance:enable'"));
        assertTrue(v15.contains("'finance:disable'"));
        assertTrue(v15.contains("'document:enable'"));
        assertTrue(v15.contains("'document:disable'"));
        assertTrue(v15.contains("'manufacturing:enable'"));
        assertTrue(v15.contains("'manufacturing:disable'"));
        assertTrue(v15.contains("'query:enable'"));
        assertTrue(v15.contains("'query:disable'"));
        assertTrue(v15.contains("iam_role_permission"));
        assertTrue(v15.contains("iam_tenant_init_permission_template"));
        assertTrue(v15.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
        assertFalse(v15.contains("CREATE TABLE"), "V15 只登记权限初始化数据，不应新增表结构");
    }

    @Test
    void v16AddsTenantPlanPermissionsWithoutRewritingHistory() throws IOException {
        Path v16Path = MIGRATION_DIR.resolve("V16__add_tenant_plan_permissions.sql");
        assertTrue(Files.exists(v16Path), "必须通过 V16 追加租户套餐权限，不能改写 V1-V15 历史迁移");

        String v16 = Files.readString(v16Path);

        assertTrue(v16.contains("'tenant-plan:view'"));
        assertTrue(v16.contains("'tenant-plan:manage'"));
        assertTrue(v16.contains("'tenant-plan:assign'"));
        assertTrue(v16.contains("'tenant-plan-management'"));
        assertTrue(v16.contains("iam_role_permission"));
        assertTrue(v16.contains("role.code IN ('tenant_admin', 'super_admin')"));
        assertTrue(v16.contains("iam_tenant_init_permission_template"));
        assertTrue(v16.contains("super_admin:tenant-plan-permissions"));
        assertTrue(v16.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
        assertFalse(v16.contains("CREATE TABLE"), "V16 只补权限初始化数据，不应新增表结构");
    }

    @Test
    void v17SupportsSuperadminAliasWithoutRewritingHistory() throws IOException {
        Path v17Path = MIGRATION_DIR.resolve("V17__support_superadmin_alias_for_platform_admin.sql");
        assertTrue(Files.exists(v17Path), "必须通过 V17 追加 superadmin 兼容迁移，不能改写 V1-V16 历史迁移");

        String v17 = Files.readString(v17Path);

        assertTrue(v17.contains("'super_admin', 'superadmin'"));
        assertTrue(v17.contains("username IN ('super_admin', 'superadmin')"));
        assertTrue(v17.contains("jsonb_build_array('*')"));
        assertTrue(v17.contains("super_admin:alias-platform-admin"));
        assertTrue(v17.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
        assertFalse(v17.contains("CREATE TABLE"), "V17 只修正平台管理员初始化数据，不应新增表结构");
    }
}
