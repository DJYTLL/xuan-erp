-- Add xuan-audit audit_log query menu and permission. This migration is append-only: V1-V9 history remains unchanged.

WITH audit_log_permission(code, name, service_name, menu_code, description) AS (
    VALUES
        ('audit:log:view', '审计日志查看', 'xuan-audit', 'audit-logs', '查询 audit_log 审计日志')
)
INSERT INTO iam_permission (code, name, service_name, menu_code, description, is_enabled, created_by, updated_by)
SELECT audit_log_permission.code,
       audit_log_permission.name,
       audit_log_permission.service_name,
       audit_log_permission.menu_code,
       audit_log_permission.description,
       true,
       'system',
       'system'
FROM audit_log_permission
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_permission existing
    WHERE existing.code = audit_log_permission.code
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
audit_log_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('audit-logs', '审计日志', 'menu.auditLogs', '/system/audit/logs', 'ScrollText', 'audit:log:view', 130)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT audit_log_menu.code,
       system_menu.id,
       audit_log_menu.title,
       audit_log_menu.i18n_key,
       audit_log_menu.path,
       audit_log_menu.icon,
       audit_log_menu.permission_code,
       audit_log_menu.sort_no,
       true,
       'system',
       'system'
FROM audit_log_menu
CROSS JOIN system_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = audit_log_menu.code
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
  AND permission.code = 'audit:log:view'
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
WHERE audit_menu.code = 'audit-logs'
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
        || CASE
            WHEN NOT (COALESCE(snapshot.permission_codes, '[]'::jsonb) ? 'audit:log:view')
            THEN jsonb_build_array('audit:log:view')
            ELSE '[]'::jsonb
        END,
    menu_codes = COALESCE(snapshot.menu_codes, '[]'::jsonb)
        || CASE
            WHEN NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? 'audit-logs')
            THEN jsonb_build_array('audit-logs')
            ELSE '[]'::jsonb
        END,
    snapshot_hash = snapshot.snapshot_hash || '|audit-log-query-menu',
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
WHERE (
      COALESCE(snapshot.permission_codes, '[]'::jsonb) ? 'audit:view'
      OR COALESCE(snapshot.permission_codes, '[]'::jsonb) ? '*'
      OR COALESCE(snapshot.permission_codes, '[]'::jsonb) ? 'audit:interface-cost:view'
      OR COALESCE(snapshot.permission_codes, '[]'::jsonb) ? 'audit:sql-ranking:view'
  )
  AND (
      NOT (COALESCE(snapshot.permission_codes, '[]'::jsonb) ? 'audit:log:view')
      OR NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? 'audit-logs')
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
    10,
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
    'super_admin:audit-log-query-menu:' || resolved_user.id,
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
