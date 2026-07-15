-- Add real IAM menu rows for frontend navigation layout groups.
-- This keeps menu management editable for groups such as 进销存 / 基础资料.

WITH group_menu(code, parent_code, title, i18n_key, icon, sort_no) AS (
    VALUES
        ('inventory-root', NULL::varchar, '进销存', 'nav.inventory', 'PackageOpen', 20),
        ('base-data', 'inventory-root', '基础资料', 'nav.baseData', NULL, 10),
        ('stock-management', 'inventory-root', '库存管理', 'menu.stockManagement', NULL, 20),
        ('purchase-management', 'inventory-root', '采购管理', 'nav.purchase', NULL, 30),
        ('sales-management', 'inventory-root', '销售管理', 'nav.sales', NULL, 40)
),
resolved_group_menu AS (
    SELECT
        group_menu.code,
        parent.id AS parent_id,
        group_menu.title,
        group_menu.i18n_key,
        group_menu.icon,
        group_menu.sort_no
    FROM group_menu
    LEFT JOIN iam_menu parent
        ON parent.code = group_menu.parent_code
       AND parent.deleted_at IS NULL
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT
    resolved_group_menu.code,
    resolved_group_menu.parent_id,
    resolved_group_menu.title,
    resolved_group_menu.i18n_key,
    NULL,
    resolved_group_menu.icon,
    NULL,
    resolved_group_menu.sort_no,
    true,
    'system',
    'system'
FROM resolved_group_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = resolved_group_menu.code
      AND existing.deleted_at IS NULL
);

WITH parent_mapping(child_code, parent_code, sort_no) AS (
    VALUES
        ('product', 'base-data', 10),
        ('party', 'base-data', 20),
        ('warehouse', 'base-data', 30),
        ('inventory', 'stock-management', 10),
        ('procurement', 'purchase-management', 10),
        ('sales', 'sales-management', 10)
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
       group_menu.id,
       true,
       'system',
       'system'
FROM iam_tenant_menu tenant_menu
CROSS JOIN iam_menu group_menu
WHERE group_menu.code IN (
        'inventory-root', 'base-data', 'stock-management', 'purchase-management', 'sales-management'
    )
  AND group_menu.deleted_at IS NULL
  AND tenant_menu.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_tenant_menu existing
      WHERE existing.tenant_id = tenant_menu.tenant_id
        AND existing.menu_id = group_menu.id
        AND existing.deleted_at IS NULL
  );

UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    menu_codes = COALESCE(snapshot.menu_codes, '[]'::jsonb)
        || COALESCE((
            SELECT jsonb_agg(group_code.code ORDER BY group_code.sort_no)
            FROM (
                VALUES
                    ('inventory-root', 10),
                    ('base-data', 20),
                    ('stock-management', 30),
                    ('purchase-management', 40),
                    ('sales-management', 50)
            ) AS group_code(code, sort_no)
            WHERE NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? group_code.code)
        ), '[]'::jsonb),
    snapshot_hash = snapshot.snapshot_hash || '|navigation-layout-groups',
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
WHERE (snapshot.permission_codes ? 'iam:view' OR snapshot.permission_codes ? '*')
  AND EXISTS (
      SELECT 1
      FROM (
          VALUES
              ('inventory-root'),
              ('base-data'),
              ('stock-management'),
              ('purchase-management'),
              ('sales-management')
      ) AS group_code(code)
      WHERE NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? group_code.code)
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
        'iam-menu-management', 'iam-permission-management', 'iam-role-management', 'iam-user-management'
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
