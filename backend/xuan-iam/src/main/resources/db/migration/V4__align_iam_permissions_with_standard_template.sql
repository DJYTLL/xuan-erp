-- Align IAM permission seed data with the standard permission template.
-- This migration is append-only: V1/V2/V3 history remains unchanged.

WITH standard_permission(code, name, service_name, menu_code, description) AS (
    VALUES
        ('tenant:view', '租户查看', 'xuan-tenant', 'system', '查看租户、租户配置和生命周期历史'),
        ('tenant:create', '租户新增', 'xuan-tenant', 'system', '新增租户'),
        ('tenant:update', '租户修改', 'xuan-tenant', 'system', '修改租户基础资料'),
        ('tenant:delete', '租户删除', 'xuan-tenant', 'system', '删除租户'),
        ('tenant:lifecycle', '租户生命周期', 'xuan-tenant', 'system', '启用、暂停、停用和恢复租户'),
        ('tenant-config:view', '租户配置查看', 'xuan-tenant', 'system', '查看租户配置'),
        ('tenant-config:manage', '租户配置管理', 'xuan-tenant', 'system', '创建、修改和删除租户配置'),
        ('tenant-provision:view', '租户初始化任务查看', 'xuan-tenant', 'system', '查看租户初始化任务和事件'),
        ('tenant-provision:manage', '租户初始化任务管理', 'xuan-tenant', 'system', '重试租户初始化任务和事件'),
        ('product:view', '商品查看', 'xuan-product', 'product', '查看商品、分类、单位和车型适配'),
        ('product:create', '商品新增', 'xuan-product', 'product', '新增商品、分类、单位和车型适配'),
        ('product:update', '商品修改', 'xuan-product', 'product', '修改商品、分类、单位和车型适配'),
        ('product:delete', '商品删除', 'xuan-product', 'product', '删除、作废或停用商品资料'),
        ('product:audit', '商品审核', 'xuan-product', 'product', '审核、反审核或红冲商品资料'),
        ('product:export', '商品导出', 'xuan-product', 'product', '导出商品列表或明细'),
        ('party:view', '往来查看', 'xuan-party', 'party', '查看客户、供应商和往来主体'),
        ('party:create', '往来新增', 'xuan-party', 'party', '新增客户、供应商和往来主体'),
        ('party:update', '往来修改', 'xuan-party', 'party', '修改客户、供应商和往来主体'),
        ('party:delete', '往来删除', 'xuan-party', 'party', '删除、作废或停用往来主体'),
        ('warehouse:view', '仓库查看', 'xuan-warehouse', 'warehouse', '查看仓库和库位'),
        ('warehouse:create', '仓库新增', 'xuan-warehouse', 'warehouse', '新增仓库和库位'),
        ('warehouse:update', '仓库修改', 'xuan-warehouse', 'warehouse', '修改仓库和库位'),
        ('warehouse:delete', '仓库删除', 'xuan-warehouse', 'warehouse', '删除、作废或停用仓库和库位'),
        ('inventory:view', '库存查看', 'xuan-inventory', 'inventory', '查看库存、盘点和移库'),
        ('inventory:create', '库存新增', 'xuan-inventory', 'inventory', '新增库存业务单据'),
        ('inventory:update', '库存修改', 'xuan-inventory', 'inventory', '修改库存业务单据'),
        ('inventory:delete', '库存删除', 'xuan-inventory', 'inventory', '删除、作废或停用库存业务单据'),
        ('sales:view', '销售查看', 'xuan-sales', 'sales', '查看销售单和销售退货'),
        ('sales:create', '销售新增', 'xuan-sales', 'sales', '新增销售单和销售退货'),
        ('sales:update', '销售修改', 'xuan-sales', 'sales', '修改销售单和销售退货'),
        ('sales:delete', '销售删除', 'xuan-sales', 'sales', '删除、作废或停用销售单和销售退货'),
        ('sales:audit', '销售审核', 'xuan-sales', 'sales', '审核、反审核或红冲销售单'),
        ('sales:export', '销售导出', 'xuan-sales', 'sales', '导出销售列表或明细'),
        ('procurement:view', '采购查看', 'xuan-procurement', 'procurement', '查看采购单和采购退货'),
        ('procurement:create', '采购新增', 'xuan-procurement', 'procurement', '新增采购单和采购退货'),
        ('procurement:update', '采购修改', 'xuan-procurement', 'procurement', '修改采购单和采购退货'),
        ('procurement:delete', '采购删除', 'xuan-procurement', 'procurement', '删除、作废或停用采购单和采购退货'),
        ('procurement:audit', '采购审核', 'xuan-procurement', 'procurement', '审核、反审核或红冲采购单'),
        ('procurement:export', '采购导出', 'xuan-procurement', 'procurement', '导出采购列表或明细'),
        ('finance:view', '财务查看', 'xuan-finance', 'finance', '查看应收、应付、收款和付款'),
        ('finance:create', '财务新增', 'xuan-finance', 'finance', '新增财务业务单据'),
        ('finance:update', '财务修改', 'xuan-finance', 'finance', '修改财务业务单据'),
        ('finance:delete', '财务删除', 'xuan-finance', 'finance', '删除、作废或停用财务业务单据'),
        ('document:view', '打印查看', 'xuan-document', 'document', '查看打印模板、日志和快照'),
        ('document:create', '打印新增', 'xuan-document', 'document', '新增打印模板'),
        ('document:update', '打印修改', 'xuan-document', 'document', '修改打印模板'),
        ('document:delete', '打印删除', 'xuan-document', 'document', '删除、作废或停用打印模板'),
        ('manufacturing:view', '组装查看', 'xuan-manufacturing', 'manufacturing', '查看组装和拆分模板与单据'),
        ('manufacturing:create', '组装新增', 'xuan-manufacturing', 'manufacturing', '新增组装和拆分模板与单据'),
        ('manufacturing:update', '组装修改', 'xuan-manufacturing', 'manufacturing', '修改组装和拆分模板与单据'),
        ('manufacturing:delete', '组装删除', 'xuan-manufacturing', 'manufacturing', '删除、作废或停用组装和拆分模板与单据'),
        ('query:view', '报表查看', 'xuan-query', 'report', '查看报表和读模型'),
        ('query:create', '报表新增', 'xuan-query', 'report', '新增报表和读模型配置'),
        ('query:update', '报表修改', 'xuan-query', 'report', '修改报表和读模型配置'),
        ('query:delete', '报表删除', 'xuan-query', 'report', '删除、作废或停用报表和读模型配置'),
        ('audit:view', '审计查看', 'xuan-audit', 'system', '查看审计日志和 SQL trace'),
        ('audit:export', '审计导出', 'xuan-audit', 'system', '导出审计日志和 SQL trace')
)
INSERT INTO iam_permission (code, name, service_name, menu_code, description, is_enabled, created_by, updated_by)
SELECT standard_permission.code,
       standard_permission.name,
       standard_permission.service_name,
       standard_permission.menu_code,
       standard_permission.description,
       true,
       'system',
       'system'
FROM standard_permission
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_permission existing
    WHERE existing.code = standard_permission.code
      AND existing.deleted_at IS NULL
);

UPDATE iam_permission
SET is_enabled = false,
    updated_by = 'system',
    updated_at = now()
WHERE code IN (
    'tenant:read', 'tenant:manage',
    'product:read', 'product:manage',
    'party:read', 'party:manage',
    'warehouse:read', 'warehouse:manage',
    'inventory:read', 'inventory:manage',
    'sales:read', 'sales:manage',
    'procurement:read', 'procurement:manage',
    'finance:read', 'finance:manage',
    'document:read', 'document:manage',
    'manufacturing:read', 'manufacturing:manage',
    'query:read',
    'audit:read'
)
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'product:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'product'
  AND permission_code = 'product:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'procurement:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'procurement'
  AND permission_code = 'procurement:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'party:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'party'
  AND permission_code = 'party:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'warehouse:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'warehouse'
  AND permission_code = 'warehouse:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'inventory:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'inventory'
  AND permission_code = 'inventory:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'sales:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'sales'
  AND permission_code = 'sales:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'finance:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'finance'
  AND permission_code = 'finance:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'document:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'document'
  AND permission_code = 'document:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'manufacturing:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'manufacturing'
  AND permission_code = 'manufacturing:read'
  AND deleted_at IS NULL;

UPDATE iam_menu
SET permission_code = 'query:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'report'
  AND permission_code = 'query:read'
  AND deleted_at IS NULL;

WITH permission_mapping(old_code, new_code) AS (
    VALUES
        ('tenant:read', 'tenant:view'),
        ('tenant:manage', 'tenant:create'),
        ('tenant:manage', 'tenant:update'),
        ('tenant:manage', 'tenant:delete'),
        ('tenant:manage', 'tenant:lifecycle'),
        ('product:read', 'product:view'),
        ('product:manage', 'product:create'),
        ('product:manage', 'product:update'),
        ('product:manage', 'product:delete'),
        ('party:read', 'party:view'),
        ('party:manage', 'party:create'),
        ('party:manage', 'party:update'),
        ('party:manage', 'party:delete'),
        ('warehouse:read', 'warehouse:view'),
        ('warehouse:manage', 'warehouse:create'),
        ('warehouse:manage', 'warehouse:update'),
        ('warehouse:manage', 'warehouse:delete'),
        ('inventory:read', 'inventory:view'),
        ('inventory:manage', 'inventory:create'),
        ('inventory:manage', 'inventory:update'),
        ('inventory:manage', 'inventory:delete'),
        ('sales:read', 'sales:view'),
        ('sales:manage', 'sales:create'),
        ('sales:manage', 'sales:update'),
        ('sales:manage', 'sales:delete'),
        ('procurement:read', 'procurement:view'),
        ('procurement:manage', 'procurement:create'),
        ('procurement:manage', 'procurement:update'),
        ('procurement:manage', 'procurement:delete'),
        ('finance:read', 'finance:view'),
        ('finance:manage', 'finance:create'),
        ('finance:manage', 'finance:update'),
        ('finance:manage', 'finance:delete'),
        ('document:read', 'document:view'),
        ('document:manage', 'document:create'),
        ('document:manage', 'document:update'),
        ('document:manage', 'document:delete'),
        ('manufacturing:read', 'manufacturing:view'),
        ('manufacturing:manage', 'manufacturing:create'),
        ('manufacturing:manage', 'manufacturing:update'),
        ('manufacturing:manage', 'manufacturing:delete'),
        ('query:read', 'query:view'),
        ('audit:read', 'audit:view')
)
INSERT INTO iam_role_permission (tenant_id, role_id, permission_id, created_by, updated_by)
SELECT old_role_permission.tenant_id,
       old_role_permission.role_id,
       new_permission.id,
       'system',
       'system'
FROM iam_role_permission old_role_permission
JOIN iam_permission old_permission ON old_permission.id = old_role_permission.permission_id
JOIN permission_mapping ON permission_mapping.old_code = old_permission.code
JOIN iam_permission new_permission ON new_permission.code = permission_mapping.new_code
WHERE old_role_permission.deleted_at IS NULL
  AND new_permission.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_role_permission existing
      WHERE existing.tenant_id = old_role_permission.tenant_id
        AND existing.role_id = old_role_permission.role_id
        AND existing.permission_id = new_permission.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO iam_role_permission (tenant_id, role_id, permission_id, created_by, updated_by)
SELECT role.tenant_id,
       role.id,
       permission.id,
       'system',
       'system'
FROM iam_role role
CROSS JOIN iam_permission permission
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
SET permission_codes = rebuilt_snapshot.permission_codes,
    snapshot_hash = 'permissions-v4:' || snapshot.tenant_id || ':' || snapshot.user_id,
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
    2,
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
    'super_admin:all-permissions:' || resolved_user.id,
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
