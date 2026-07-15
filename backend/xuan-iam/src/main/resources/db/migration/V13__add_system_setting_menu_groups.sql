-- Add second-level grouping menus under 系统设置.
-- This migration is append-only: V1-V12 history remains unchanged.

WITH system_menu AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system'
      AND deleted_at IS NULL
),
group_menu(code, title, i18n_key, icon, sort_no) AS (
    VALUES
        ('system-permission-center', '权限中心', 'menu.permissionCenter', 'KeyRound', 10),
        ('system-tenant-center', '租户中心', 'menu.tenantCenter', 'Building2', 20),
        ('system-audit-center', '审计中心', 'menu.auditCenter', 'ScrollText', 30)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT
    group_menu.code,
    system_menu.id,
    group_menu.title,
    group_menu.i18n_key,
    NULL,
    group_menu.icon,
    NULL,
    group_menu.sort_no,
    true,
    'system',
    'system'
FROM group_menu
CROSS JOIN system_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = group_menu.code
      AND existing.deleted_at IS NULL
);

WITH system_menu AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system'
      AND deleted_at IS NULL
),
group_menu(code, title, i18n_key, icon, sort_no) AS (
    VALUES
        ('system-permission-center', '权限中心', 'menu.permissionCenter', 'KeyRound', 10),
        ('system-tenant-center', '租户中心', 'menu.tenantCenter', 'Building2', 20),
        ('system-audit-center', '审计中心', 'menu.auditCenter', 'ScrollText', 30)
)
UPDATE iam_menu menu
SET parent_id = system_menu.id,
    title = group_menu.title,
    i18n_key = group_menu.i18n_key,
    path = NULL,
    icon = group_menu.icon,
    permission_code = NULL,
    sort_no = group_menu.sort_no,
    is_enabled = true,
    updated_by = 'system',
    updated_at = now()
FROM group_menu
CROSS JOIN system_menu
WHERE menu.code = group_menu.code
  AND menu.deleted_at IS NULL;

WITH tenant_center AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system-tenant-center'
      AND deleted_at IS NULL
),
tenant_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('tenant-management', '租户管理', 'route.tenants', '/system/tenants', 'Building2', 'tenant:view', 10)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT
    tenant_menu.code,
    tenant_center.id,
    tenant_menu.title,
    tenant_menu.i18n_key,
    tenant_menu.path,
    tenant_menu.icon,
    tenant_menu.permission_code,
    tenant_menu.sort_no,
    true,
    'system',
    'system'
FROM tenant_menu
CROSS JOIN tenant_center
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = tenant_menu.code
      AND existing.deleted_at IS NULL
);

WITH tenant_center AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system-tenant-center'
      AND deleted_at IS NULL
),
tenant_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('tenant-management', '租户管理', 'route.tenants', '/system/tenants', 'Building2', 'tenant:view', 10)
)
UPDATE iam_menu menu
SET parent_id = tenant_center.id,
    title = tenant_menu.title,
    i18n_key = tenant_menu.i18n_key,
    path = tenant_menu.path,
    icon = tenant_menu.icon,
    permission_code = tenant_menu.permission_code,
    sort_no = tenant_menu.sort_no,
    is_enabled = true,
    updated_by = 'system',
    updated_at = now()
FROM tenant_menu
CROSS JOIN tenant_center
WHERE menu.code = tenant_menu.code
  AND menu.deleted_at IS NULL;

WITH parent_mapping(child_code, parent_code, sort_no) AS (
    VALUES
        ('iam-menu-management', 'system-permission-center', 10),
        ('iam-permission-management', 'system-permission-center', 20),
        ('iam-role-management', 'system-permission-center', 30),
        ('iam-user-management', 'system-permission-center', 40),
        ('tenant-management', 'system-tenant-center', 10),
        ('iam-init-template-management', 'system-tenant-center', 20),
        ('audit-logs', 'system-audit-center', 10),
        ('audit-interface-costs', 'system-audit-center', 20),
        ('audit-sql-rankings', 'system-audit-center', 30)
)
UPDATE iam_menu child
SET parent_id = parent.id,
    sort_no = parent_mapping.sort_no,
    updated_by = 'system',
    updated_at = now()
FROM parent_mapping
JOIN iam_menu parent
  ON parent.code = parent_mapping.parent_code
 AND parent.deleted_at IS NULL
WHERE child.code = parent_mapping.child_code
  AND child.deleted_at IS NULL;

INSERT INTO iam_tenant_menu (tenant_id, menu_id, is_enabled, created_by, updated_by)
SELECT DISTINCT tenant_menu.tenant_id,
       system_menu.id,
       true,
       'system',
       'system'
FROM iam_tenant_menu tenant_menu
CROSS JOIN iam_menu system_menu
WHERE system_menu.code IN (
        'system-permission-center', 'system-tenant-center', 'system-audit-center',
        'tenant-management', 'iam-init-template-management'
    )
  AND system_menu.deleted_at IS NULL
  AND tenant_menu.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_tenant_menu existing
      WHERE existing.tenant_id = tenant_menu.tenant_id
        AND existing.menu_id = system_menu.id
        AND existing.deleted_at IS NULL
  );

CREATE OR REPLACE FUNCTION bootstrap_iam_tenant(
    p_tenant_id bigint,
    p_requested_by varchar DEFAULT 'tenant-provision'
)
RETURNS integer
LANGUAGE plpgsql
AS $$
DECLARE
    v_inserted_count integer := 0;
    v_task_key varchar(160);
    v_idempotency_key varchar(200);
BEGIN
    IF p_tenant_id IS NULL OR p_tenant_id <= 0 THEN
        RAISE EXCEPTION 'p_tenant_id must be a positive tenant id';
    END IF;

    v_task_key := 'IAM_TENANT_BOOTSTRAP';
    v_idempotency_key := 'iam:tenant:' || p_tenant_id || ':bootstrap:v1';

    INSERT INTO iam_tenant_menu (tenant_id, menu_id, is_enabled, created_by, updated_by)
    SELECT p_tenant_id, iam_menu.id, true, p_requested_by, p_requested_by
    FROM iam_menu
    WHERE iam_menu.code IN (
        'workbench',
        'inventory-root', 'base-data', 'product', 'party', 'warehouse',
        'stock-management', 'inventory',
        'purchase-management', 'procurement',
        'sales-management', 'sales',
        'finance', 'document', 'manufacturing', 'report', 'system',
        'system-permission-center',
        'iam-menu-management', 'iam-permission-management', 'iam-role-management', 'iam-user-management',
        'system-tenant-center',
        'tenant-management', 'iam-init-template-management',
        'system-audit-center',
        'audit-logs', 'audit-interface-costs', 'audit-sql-rankings'
    )
      AND iam_menu.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM iam_tenant_menu existing
          WHERE existing.tenant_id = p_tenant_id
            AND existing.menu_id = iam_menu.id
            AND existing.deleted_at IS NULL
      );

    GET DIAGNOSTICS v_inserted_count = ROW_COUNT;

    INSERT INTO iam_tenant_bootstrap_task (
        tenant_id,
        task_key,
        status,
        idempotency_key,
        requested_by,
        menu_grant_count,
        started_at,
        finished_at,
        created_by,
        updated_by
    )
    SELECT
        p_tenant_id,
        v_task_key,
        'SUCCEEDED',
        v_idempotency_key,
        p_requested_by,
        v_inserted_count,
        now(),
        now(),
        p_requested_by,
        p_requested_by
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_tenant_bootstrap_task existing
        WHERE existing.idempotency_key = v_idempotency_key
    );

    RETURN v_inserted_count;
EXCEPTION WHEN OTHERS THEN
    INSERT INTO iam_tenant_bootstrap_task (
        tenant_id,
        task_key,
        status,
        idempotency_key,
        requested_by,
        menu_grant_count,
        started_at,
        finished_at,
        last_error_message,
        created_by,
        updated_by
    )
    VALUES (
        p_tenant_id,
        COALESCE(v_task_key, 'IAM_TENANT_BOOTSTRAP'),
        'FAILED',
        COALESCE(v_idempotency_key, 'iam:tenant:' || COALESCE(p_tenant_id::text, 'null') || ':bootstrap:v1'),
        p_requested_by,
        0,
        now(),
        now(),
        SQLERRM,
        p_requested_by,
        p_requested_by
    );
    RAISE;
END;
$$;

COMMENT ON FUNCTION bootstrap_iam_tenant(bigint, varchar) IS '按 xuan-tenant 传入的真实 tenantId 幂等初始化 IAM 租户菜单授权。';
