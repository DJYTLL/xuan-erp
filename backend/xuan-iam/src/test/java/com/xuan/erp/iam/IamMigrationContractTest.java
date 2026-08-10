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

    @Test
    void v18RetiresTenantLifecyclePermissionWithoutRewritingHistory() throws IOException {
        Path v18Path = MIGRATION_DIR.resolve("V18__retire_tenant_lifecycle_permission.sql");
        assertTrue(Files.exists(v18Path), "必须通过 V18 退役 tenant:lifecycle，不能改写 V1-V17 历史迁移");

        String v18 = Files.readString(v18Path);

        assertTrue(v18.contains("'tenant:lifecycle'"));
        assertTrue(v18.contains("'tenant:enable'"));
        assertTrue(v18.contains("'tenant:disable'"));
        assertTrue(v18.contains("is_enabled = false"));
        assertTrue(v18.contains("iam_role_permission"));
        assertTrue(v18.contains("iam_tenant_init_permission_template"));
        assertTrue(v18.contains("jsonb_agg"));
        assertTrue(v18.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
        assertFalse(v18.contains("CREATE TABLE"), "V18 只调整权限初始化数据，不应新增表结构");
    }

    @Test
    void v19DrivesTenantBootstrapByInitTemplateWithoutRewritingHistory() throws IOException {
        Path v19Path = MIGRATION_DIR.resolve("V19__drive_bootstrap_by_tenant_init_template.sql");
        assertTrue(Files.exists(v19Path), "必须通过 V19 让套餐初始化模板真正驱动 IAM 初始化，不能改写 V1-V18 历史迁移");

        String v19 = Files.readString(v19Path);

        assertTrue(v19.contains("p_iam_init_template_code"));
        assertTrue(v19.contains("iam_tenant_init_permission_template"));
        assertTrue(v19.contains("jsonb_array_elements_text"));
        assertTrue(v19.contains("selected_permission"));
        assertTrue(v19.contains("JOIN iam_permission permission ON permission.code = selected_permission.code"));
        assertTrue(v19.contains("'iamInitTemplateCode', v_template_code"));
        assertTrue(v19.contains("CREATE OR REPLACE FUNCTION bootstrap_iam_tenant("));
        assertTrue(v19.contains("bigint, varchar, varchar, varchar, varchar, varchar, varchar, varchar"));
        assertFalse(v19.contains("CREATE TABLE"), "V19 只替换初始化函数，不应新增表结构");
    }

    @Test
    void v20SynchronizesTenantAdminPermissionsToInitTemplateWithoutRewritingHistory() throws IOException {
        Path v20Path = MIGRATION_DIR.resolve("V20__sync_tenant_admin_permissions_by_init_template.sql");
        assertTrue(Files.exists(v20Path), "必须通过 V20 让初始化模板变更同步收敛租户权限，不能改写 V1-V19 历史迁移");

        String v20 = Files.readString(v20Path);

        assertTrue(v20.contains("CREATE OR REPLACE FUNCTION bootstrap_iam_tenant("));
        assertTrue(v20.contains("p_iam_init_template_code"));
        assertTrue(v20.contains("selected_permission"));
        assertTrue(v20.contains("UPDATE iam_role_permission"));
        assertTrue(v20.contains("delete_reason = 'sync by iam init template ' || v_template_code"));
        assertTrue(v20.contains("UPDATE iam_tenant_menu"));
        assertTrue(v20.contains("menu_tree"));
        assertTrue(v20.contains("NOT EXISTS"));
        assertTrue(v20.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
        assertFalse(v20.contains("CREATE TABLE"), "V20 只替换初始化函数，不应新增表结构");
    }

    @Test
    void v21AddsTenantInitRoleTemplatesWithoutRewritingHistory() throws IOException {
        Path v21Path = MIGRATION_DIR.resolve("V21__add_tenant_init_role_templates.sql");
        assertTrue(Files.exists(v21Path), "必须通过 V21 新增租户初始化角色模板，不能改写 V1-V20 历史迁移");

        String v21 = Files.readString(v21Path);

        assertTrue(v21.contains("CREATE TABLE IF NOT EXISTS iam_tenant_init_role_template"));
        assertTrue(v21.contains("init_template_code"));
        assertTrue(v21.contains("role_code"));
        assertTrue(v21.contains("permission_codes jsonb"));
        assertTrue(v21.contains("assign_to_admin"));
        assertTrue(v21.contains("'basic', 'tenant_readonly'"));
        assertTrue(v21.contains("'standard', 'tenant_admin'"));
        assertTrue(v21.contains("'full', 'tenant_owner'"));
        assertTrue(v21.contains("'full', 'tenant_admin'"));
        assertTrue(v21.contains("role_template.assign_to_admin = true"));
        assertTrue(v21.contains("template_permission"));
        assertTrue(v21.contains("UPDATE iam_user_role"));
        assertTrue(v21.contains("delete_reason = 'sync by iam init role template ' || v_template_code"));
        assertTrue(v21.contains("CREATE OR REPLACE FUNCTION bootstrap_iam_tenant("));
        assertTrue(v21.contains("ON CONFLICT (tenant_id, user_id) DO UPDATE"));
    }

    @Test
    void v22RetiresUnimplementedNavigationMenusWithoutRewritingHistory() throws IOException {
        Path v22Path = MIGRATION_DIR.resolve("V22__retire_unimplemented_navigation_menus.sql");
        assertTrue(Files.exists(v22Path), "必须通过 V22 退役未落地页面的旧占位菜单，不能改写 V1-V21 历史迁移");

        String v22 = Files.readString(v22Path);

        assertTrue(v22.contains("'party'"));
        assertTrue(v22.contains("'warehouse'"));
        assertTrue(v22.contains("'inventory'"));
        assertTrue(v22.contains("'sales'"));
        assertTrue(v22.contains("'finance'"));
        assertTrue(v22.contains("'document'"));
        assertTrue(v22.contains("'manufacturing'"));
        assertTrue(v22.contains("'report'"));
        assertTrue(v22.contains("is_enabled = false"));
        assertTrue(v22.contains("iam_tenant_menu"));
        assertTrue(v22.contains("iam_authorization_snapshot"));
        assertTrue(v22.contains("jsonb_array_elements_text"));
        assertTrue(v22.contains("super_admin:retire-unimplemented-navigation-menus"));
        assertFalse(v22.contains("CREATE TABLE"), "V22 只调整菜单初始化数据，不应新增表结构");
    }

    @Test
    void v23RepairsNavigationGroupParentLinksWithoutRewritingHistory() throws IOException {
        Path v23Path = MIGRATION_DIR.resolve("V23__repair_navigation_group_parent_links.sql");
        assertTrue(Files.exists(v23Path), "必须通过 V23 修复导航分组父级关系，不能改写 V1-V22 历史迁移");

        String v23 = Files.readString(v23Path);

        assertTrue(v23.contains("'base-data', 'inventory-root'"));
        assertTrue(v23.contains("'stock-management', 'inventory-root'"));
        assertTrue(v23.contains("'purchase-management', 'inventory-root'"));
        assertTrue(v23.contains("'sales-management', 'inventory-root'"));
        assertTrue(v23.contains("UPDATE iam_menu child"));
        assertTrue(v23.contains("SET parent_id = parent.id"));
        assertTrue(v23.contains("parent_id IS DISTINCT FROM parent.id"));
        assertTrue(v23.contains("iam_authorization_snapshot"));
        assertTrue(v23.contains("'inventory-root'"));
        assertTrue(v23.contains("navigation-group-parent-links-v23"));
        assertFalse(v23.contains("CREATE TABLE"), "V23 只修复菜单父级和快照数据，不应新增表结构");
    }

    @Test
    void v24RestoresFullNavigationMenusWithoutRewritingHistory() throws IOException {
        Path v24Path = MIGRATION_DIR.resolve("V24__restore_full_navigation_menus.sql");
        assertTrue(Files.exists(v24Path), "必须通过 V24 恢复完整导航菜单，不能改写 V1-V23 历史迁移");

        String v24 = Files.readString(v24Path);

        assertTrue(v24.contains("'party', 'base-data'"));
        assertTrue(v24.contains("'warehouse', 'base-data'"));
        assertTrue(v24.contains("'inventory', 'stock-management'"));
        assertTrue(v24.contains("'manufacturing', 'stock-management'"));
        assertTrue(v24.contains("'sales', 'sales-management'"));
        assertTrue(v24.contains("'finance', NULL::varchar"));
        assertTrue(v24.contains("'document', NULL::varchar"));
        assertTrue(v24.contains("'report', NULL::varchar"));
        assertTrue(v24.contains("is_enabled = true"));
        assertTrue(v24.contains("UPDATE iam_menu menu"));
        assertTrue(v24.contains("iam_authorization_snapshot"));
        assertTrue(v24.contains("jsonb_array_elements_text"));
        assertTrue(v24.contains("restore-full-navigation-menus-v24"));
        assertFalse(v24.contains("CREATE TABLE"), "V24 只恢复菜单初始化数据，不应新增表结构");
    }

    @Test
    void v25AddsTenantPermissionEntitlementsWithoutRewritingHistory() throws IOException {
        Path v25Path = MIGRATION_DIR.resolve("V25__add_tenant_permission_entitlements.sql");
        assertTrue(Files.exists(v25Path), "必须通过 V25 新增租户权限池，不能改写 V1-V24 历史迁移");

        String v25 = Files.readString(v25Path);

        assertTrue(v25.contains("CREATE TABLE IF NOT EXISTS iam_tenant_permission_entitlement"));
        assertTrue(v25.contains("tenant_id bigint NOT NULL"));
        assertTrue(v25.contains("init_template_code varchar(64)"));
        assertTrue(v25.contains("permission_id bigint NOT NULL"));
        assertTrue(v25.contains("entitlement_version bigint"));
        assertTrue(v25.contains("uk_iam_tenant_permission_entitlement_active"));
        assertTrue(v25.contains("bootstrap_iam_tenant"));
        assertTrue(v25.contains("iam_role_permission"));
        assertTrue(v25.contains("delete_reason = 'tenant permission entitlement reduced'"));
        assertTrue(v25.contains("iam_authorization_snapshot"));
        assertTrue(v25.contains("jsonb_array_elements_text"));
    }

    @Test
    void v26AddsStableTenantInitTemplateBindingWithoutRewritingHistory() throws IOException {
        Path v26Path = MIGRATION_DIR.resolve("V26__add_tenant_init_template_binding.sql");
        assertTrue(Files.exists(v26Path), "必须通过 V26 新增租户初始化模板绑定，不能改写 V1-V25 历史迁移");

        String v26 = Files.readString(v26Path);

        assertTrue(v26.contains("CREATE TABLE IF NOT EXISTS iam_tenant_init_template_binding"));
        assertTrue(v26.contains("tenant_id bigint NOT NULL"));
        assertTrue(v26.contains("init_template_code varchar(64) NOT NULL"));
        assertTrue(v26.contains("last_entitlement_version bigint"));
        assertTrue(v26.contains("uk_iam_tenant_init_template_binding_tenant_active"));
        assertTrue(v26.contains("idx_iam_tenant_init_template_binding_template"));
        assertTrue(v26.contains("iam_outbox_event"));
        assertTrue(v26.contains("iam_tenant_permission_entitlement"));
        assertTrue(v26.contains("payload ->> 'iamInitTemplateCode'"));
        assertTrue(v26.contains("ON CONFLICT"));
        assertTrue(v26.contains("bootstrap_iam_tenant"));
    }

    @Test
    void v27RebuildsTenantEntitlementsFromTemplateBindingAndPrunesStaleGrants() throws IOException {
        Path v27Path = MIGRATION_DIR.resolve("V27__rebuild_tenant_entitlements_from_template_binding.sql");
        assertTrue(Files.exists(v27Path), "必须通过 V27 修复历史权限池污染，不能改写 V25/V26 历史迁移");

        String v27 = Files.readString(v27Path);

        assertTrue(v27.contains("iam_tenant_init_template_binding"));
        assertTrue(v27.contains("iam_tenant_init_permission_template"));
        assertTrue(v27.contains("iam_tenant_permission_entitlement"));
        assertTrue(v27.contains("jsonb_array_elements_text"));
        assertTrue(v27.contains("UPDATE iam_tenant_permission_entitlement"));
        assertTrue(v27.contains("INSERT INTO iam_tenant_permission_entitlement"));
        assertTrue(v27.contains("UPDATE iam_role_permission"));
        assertTrue(v27.contains("delete_reason = 'tenant permission entitlement reduced'"));
        assertTrue(v27.contains("iam_authorization_snapshot"));
        assertFalse(v27.contains("role_permission_seed"), "V27 不能再把已有角色权限反向写回租户权限池");
    }

    @Test
    void v28BackfillsMissingTemplateBindingsFromTenantPlanWhenTablesAreCoLocated() throws IOException {
        Path v28Path = MIGRATION_DIR.resolve("V28__backfill_missing_template_bindings_from_tenant_plan.sql");
        assertTrue(Files.exists(v28Path), "必须通过 V28 修复缺失模板绑定的历史租户，不能改写 V26/V27 历史迁移");

        String v28 = Files.readString(v28Path);

        assertTrue(v28.contains("to_regclass('tenant_plan_assignment')"));
        assertTrue(v28.contains("to_regclass('tenant_plan')"));
        assertTrue(v28.contains("feature_flags ->> 'iamInitTemplateCode'"));
        assertTrue(v28.contains("iam_tenant_init_template_binding"));
        assertTrue(v28.contains("created_by, updated_by"));
        assertTrue(v28.contains("'system-v28'"));
        assertTrue(v28.contains("iam_tenant_permission_entitlement"));
        assertTrue(v28.contains("UPDATE iam_role_permission"));
        assertTrue(v28.contains("delete_reason = 'tenant permission entitlement reduced'"));
        assertTrue(v28.contains("iam_authorization_snapshot"));
    }

    @Test
    void v29SynchronizesTenantAdminRoleWithTenantEntitlementPoolWithoutRewritingHistory() throws IOException {
        Path v29Path = MIGRATION_DIR.resolve("V29__sync_admin_role_permissions_with_tenant_entitlements.sql");
        assertTrue(Files.exists(v29Path), "必须通过 V29 追加租户管理员角色同步逻辑，不能改写 V25-V28 历史迁移");

        String v29 = Files.readString(v29Path);

        assertTrue(v29.contains("sync_tenant_admin_role_permissions_with_entitlements"));
        assertTrue(v29.contains("'tenant_admin'"));
        assertTrue(v29.contains("iam_user_tenant"));
        assertTrue(v29.contains("is_tenant_admin = true"));
        assertTrue(v29.contains("membership_status = 'ACTIVE'"));
        assertTrue(v29.contains("iam_tenant_permission_entitlement"));
        assertTrue(v29.contains("tenant admin sync by tenant permission entitlement"));
        assertTrue(v29.contains("tenant permission entitlement reduced"));
        assertTrue(v29.contains("iam_authorization_snapshot"));
        assertTrue(v29.contains("trg_sync_tenant_admin_role_permissions_after_bootstrap_task"));
        assertTrue(v29.contains("iam_tenant_bootstrap_task"));
        assertTrue(v29.contains("system-v29"));
        assertFalse(v29.contains("CREATE TABLE"), "V29 只新增同步函数/触发器和历史数据修复，不应新增业务表");
    }

    @Test
    void v30WidensAuthorizationSnapshotHashWithoutRewritingHistory() throws IOException {
        Path v30Path = MIGRATION_DIR.resolve("V30__widen_iam_authorization_snapshot_hash.sql");
        assertTrue(Files.exists(v30Path), "必须通过 V30 放宽授权快照哈希字段，不能改写 V1-V29 历史迁移");

        String v30 = Files.readString(v30Path);

        assertTrue(v30.contains("ALTER TABLE iam_authorization_snapshot"));
        assertTrue(v30.contains("ALTER COLUMN snapshot_hash TYPE varchar(256)"));
        assertFalse(v30.contains("CREATE TABLE"), "V30 只调整快照哈希字段长度，不应新增业务表");
    }

    @Test
    void v31SplitsIamManagementPagePermissionsWithoutRewritingHistory() throws IOException {
        Path v31Path = MIGRATION_DIR.resolve("V31__split_iam_management_page_permissions.sql");
        assertTrue(Files.exists(v31Path), "必须通过 V31 拆分 IAM 管理页面权限，不能改写 V1-V30 历史迁移");

        String v31 = Files.readString(v31Path);

        for (String code : new String[] {
                "component-center:view",
                "iam-menu:view", "iam-menu:create", "iam-menu:update",
                "iam-permission:view", "iam-permission:create", "iam-permission:update",
                "iam-role:view", "iam-role:create", "iam-role:update",
                "iam-user:view", "iam-user:create", "iam-user:update", "iam-user:delete",
                "iam-init-template:view", "iam-init-template:create", "iam-init-template:update"
        }) {
            assertTrue(v31.contains("'" + code + "'"), code + " 必须在 V31 中登记和迁移");
        }
        assertTrue(v31.contains("UPDATE iam_menu menu"));
        assertTrue(v31.contains("WHERE code = 'system'"));
        assertTrue(v31.contains("permission_code = NULL"));
        assertTrue(v31.contains("iam_role_permission"));
        assertTrue(v31.contains("iam_tenant_permission_entitlement"));
        assertTrue(v31.contains("iam_tenant_init_permission_template"));
        assertTrue(v31.contains("iam_tenant_init_role_template"));
        assertTrue(v31.contains("iam_authorization_snapshot"));
        assertFalse(v31.contains("CREATE TABLE"), "V31 只新增权限初始化数据和历史授权迁移，不应新增业务表");
    }

    @Test
    void v32AddsIamUserResetPasswordPermissionWithoutRewritingHistory() throws IOException {
        Path v32Path = MIGRATION_DIR.resolve("V32__add_iam_user_reset_password_permission.sql");
        assertTrue(Files.exists(v32Path), "必须通过 V32 追加用户重置密码权限，不能改写 V1-V31 历史迁移");

        String v32 = Files.readString(v32Path);

        assertTrue(v32.contains("'iam-user:reset-password'"));
        assertTrue(v32.contains("'iam-user-management'"));
        assertTrue(v32.contains("iam_role_permission"));
        assertTrue(v32.contains("iam_tenant_permission_entitlement"));
        assertTrue(v32.contains("iam_tenant_init_permission_template"));
        assertTrue(v32.contains("iam_tenant_init_role_template"));
        assertTrue(v32.contains("iam_authorization_snapshot"));
        assertFalse(v32.contains("CREATE TABLE"), "V32 只新增权限初始化数据，不应新增业务表");
    }

    @Test
    void v34AddsColumnPermissionManagementPageWithoutRewritingHistory() throws IOException {
        Path v34Path = MIGRATION_DIR.resolve("V34__add_column_permission_management_page.sql");
        assertTrue(Files.exists(v34Path), "必须通过 V34 追加列权限管理页面和权限链，不能改写 V33 历史迁移");

        String v34 = Files.readString(v34Path);

        assertTrue(v34.contains("'iam-column-permission:view'"));
        assertTrue(v34.contains("'iam-column-permission:create'"));
        assertTrue(v34.contains("'iam-column-permission:update'"));
        assertTrue(v34.contains("'iam-column-permission-management'"));
        assertTrue(v34.contains("/system/iam/column-permissions"));
        assertTrue(v34.contains("'system-permission-center'"));
        assertTrue(v34.contains("iam_role_permission"));
        assertTrue(v34.contains("iam_tenant_permission_entitlement"));
        assertTrue(v34.contains("iam_tenant_init_permission_template"));
        assertTrue(v34.contains("iam_tenant_init_role_template"));
        assertTrue(v34.contains("iam_authorization_snapshot"));
        assertFalse(v34.contains("CREATE TABLE"), "V34 只新增管理权限、菜单和授权数据，不应新增业务表");
    }

    @Test
    void v36AddsTenantColumnPermissionTemplateAssignmentsWithoutRewritingHistory() throws IOException {
        Path v36Path = MIGRATION_DIR.resolve("V36__add_tenant_column_permission_template_assignments.sql");
        assertTrue(Files.exists(v36Path), "必须通过 V36 追加租户列权限模板分配表，不能改写 V33-V35 历史迁移");

        String v36 = Files.readString(v36Path);

        assertTrue(v36.contains("CREATE TABLE IF NOT EXISTS iam_tenant_column_permission_template_assignment"));
        assertTrue(v36.contains("tenant_id bigint NOT NULL"));
        assertTrue(v36.contains("template_id bigint NOT NULL"));
        assertTrue(v36.contains("is_default boolean DEFAULT false NOT NULL"));
        assertTrue(v36.contains("idx_iam_tenant_column_template_assignment_active"));
        assertTrue(v36.contains("idx_iam_tenant_column_template_assignment_default"));
        assertTrue(v36.contains("租户可用列权限模板分配表"));
        assertTrue(v36.contains("iam_role"));
        assertTrue(v36.contains("iam_column_permission_template"));
        assertFalse(v36.contains("DROP TABLE"), "V36 只能追加租户模板分配能力，不能删除历史表");
    }

    @Test
    void v37AddsRoleColumnPermissionRulesAndStandalonePageWithoutRewritingHistory() throws IOException {
        Path v37Path = MIGRATION_DIR.resolve("V37__add_role_column_permission_rules.sql");
        assertTrue(Files.exists(v37Path), "必须通过 V37 追加角色列权限规则表和独立页面，不能改写 V33-V36 历史迁移");

        String v37 = Files.readString(v37Path);

        assertTrue(v37.contains("CREATE TABLE IF NOT EXISTS iam_role_column_permission_rule"));
        assertTrue(v37.contains("tenant_id bigint NOT NULL"));
        assertTrue(v37.contains("role_id bigint NOT NULL"));
        assertTrue(v37.contains("resource_column_id bigint NOT NULL"));
        assertTrue(v37.contains("access_mode varchar(30) NOT NULL"));
        assertTrue(v37.contains("CHECK (access_mode IN ('VISIBLE', 'MASKED', 'HIDDEN'))"));
        assertTrue(v37.contains("idx_iam_role_column_permission_rule_active"));
        assertTrue(v37.contains("角色列权限规则表"));
        assertTrue(v37.contains("'iam-role-column-permission:view'"));
        assertTrue(v37.contains("'iam-role-column-permission:update'"));
        assertTrue(v37.contains("'iam-role-column-permission-management'"));
        assertTrue(v37.contains("/system/iam/role-column-permissions"));
        assertTrue(v37.contains("'system-permission-center'"));
        assertTrue(v37.contains("iam_role_column_permission_template_binding"));
        assertTrue(v37.contains("iam_tenant_column_permission_template_assignment"));
        assertTrue(v37.contains("iam_authorization_snapshot"));
        assertFalse(v37.contains("DROP TABLE"), "V37 只能追加角色规则能力，不能删除历史表");
    }

    @Test
    void v38GrantsRoleColumnPermissionToSuperAdminWithoutRewritingHistory() throws IOException {
        Path v38Path = MIGRATION_DIR.resolve("V38__grant_role_column_permission_to_super_admin.sql");
        assertTrue(Files.exists(v38Path), "必须通过 V38 追加修复 super_admin 角色列权限授权，不能改写 V37 或更早迁移");

        String v38 = Files.readString(v38Path);

        assertTrue(v38.contains("'super_admin'"));
        assertTrue(v38.contains("'iam-role-column-permission:view'"));
        assertTrue(v38.contains("'iam-role-column-permission:update'"));
        assertTrue(v38.contains("iam_role_permission"));
        assertTrue(v38.contains("iam_authorization_snapshot"));
        assertTrue(v38.contains("iam_user_role"));
        assertTrue(v38.contains("'iam-role-column-permission-management'"));
        assertFalse(v38.contains("DROP TABLE"), "V38 只能补授权数据和快照，不能删除历史表");
    }

    @Test
    void v39RegistersIamUserColumnPermissionResourcesWithoutRewritingHistory() throws IOException {
        Path v39Path = MIGRATION_DIR.resolve("V39__add_iam_user_column_permission_resources.sql");
        assertTrue(Files.exists(v39Path), "必须通过 V39 追加用户授权页列权限字段资源，不能改写 V38 或更早迁移");

        String v39 = Files.readString(v39Path);

        assertTrue(v39.contains("INSERT INTO iam_resource_column"));
        assertTrue(v39.contains("'iam-user'"));
        assertTrue(v39.contains("'username'"));
        assertTrue(v39.contains("'displayName'"));
        assertTrue(v39.contains("'phone'"));
        assertTrue(v39.contains("'PHONE'"));
        assertTrue(v39.contains("'email'"));
        assertTrue(v39.contains("'authVersion'"));
        assertTrue(v39.contains("'status'"));
        assertTrue(v39.contains("WHERE NOT EXISTS"));
        assertFalse(v39.contains("DROP TABLE"), "V39 只能补字段资源种子，不能删除历史表");
    }

    @Test
    void v40RegistersTenantProvisionedAtColumnPermissionResourceWithoutRewritingHistory() throws IOException {
        Path v40Path = MIGRATION_DIR.resolve("V40__add_tenant_provisioned_column_permission_resource.sql");
        assertTrue(Files.exists(v40Path), "必须通过 V40 追加租户管理开通完成字段资源，不能改写 V39 或更早迁移");

        String v40 = Files.readString(v40Path);

        assertTrue(v40.contains("INSERT INTO iam_resource_column"));
        assertTrue(v40.contains("'tenant'"));
        assertTrue(v40.contains("'provisionedAt'"));
        assertTrue(v40.contains("'开通完成'"));
        assertTrue(v40.contains("'DATETIME'"));
        assertTrue(v40.contains("'tenant_admin_default', 'provisionedAt', 'VISIBLE'"));
        assertTrue(v40.contains("'tenant_readonly_masked', 'provisionedAt', 'VISIBLE'"));
        assertTrue(v40.contains("WHERE NOT EXISTS"));
        assertTrue(v40.contains("iam_column_permission_template_item"));
        assertFalse(v40.contains("DROP TABLE"), "V40 只能补字段资源和模板明细，不能删除历史表");
    }

    @Test
    void v41RepairsCodexTenantViewerGarbledTextWithoutRewritingHistory() throws IOException {
        Path v41Path = MIGRATION_DIR.resolve("V41__repair_codex_tenant_viewer_garbled_text.sql");
        assertTrue(Files.exists(v41Path), "必须通过 V41 修复 codex_tenant_viewer 乱码数据，不能改写 V40 或更早迁移");

        String v41 = Files.readString(v41Path);

        assertTrue(v41.contains("UPDATE iam_role"));
        assertTrue(v41.contains("UPDATE iam_user"));
        assertTrue(v41.contains("'codex_tenant_viewer'"));
        assertTrue(v41.contains("'Codex 租户查看员'"));
        assertTrue(v41.contains("'Codex 租户查看员，默认拥有 tenant:view 权限'"));
        assertTrue(v41.contains("'Codex 租户查看员备注'"));
        assertTrue(v41.contains("position('?' in coalesce(name, '')) > 0"));
        assertTrue(v41.contains("position('?' in coalesce(display_name, '')) > 0"));
        assertFalse(v41.contains("DROP TABLE"), "V41 只能修复历史坏数据，不能删除历史表");
    }

    @Test
    void v42AddsTenantAdminPasswordResetPermissionAndRenamesUserManagementWithoutRewritingHistory() throws IOException {
        Path v42Path = MIGRATION_DIR.resolve("V42__add_tenant_admin_password_reset_permission.sql");
        assertTrue(Files.exists(v42Path), "必须通过 V42 追加租户管理员密码重置权限，不能改写 V41 或更早迁移");

        String v42 = Files.readString(v42Path);

        assertTrue(v42.contains("'tenant:admin-password:reset'"));
        assertTrue(v42.contains("'tenant-management'"));
        assertTrue(v42.contains("UPDATE iam_menu"));
        assertTrue(v42.contains("title = '用户管理'"));
        assertTrue(v42.contains("REPLACE(name, '用户授权', '用户管理')"));
        assertTrue(v42.contains("iam_role_permission"));
        assertTrue(v42.contains("iam_tenant_permission_entitlement"));
        assertTrue(v42.contains("iam_tenant_init_permission_template"));
        assertTrue(v42.contains("iam_tenant_init_role_template"));
        assertTrue(v42.contains("iam_authorization_snapshot"));
        assertFalse(v42.contains("CREATE TABLE"), "V42 只新增权限初始化数据和文案修正，不应新增业务表");
        assertFalse(v42.contains("DROP TABLE"), "V42 不能删除历史表");
    }

    @Test
    void v44AddsTenantPermissionSyncHashStateWithoutRewritingHistory() throws IOException {
        Path v44Path = MIGRATION_DIR.resolve("V44__add_tenant_permission_sync_hash.sql");
        assertTrue(Files.exists(v44Path), "必须通过 V44 追加租户权限同步 hash 状态，不能改写 V1-V43 历史迁移");

        String v44 = Files.readString(v44Path);

        assertTrue(v44.contains("CREATE TABLE IF NOT EXISTS iam_tenant_permission_sync_state"));
        assertTrue(v44.contains("tenant_id bigint NOT NULL"));
        assertTrue(v44.contains("last_synced_permission_hash varchar(128)"));
        assertTrue(v44.contains("last_synced_at timestamptz"));
        assertTrue(v44.contains("last_sync_source varchar(64)"));
        assertTrue(v44.contains("last_error_code varchar(64)"));
        assertTrue(v44.contains("last_error_message varchar(500)"));
        assertTrue(v44.contains("uk_iam_tenant_permission_sync_state_tenant"));
        assertTrue(v44.contains("idx_iam_tenant_permission_sync_state_hash"));
        assertFalse(v44.contains("DROP TABLE"), "V44 只能追加同步状态表，不能删除历史表");
    }

    @Test
    void v45BackfillsRoleColumnPermissionPageDependenciesWithoutRewritingHistory() throws IOException {
        Path v45Path = MIGRATION_DIR.resolve("V45__backfill_role_column_permission_page_dependencies.sql");
        assertTrue(Files.exists(v45Path), "必须通过 V45 回填角色列权限页面运行依赖权限，不能改写 V1-V44 历史迁移");

        String v45 = Files.readString(v45Path);

        assertTrue(v45.contains("'iam-role-column-permission:view'"));
        assertTrue(v45.contains("'iam-role-column-permission:update'"));
        assertTrue(v45.contains("'iam-role:view'"));
        assertTrue(v45.contains("'iam-column-permission:view'"));
        assertTrue(v45.contains("iam_role_permission"));
        assertTrue(v45.contains("iam_tenant_permission_entitlement"));
        assertTrue(v45.contains("MIN(entitlement.init_template_code)"));
        assertTrue(v45.contains("GROUP BY entitlement.tenant_id, dependency.id"));
        assertTrue(v45.contains("iam_tenant_init_permission_template"));
        assertTrue(v45.contains("iam_tenant_init_role_template"));
        assertTrue(v45.contains("iam_authorization_snapshot"));
        assertTrue(v45.contains("auth_version = snapshot.auth_version + 1"));
        assertTrue(v45.contains("role-column-dependencies-v45"));
        assertFalse(v45.contains("CREATE TABLE"), "V45 只回填权限依赖数据，不应新增业务表");
        assertFalse(v45.contains("DROP TABLE"), "V45 不能删除历史表");
    }

    @Test
    void v46RegistersTenantPermissionSyncStatusColumnWithoutRewritingHistory() throws IOException {
        Path v46Path = MIGRATION_DIR.resolve("V46__add_tenant_permission_sync_status_column_resource.sql");
        assertTrue(Files.exists(v46Path), "必须通过 V46 追加 tenant 权限同步状态字段资源，不能改写 V1-V45 历史迁移");

        String v46 = Files.readString(v46Path);

        assertTrue(v46.contains("INSERT INTO iam_resource_column"));
        assertTrue(v46.contains("'tenant'"));
        assertTrue(v46.contains("'permissionSyncStatus'"));
        assertTrue(v46.contains("'权限同步状态'"));
        assertTrue(v46.contains("'ENUM'"));
        assertTrue(v46.contains("'tenant_admin_default', 'permissionSyncStatus', 'VISIBLE'"));
        assertTrue(v46.contains("'tenant_readonly_masked', 'permissionSyncStatus', 'VISIBLE'"));
        assertTrue(v46.contains("WHERE NOT EXISTS"));
        assertTrue(v46.contains("iam_column_permission_template_item"));
        assertFalse(v46.contains("CREATE TABLE"), "V46 只补 tenant 权限同步状态字段资源，不应新增业务表");
        assertFalse(v46.contains("DROP TABLE"), "V46 不能删除历史表");
    }

    @Test
    void v47RegistersTenantDomainAndContactPermissionsWithoutRewritingHistory() throws IOException {
        Path v47Path = MIGRATION_DIR.resolve("V47__add_tenant_resource_permissions.sql");
        assertTrue(Files.exists(v47Path), "必须通过 V47 追加租户域名和联系人权限归属，不能改写 V1-V46 历史迁移");

        String v47 = Files.readString(v47Path);

        assertTrue(v47.contains("'tenant-domain:view'"));
        assertTrue(v47.contains("'tenant-domain:manage'"));
        assertTrue(v47.contains("'tenant-contact:view'"));
        assertTrue(v47.contains("'tenant-contact:manage'"));
        assertTrue(v47.contains("iam_permission"));
        assertTrue(v47.contains("iam_role_permission"));
        assertTrue(v47.contains("iam_tenant_permission_entitlement"));
        assertTrue(v47.contains("iam_tenant_init_permission_template"));
        assertTrue(v47.contains("iam_tenant_init_role_template"));
        assertTrue(v47.contains("iam_authorization_snapshot"));
        assertTrue(v47.contains("'tenant-management'"));
        assertFalse(v47.contains("CREATE TABLE"), "V47 只补权限归属与回填数据，不应新增业务表");
        assertFalse(v47.contains("DROP TABLE"), "V47 不能删除历史表");
    }

    @Test
    void v48AddsStateActionPermissionRulesWithoutRewritingHistory() throws IOException {
        Path v48Path = MIGRATION_DIR.resolve("V48__add_state_action_permission_rules.sql");
        assertTrue(Files.exists(v48Path), "必须通过 V48 追加状态动作权限规则，不能改写 V1-V47 历史迁移");

        String v48 = Files.readString(v48Path);

        assertTrue(v48.contains("CREATE TABLE IF NOT EXISTS iam_resource_state"));
        assertTrue(v48.contains("CREATE TABLE IF NOT EXISTS iam_resource_action"));
        assertTrue(v48.contains("CREATE TABLE IF NOT EXISTS iam_role_state_action_rule"));
        assertTrue(v48.contains("metadata_json JSONB"));
        assertTrue(v48.contains("idx_iam_resource_state_active"));
        assertTrue(v48.contains("idx_iam_resource_action_active"));
        assertTrue(v48.contains("idx_iam_role_state_action_rule_active"));
        assertTrue(v48.contains("'iam-state-action:view'"));
        assertTrue(v48.contains("'iam-state-action:create'"));
        assertTrue(v48.contains("'iam-state-action:update'"));
        assertTrue(v48.contains("'iam-state-action-management'"));
        assertTrue(v48.contains("/system/iam/state-action-permissions"));
        assertTrue(v48.contains("iam_role_permission"));
        assertTrue(v48.contains("iam_tenant_permission_entitlement"));
        assertTrue(v48.contains("iam_tenant_init_permission_template"));
        assertTrue(v48.contains("iam_tenant_init_role_template"));
        assertTrue(v48.contains("iam_authorization_snapshot"));
        assertTrue(v48.contains("state-action-v48"));
        assertFalse(v48.contains("DROP TABLE"), "V48 只能追加状态动作权限能力，不能删除历史表");
    }

    @Test
    void v49SeedsTenantStateActionRulesWithoutRewritingHistory() throws IOException {
        Path v49Path = MIGRATION_DIR.resolve("V49__seed_tenant_state_action_rules.sql");
        assertTrue(Files.exists(v49Path), "必须通过 V49 追加租户状态动作权限种子，不能改写 V1-V48 历史迁移");

        String v49 = Files.readString(v49Path);

        assertTrue(v49.contains("INSERT INTO iam_resource_state"));
        assertTrue(v49.contains("INSERT INTO iam_resource_action"));
        assertTrue(v49.contains("INSERT INTO iam_role_state_action_rule"));
        assertTrue(v49.contains("'tenant', 'PROVISIONED'"));
        assertTrue(v49.contains("'tenant', 'ENABLED'"));
        assertTrue(v49.contains("'tenant', 'DISABLED'"));
        assertTrue(v49.contains("'tenant', 'enable'"));
        assertTrue(v49.contains("'tenant', 'disable'"));
        assertTrue(v49.contains("'tenant', 'delete'"));
        assertTrue(v49.contains("'tenant:PROVISIONED', 'enable'"));
        assertTrue(v49.contains("'tenant:ENABLED', 'disable'"));
        assertTrue(v49.contains("'tenant:PROVISIONED', 'delete'"));
        assertTrue(v49.contains("'tenant:DISABLED', 'delete'"));
        assertTrue(v49.contains("state-action-tenant-v49"));
        assertFalse(v49.contains("CREATE TABLE"), "V49 只追加状态动作 seed，不应新增业务表");
        assertFalse(v49.contains("DROP TABLE"), "V49 不能删除历史表");
    }
}
