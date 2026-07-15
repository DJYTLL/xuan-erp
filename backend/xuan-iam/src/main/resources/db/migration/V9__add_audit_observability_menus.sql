-- Add xuan-audit observability menus and permissions. This migration is append-only: V1-V8 history remains unchanged.

WITH audit_permission(code, name, service_name, menu_code, description) AS (
    VALUES
        ('audit:interface-cost:view', '接口耗时查看', 'xuan-audit', 'audit-interface-costs', '查看 SkyWalking 接口耗时、trace 和 span 明细'),
        ('audit:sql-ranking:view', 'SQL 排名查看', 'xuan-audit', 'audit-sql-rankings', '查看 PostgreSQL pg_stat_statements SQL 耗时排名')
)
INSERT INTO iam_permission (code, name, service_name, menu_code, description, is_enabled, created_by, updated_by)
SELECT audit_permission.code,
       audit_permission.name,
       audit_permission.service_name,
       audit_permission.menu_code,
       audit_permission.description,
       true,
       'system',
       'system'
FROM audit_permission
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_permission existing
    WHERE existing.code = audit_permission.code
      AND existing.deleted_at IS NULL
);

WITH system_menu AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
),
audit_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('audit-interface-costs', '接口耗时', 'menu.auditInterfaceCosts', '/system/audit/interface-costs', 'Activity', 'audit:interface-cost:view', 131),
        ('audit-sql-rankings', 'SQL 排名', 'menu.auditSqlRankings', '/system/audit/sql-rankings', 'Database', 'audit:sql-ranking:view', 132)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT audit_menu.code,
       system_menu.id,
       audit_menu.title,
       audit_menu.i18n_key,
       audit_menu.path,
       audit_menu.icon,
       audit_menu.permission_code,
       audit_menu.sort_no,
       true,
       'system',
       'system'
FROM audit_menu
CROSS JOIN system_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = audit_menu.code
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
  AND permission.code IN ('audit:interface-cost:view', 'audit:sql-ranking:view')
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

INSERT INTO iam_tenant_menu (tenant_id, menu_id, is_enabled, created_by, updated_by)
SELECT DISTINCT tenant_menu.tenant_id,
       audit_menu.id,
       true,
       'system',
       'system'
FROM iam_tenant_menu tenant_menu
CROSS JOIN iam_menu audit_menu
WHERE audit_menu.code IN ('audit-interface-costs', 'audit-sql-rankings')
  AND audit_menu.deleted_at IS NULL
  AND tenant_menu.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_tenant_menu existing
      WHERE existing.tenant_id = tenant_menu.tenant_id
        AND existing.menu_id = audit_menu.id
        AND existing.deleted_at IS NULL
  );

UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    permission_codes = COALESCE(snapshot.permission_codes, '[]'::jsonb)
        || COALESCE((
            SELECT jsonb_agg(permission_code.code ORDER BY permission_code.sort_no)
            FROM (
                VALUES
                    ('audit:interface-cost:view', 10),
                    ('audit:sql-ranking:view', 20)
            ) AS permission_code(code, sort_no)
            WHERE NOT (COALESCE(snapshot.permission_codes, '[]'::jsonb) ? permission_code.code)
        ), '[]'::jsonb),
    menu_codes = COALESCE(snapshot.menu_codes, '[]'::jsonb)
        || COALESCE((
            SELECT jsonb_agg(menu_code.code ORDER BY menu_code.sort_no)
            FROM (
                VALUES
                    ('audit-interface-costs', 10),
                    ('audit-sql-rankings', 20)
            ) AS menu_code(code, sort_no)
            WHERE NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? menu_code.code)
        ), '[]'::jsonb),
    snapshot_hash = snapshot.snapshot_hash || '|audit-observability-menus',
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
WHERE (snapshot.permission_codes ? 'audit:view' OR snapshot.permission_codes ? '*')
  AND (
      NOT (COALESCE(snapshot.permission_codes, '[]'::jsonb) ? 'audit:interface-cost:view')
      OR NOT (COALESCE(snapshot.permission_codes, '[]'::jsonb) ? 'audit:sql-ranking:view')
      OR NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? 'audit-interface-costs')
      OR NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? 'audit-sql-rankings')
  );

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
    9,
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
    'super_admin:audit-observability-menus:' || resolved_user.id,
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
        'iam-menu-management', 'iam-permission-management', 'iam-role-management', 'iam-user-management',
        'audit-interface-costs', 'audit-sql-rankings'
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
