-- Register base permissions for every planned Xuan ERP microservice.
-- This migration is append-only: V1-V14 history remains unchanged.

WITH base_permission(code, name, service_name, menu_code, description) AS (
    VALUES
        ('gateway:view', '网关查看', 'xuan-gateway', 'system', '查看网关路由、入口安全和转发状态'),
        ('gateway-config:view', '网关配置查看', 'xuan-gateway', 'system', '查看网关配置和路由策略'),
        ('gateway-config:manage', '网关配置管理', 'xuan-gateway', 'system', '维护网关配置和路由策略'),

        ('iam:view', '权限查看', 'xuan-iam', 'system', '查看用户、角色、权限、菜单和授权快照'),
        ('iam:create', '权限新增', 'xuan-iam', 'system', '新增用户、角色、权限和菜单'),
        ('iam:update', '权限修改', 'xuan-iam', 'system', '修改用户、角色、权限和菜单'),
        ('iam:delete', '权限删除', 'xuan-iam', 'system', '删除或逻辑删除用户、角色、权限和菜单'),
        ('iam:enable', '权限启用', 'xuan-iam', 'system', '启用用户、角色、权限或菜单'),
        ('iam:disable', '权限停用', 'xuan-iam', 'system', '停用用户、角色、权限或菜单'),

        ('tenant:view', '租户查看', 'xuan-tenant', 'system', '查看租户、租户配置和生命周期历史'),
        ('tenant:create', '租户新增', 'xuan-tenant', 'system', '新增租户'),
        ('tenant:update', '租户修改', 'xuan-tenant', 'system', '修改租户基础资料'),
        ('tenant:delete', '租户删除', 'xuan-tenant', 'system', '删除租户'),
        ('tenant:enable', '租户启用', 'xuan-tenant', 'system', '启用或恢复租户'),
        ('tenant:disable', '租户停用', 'xuan-tenant', 'system', '暂停、停用或冻结租户'),
        ('tenant-config:view', '租户配置查看', 'xuan-tenant', 'system', '查看租户配置'),
        ('tenant-config:manage', '租户配置管理', 'xuan-tenant', 'system', '创建、修改和删除租户配置'),
        ('tenant-provision:view', '租户初始化任务查看', 'xuan-tenant', 'system', '查看租户初始化任务和事件'),
        ('tenant-provision:manage', '租户初始化任务管理', 'xuan-tenant', 'system', '重试租户初始化任务和事件'),
        ('tenant-provision:callback', '租户初始化回调', 'xuan-tenant', 'system', '租户初始化步骤回调 IAM 的服务间权限'),

        ('audit:view', '审计查看', 'xuan-audit', 'system', '查看审计日志和 SQL trace'),
        ('audit:export', '审计导出', 'xuan-audit', 'system', '导出审计日志和 SQL trace'),
        ('audit:log:view', '审计日志查看', 'xuan-audit', 'audit-logs', '查看登录、操作和安全审计日志'),
        ('audit:interface-cost:view', '接口耗时查看', 'xuan-audit', 'audit-interface-costs', '查看 SkyWalking 接口耗时、trace 和 span 明细'),
        ('audit:sql-ranking:view', 'SQL 排名查看', 'xuan-audit', 'audit-sql-rankings', '查看 PostgreSQL pg_stat_statements SQL 耗时排名'),

        ('product:view', '商品查看', 'xuan-product', 'product', '查看商品、分类、单位和车型适配'),
        ('product:create', '商品新增', 'xuan-product', 'product', '新增商品、分类、单位和车型适配'),
        ('product:update', '商品修改', 'xuan-product', 'product', '修改商品、分类、单位和车型适配'),
        ('product:delete', '商品删除', 'xuan-product', 'product', '删除商品资料'),
        ('product:enable', '商品启用', 'xuan-product', 'product', '启用商品、分类、单位或车型适配'),
        ('product:disable', '商品停用', 'xuan-product', 'product', '停用商品、分类、单位或车型适配'),
        ('product:audit', '商品审核', 'xuan-product', 'product', '审核、反审核或红冲商品资料'),
        ('product:import', '商品导入', 'xuan-product', 'product', '导入商品列表或明细'),
        ('product:export', '商品导出', 'xuan-product', 'product', '导出商品列表或明细'),

        ('party:view', '往来查看', 'xuan-party', 'party', '查看客户、供应商和往来主体'),
        ('party:create', '往来新增', 'xuan-party', 'party', '新增客户、供应商和往来主体'),
        ('party:update', '往来修改', 'xuan-party', 'party', '修改客户、供应商和往来主体'),
        ('party:delete', '往来删除', 'xuan-party', 'party', '删除往来主体'),
        ('party:enable', '往来启用', 'xuan-party', 'party', '启用客户、供应商和往来主体'),
        ('party:disable', '往来停用', 'xuan-party', 'party', '停用客户、供应商和往来主体'),
        ('party:import', '往来导入', 'xuan-party', 'party', '导入客户、供应商和往来主体'),
        ('party:export', '往来导出', 'xuan-party', 'party', '导出客户、供应商和往来主体'),

        ('warehouse:view', '仓库查看', 'xuan-warehouse', 'warehouse', '查看仓库和库位'),
        ('warehouse:create', '仓库新增', 'xuan-warehouse', 'warehouse', '新增仓库和库位'),
        ('warehouse:update', '仓库修改', 'xuan-warehouse', 'warehouse', '修改仓库和库位'),
        ('warehouse:delete', '仓库删除', 'xuan-warehouse', 'warehouse', '删除仓库和库位'),
        ('warehouse:enable', '仓库启用', 'xuan-warehouse', 'warehouse', '启用仓库和库位'),
        ('warehouse:disable', '仓库停用', 'xuan-warehouse', 'warehouse', '停用仓库和库位'),
        ('warehouse:import', '仓库导入', 'xuan-warehouse', 'warehouse', '导入仓库和库位'),
        ('warehouse:export', '仓库导出', 'xuan-warehouse', 'warehouse', '导出仓库和库位'),

        ('inventory:view', '库存查看', 'xuan-inventory', 'inventory', '查看库存、盘点和移库'),
        ('inventory:create', '库存新增', 'xuan-inventory', 'inventory', '新增库存业务单据'),
        ('inventory:update', '库存修改', 'xuan-inventory', 'inventory', '修改库存业务单据'),
        ('inventory:delete', '库存删除', 'xuan-inventory', 'inventory', '删除库存业务单据'),
        ('inventory:enable', '库存启用', 'xuan-inventory', 'inventory', '启用库存业务单据或策略'),
        ('inventory:disable', '库存停用', 'xuan-inventory', 'inventory', '停用库存业务单据或策略'),
        ('inventory:audit', '库存审核', 'xuan-inventory', 'inventory', '审核库存业务单据'),
        ('inventory:import', '库存导入', 'xuan-inventory', 'inventory', '导入库存数据'),
        ('inventory:export', '库存导出', 'xuan-inventory', 'inventory', '导出库存数据'),

        ('sales:view', '销售查看', 'xuan-sales', 'sales', '查看销售单和销售退货'),
        ('sales:create', '销售新增', 'xuan-sales', 'sales', '新增销售单和销售退货'),
        ('sales:update', '销售修改', 'xuan-sales', 'sales', '修改销售单和销售退货'),
        ('sales:delete', '销售删除', 'xuan-sales', 'sales', '删除销售单和销售退货'),
        ('sales:enable', '销售启用', 'xuan-sales', 'sales', '启用销售业务配置或单据能力'),
        ('sales:disable', '销售停用', 'xuan-sales', 'sales', '停用销售业务配置或单据能力'),
        ('sales:audit', '销售审核', 'xuan-sales', 'sales', '审核、反审核或红冲销售单'),
        ('sales:import', '销售导入', 'xuan-sales', 'sales', '导入销售列表或明细'),
        ('sales:export', '销售导出', 'xuan-sales', 'sales', '导出销售列表或明细'),

        ('procurement:view', '采购查看', 'xuan-procurement', 'procurement', '查看采购单和采购退货'),
        ('procurement:create', '采购新增', 'xuan-procurement', 'procurement', '新增采购单和采购退货'),
        ('procurement:update', '采购修改', 'xuan-procurement', 'procurement', '修改采购单和采购退货'),
        ('procurement:delete', '采购删除', 'xuan-procurement', 'procurement', '删除采购单和采购退货'),
        ('procurement:enable', '采购启用', 'xuan-procurement', 'procurement', '启用采购业务配置或单据能力'),
        ('procurement:disable', '采购停用', 'xuan-procurement', 'procurement', '停用采购业务配置或单据能力'),
        ('procurement:audit', '采购审核', 'xuan-procurement', 'procurement', '审核、反审核或红冲采购单'),
        ('procurement:import', '采购导入', 'xuan-procurement', 'procurement', '导入采购列表或明细'),
        ('procurement:export', '采购导出', 'xuan-procurement', 'procurement', '导出采购列表或明细'),

        ('finance:view', '财务查看', 'xuan-finance', 'finance', '查看应收、应付、收款和付款'),
        ('finance:create', '财务新增', 'xuan-finance', 'finance', '新增财务业务单据'),
        ('finance:update', '财务修改', 'xuan-finance', 'finance', '修改财务业务单据'),
        ('finance:delete', '财务删除', 'xuan-finance', 'finance', '删除财务业务单据'),
        ('finance:enable', '财务启用', 'xuan-finance', 'finance', '启用财务业务配置或单据能力'),
        ('finance:disable', '财务停用', 'xuan-finance', 'finance', '停用财务业务配置或单据能力'),
        ('finance:audit', '财务审核', 'xuan-finance', 'finance', '审核财务业务单据'),
        ('finance:import', '财务导入', 'xuan-finance', 'finance', '导入财务列表或明细'),
        ('finance:export', '财务导出', 'xuan-finance', 'finance', '导出财务列表或明细'),

        ('document:view', '打印查看', 'xuan-document', 'document', '查看打印模板、日志和快照'),
        ('document:create', '打印新增', 'xuan-document', 'document', '新增打印模板'),
        ('document:update', '打印修改', 'xuan-document', 'document', '修改打印模板'),
        ('document:delete', '打印删除', 'xuan-document', 'document', '删除打印模板'),
        ('document:enable', '打印启用', 'xuan-document', 'document', '启用打印模板'),
        ('document:disable', '打印停用', 'xuan-document', 'document', '停用打印模板'),
        ('document:export', '打印导出', 'xuan-document', 'document', '导出打印模板、日志或快照'),

        ('manufacturing:view', '组装查看', 'xuan-manufacturing', 'manufacturing', '查看组装和拆分模板与单据'),
        ('manufacturing:create', '组装新增', 'xuan-manufacturing', 'manufacturing', '新增组装和拆分模板与单据'),
        ('manufacturing:update', '组装修改', 'xuan-manufacturing', 'manufacturing', '修改组装和拆分模板与单据'),
        ('manufacturing:delete', '组装删除', 'xuan-manufacturing', 'manufacturing', '删除组装和拆分模板与单据'),
        ('manufacturing:enable', '组装启用', 'xuan-manufacturing', 'manufacturing', '启用组装和拆分模板与单据'),
        ('manufacturing:disable', '组装停用', 'xuan-manufacturing', 'manufacturing', '停用组装和拆分模板与单据'),
        ('manufacturing:audit', '组装审核', 'xuan-manufacturing', 'manufacturing', '审核组装和拆分业务单据'),
        ('manufacturing:import', '组装导入', 'xuan-manufacturing', 'manufacturing', '导入组装和拆分数据'),
        ('manufacturing:export', '组装导出', 'xuan-manufacturing', 'manufacturing', '导出组装和拆分数据'),

        ('query:view', '报表查看', 'xuan-query', 'report', '查看报表和读模型'),
        ('query:create', '报表新增', 'xuan-query', 'report', '新增报表和读模型配置'),
        ('query:update', '报表修改', 'xuan-query', 'report', '修改报表和读模型配置'),
        ('query:delete', '报表删除', 'xuan-query', 'report', '删除报表和读模型配置'),
        ('query:enable', '报表启用', 'xuan-query', 'report', '启用报表和读模型配置'),
        ('query:disable', '报表停用', 'xuan-query', 'report', '停用报表和读模型配置'),
        ('query:export', '报表导出', 'xuan-query', 'report', '导出报表和读模型数据')
)
INSERT INTO iam_permission (code, name, service_name, menu_code, description, is_enabled, created_by, updated_by)
SELECT base_permission.code,
       base_permission.name,
       base_permission.service_name,
       base_permission.menu_code,
       base_permission.description,
       true,
       'system',
       'system'
FROM base_permission
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_permission existing
    WHERE existing.code = base_permission.code
      AND existing.deleted_at IS NULL
);

WITH legacy_permission_mapping(old_code, new_code) AS (
    VALUES
        ('tenant:lifecycle', 'tenant:enable'),
        ('tenant:lifecycle', 'tenant:disable')
)
INSERT INTO iam_role_permission (tenant_id, role_id, permission_id, created_by, updated_by)
SELECT old_role_permission.tenant_id,
       old_role_permission.role_id,
       new_permission.id,
       'system',
       'system'
FROM iam_role_permission old_role_permission
JOIN iam_permission old_permission ON old_permission.id = old_role_permission.permission_id
JOIN legacy_permission_mapping ON legacy_permission_mapping.old_code = old_permission.code
JOIN iam_permission new_permission ON new_permission.code = legacy_permission_mapping.new_code
WHERE old_role_permission.deleted_at IS NULL
  AND old_permission.deleted_at IS NULL
  AND new_permission.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_role_permission existing
      WHERE existing.tenant_id = old_role_permission.tenant_id
        AND existing.role_id = old_role_permission.role_id
        AND existing.permission_id = new_permission.id
        AND existing.deleted_at IS NULL
  );

WITH base_permission(code) AS (
    VALUES
        ('gateway:view'), ('gateway-config:view'), ('gateway-config:manage'),
        ('iam:view'), ('iam:create'), ('iam:update'), ('iam:delete'), ('iam:enable'), ('iam:disable'),
        ('tenant:view'), ('tenant:create'), ('tenant:update'), ('tenant:delete'), ('tenant:enable'), ('tenant:disable'),
        ('tenant-config:view'), ('tenant-config:manage'), ('tenant-provision:view'), ('tenant-provision:manage'), ('tenant-provision:callback'),
        ('audit:view'), ('audit:export'), ('audit:log:view'), ('audit:interface-cost:view'), ('audit:sql-ranking:view'),
        ('product:view'), ('product:create'), ('product:update'), ('product:delete'), ('product:enable'), ('product:disable'), ('product:audit'), ('product:import'), ('product:export'),
        ('party:view'), ('party:create'), ('party:update'), ('party:delete'), ('party:enable'), ('party:disable'), ('party:import'), ('party:export'),
        ('warehouse:view'), ('warehouse:create'), ('warehouse:update'), ('warehouse:delete'), ('warehouse:enable'), ('warehouse:disable'), ('warehouse:import'), ('warehouse:export'),
        ('inventory:view'), ('inventory:create'), ('inventory:update'), ('inventory:delete'), ('inventory:enable'), ('inventory:disable'), ('inventory:audit'), ('inventory:import'), ('inventory:export'),
        ('sales:view'), ('sales:create'), ('sales:update'), ('sales:delete'), ('sales:enable'), ('sales:disable'), ('sales:audit'), ('sales:import'), ('sales:export'),
        ('procurement:view'), ('procurement:create'), ('procurement:update'), ('procurement:delete'), ('procurement:enable'), ('procurement:disable'), ('procurement:audit'), ('procurement:import'), ('procurement:export'),
        ('finance:view'), ('finance:create'), ('finance:update'), ('finance:delete'), ('finance:enable'), ('finance:disable'), ('finance:audit'), ('finance:import'), ('finance:export'),
        ('document:view'), ('document:create'), ('document:update'), ('document:delete'), ('document:enable'), ('document:disable'), ('document:export'),
        ('manufacturing:view'), ('manufacturing:create'), ('manufacturing:update'), ('manufacturing:delete'), ('manufacturing:enable'), ('manufacturing:disable'), ('manufacturing:audit'), ('manufacturing:import'), ('manufacturing:export'),
        ('query:view'), ('query:create'), ('query:update'), ('query:delete'), ('query:enable'), ('query:disable'), ('query:export')
)
INSERT INTO iam_role_permission (tenant_id, role_id, permission_id, created_by, updated_by)
SELECT role.tenant_id,
       role.id,
       permission.id,
       'system',
       'system'
FROM iam_role role
CROSS JOIN base_permission
JOIN iam_permission permission ON permission.code = base_permission.code
WHERE role.code IN ('tenant_admin', 'super_admin')
  AND role.deleted_at IS NULL
  AND role.is_enabled = true
  AND permission.deleted_at IS NULL
  AND permission.is_enabled = true
  AND NOT EXISTS (
      SELECT 1
      FROM iam_role_permission existing
      WHERE existing.tenant_id = role.tenant_id
        AND existing.role_id = role.id
        AND existing.permission_id = permission.id
        AND existing.deleted_at IS NULL
  );

WITH base_permission(code) AS (
    VALUES
        ('gateway:view'), ('gateway-config:view'), ('gateway-config:manage'),
        ('iam:view'), ('iam:create'), ('iam:update'), ('iam:delete'), ('iam:enable'), ('iam:disable'),
        ('tenant:view'), ('tenant:create'), ('tenant:update'), ('tenant:delete'), ('tenant:enable'), ('tenant:disable'),
        ('tenant-config:view'), ('tenant-config:manage'), ('tenant-provision:view'), ('tenant-provision:manage'), ('tenant-provision:callback'),
        ('audit:view'), ('audit:export'), ('audit:log:view'), ('audit:interface-cost:view'), ('audit:sql-ranking:view'),
        ('product:view'), ('product:create'), ('product:update'), ('product:delete'), ('product:enable'), ('product:disable'), ('product:audit'), ('product:import'), ('product:export'),
        ('party:view'), ('party:create'), ('party:update'), ('party:delete'), ('party:enable'), ('party:disable'), ('party:import'), ('party:export'),
        ('warehouse:view'), ('warehouse:create'), ('warehouse:update'), ('warehouse:delete'), ('warehouse:enable'), ('warehouse:disable'), ('warehouse:import'), ('warehouse:export'),
        ('inventory:view'), ('inventory:create'), ('inventory:update'), ('inventory:delete'), ('inventory:enable'), ('inventory:disable'), ('inventory:audit'), ('inventory:import'), ('inventory:export'),
        ('sales:view'), ('sales:create'), ('sales:update'), ('sales:delete'), ('sales:enable'), ('sales:disable'), ('sales:audit'), ('sales:import'), ('sales:export'),
        ('procurement:view'), ('procurement:create'), ('procurement:update'), ('procurement:delete'), ('procurement:enable'), ('procurement:disable'), ('procurement:audit'), ('procurement:import'), ('procurement:export'),
        ('finance:view'), ('finance:create'), ('finance:update'), ('finance:delete'), ('finance:enable'), ('finance:disable'), ('finance:audit'), ('finance:import'), ('finance:export'),
        ('document:view'), ('document:create'), ('document:update'), ('document:delete'), ('document:enable'), ('document:disable'), ('document:export'),
        ('manufacturing:view'), ('manufacturing:create'), ('manufacturing:update'), ('manufacturing:delete'), ('manufacturing:enable'), ('manufacturing:disable'), ('manufacturing:audit'), ('manufacturing:import'), ('manufacturing:export'),
        ('query:view'), ('query:create'), ('query:update'), ('query:delete'), ('query:enable'), ('query:disable'), ('query:export')
)
UPDATE iam_tenant_init_permission_template template
SET permission_codes = COALESCE(template.permission_codes, '[]'::jsonb)
    || COALESCE((
        SELECT jsonb_agg(base_permission.code ORDER BY base_permission.code)
        FROM base_permission
        WHERE NOT (COALESCE(template.permission_codes, '[]'::jsonb) ? base_permission.code)
    ), '[]'::jsonb),
    updated_by = 'system',
    updated_at = now()
WHERE template.code = 'full'
  AND template.deleted_at IS NULL;

WITH rebuilt_snapshot AS (
    SELECT snapshot.id,
           COALESCE((
               SELECT jsonb_agg(code ORDER BY code)
               FROM (
                   SELECT DISTINCT permission.code
                   FROM jsonb_array_elements_text(snapshot.role_ids) AS role_id(value)
                   JOIN iam_role_permission role_permission
                     ON role_permission.tenant_id = snapshot.tenant_id
                    AND role_permission.role_id = role_id.value::bigint
                    AND role_permission.deleted_at IS NULL
                   JOIN iam_permission permission
                     ON permission.id = role_permission.permission_id
                    AND permission.deleted_at IS NULL
                    AND permission.is_enabled = true
               ) active_permission
           ), '[]'::jsonb) AS permission_codes
    FROM iam_authorization_snapshot snapshot
    WHERE snapshot.tenant_id <> 0
)
UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    permission_codes = rebuilt_snapshot.permission_codes,
    snapshot_hash = 'permissions-v15:' || snapshot.tenant_id || ':' || snapshot.user_id,
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM rebuilt_snapshot
WHERE snapshot.id = rebuilt_snapshot.id;

WITH resolved_user AS (
    SELECT id
    FROM iam_user
    WHERE tenant_id = 0
      AND username = 'super_admin'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
),
resolved_role AS (
    SELECT id
    FROM iam_role
    WHERE tenant_id = 0
      AND code = 'super_admin'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
)
INSERT INTO iam_authorization_snapshot (
    tenant_id, user_id, auth_version, role_ids, permission_codes, menu_codes, column_settings,
    snapshot_hash, built_at, created_by, updated_by
)
SELECT
    0,
    resolved_user.id,
    15,
    jsonb_build_array(resolved_role.id),
    jsonb_build_array('*') || COALESCE((
        SELECT jsonb_agg(permission.code ORDER BY permission.code)
        FROM iam_permission permission
        WHERE permission.is_enabled = true
          AND permission.deleted_at IS NULL
    ), '[]'::jsonb),
    COALESCE((
        SELECT jsonb_agg(menu.code ORDER BY menu.sort_no, menu.code)
        FROM iam_menu menu
        WHERE menu.deleted_at IS NULL
    ), '[]'::jsonb),
    '{}'::jsonb,
    'super_admin:all-service-base-permissions:' || resolved_user.id,
    now(),
    'system',
    'system'
FROM resolved_user
CROSS JOIN resolved_role
ON CONFLICT (tenant_id, user_id) DO UPDATE
SET auth_version = EXCLUDED.auth_version,
    role_ids = EXCLUDED.role_ids,
    permission_codes = EXCLUDED.permission_codes,
    menu_codes = EXCLUDED.menu_codes,
    column_settings = EXCLUDED.column_settings,
    snapshot_hash = EXCLUDED.snapshot_hash,
    built_at = EXCLUDED.built_at,
    updated_by = EXCLUDED.updated_by,
    updated_at = now();
