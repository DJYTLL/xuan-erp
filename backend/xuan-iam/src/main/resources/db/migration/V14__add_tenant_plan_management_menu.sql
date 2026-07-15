-- Add tenant plan management menu under 租户中心.
-- This migration is append-only: V1-V13 history remains unchanged.

WITH tenant_center AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system-tenant-center'
      AND deleted_at IS NULL
),
tenant_plan_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('tenant-plan-management', '套餐管理', 'route.tenantPlans', '/system/tenant-plans', 'CircleDollarSign', 'tenant:view', 15)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT
    tenant_plan_menu.code,
    tenant_center.id,
    tenant_plan_menu.title,
    tenant_plan_menu.i18n_key,
    tenant_plan_menu.path,
    tenant_plan_menu.icon,
    tenant_plan_menu.permission_code,
    tenant_plan_menu.sort_no,
    true,
    'system',
    'system'
FROM tenant_plan_menu
CROSS JOIN tenant_center
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = tenant_plan_menu.code
      AND existing.deleted_at IS NULL
);

WITH tenant_center AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system-tenant-center'
      AND deleted_at IS NULL
),
tenant_plan_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('tenant-plan-management', '套餐管理', 'route.tenantPlans', '/system/tenant-plans', 'CircleDollarSign', 'tenant:view', 15)
)
UPDATE iam_menu menu
SET parent_id = tenant_center.id,
    title = tenant_plan_menu.title,
    i18n_key = tenant_plan_menu.i18n_key,
    path = tenant_plan_menu.path,
    icon = tenant_plan_menu.icon,
    permission_code = tenant_plan_menu.permission_code,
    sort_no = tenant_plan_menu.sort_no,
    is_enabled = true,
    updated_by = 'system',
    updated_at = now()
FROM tenant_plan_menu
CROSS JOIN tenant_center
WHERE menu.code = tenant_plan_menu.code
  AND menu.deleted_at IS NULL;

INSERT INTO iam_tenant_menu (tenant_id, menu_id, is_enabled, created_by, updated_by)
SELECT DISTINCT tenant_menu.tenant_id,
       plan_menu.id,
       true,
       'system',
       'system'
FROM iam_tenant_menu tenant_menu
CROSS JOIN iam_menu plan_menu
WHERE plan_menu.code = 'tenant-plan-management'
  AND plan_menu.deleted_at IS NULL
  AND tenant_menu.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_tenant_menu existing
      WHERE existing.tenant_id = tenant_menu.tenant_id
        AND existing.menu_id = plan_menu.id
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
        'tenant-management', 'tenant-plan-management', 'iam-init-template-management',
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
