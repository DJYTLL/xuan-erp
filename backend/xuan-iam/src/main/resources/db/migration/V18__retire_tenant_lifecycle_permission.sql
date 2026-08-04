-- Retire the legacy tenant:lifecycle permission after tenant enable/disable split.
-- This migration is append-only: V1-V17 history remains unchanged.

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

UPDATE iam_role_permission role_permission
SET deleted_by = 'system',
    delete_reason = 'tenant:lifecycle retired by V18; use tenant:enable and tenant:disable',
    deleted_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM iam_permission permission
WHERE role_permission.permission_id = permission.id
  AND permission.code = 'tenant:lifecycle'
  AND role_permission.deleted_at IS NULL
  AND permission.deleted_at IS NULL;

UPDATE iam_permission
SET is_enabled = false,
    updated_by = 'system',
    updated_at = now()
WHERE code = 'tenant:lifecycle'
  AND deleted_at IS NULL;

WITH replacement_permission(code) AS (
    VALUES
        ('tenant:enable'),
        ('tenant:disable')
)
UPDATE iam_tenant_init_permission_template template
SET permission_codes = (COALESCE(template.permission_codes, '[]'::jsonb) - 'tenant:lifecycle')
    || COALESCE((
        SELECT jsonb_agg(replacement_permission.code ORDER BY replacement_permission.code)
        FROM replacement_permission
        WHERE NOT ((COALESCE(template.permission_codes, '[]'::jsonb) - 'tenant:lifecycle') ? replacement_permission.code)
    ), '[]'::jsonb),
    updated_by = 'system',
    updated_at = now()
WHERE template.deleted_at IS NULL
  AND (
      COALESCE(template.permission_codes, '[]'::jsonb) ? 'tenant:lifecycle'
      OR template.code = 'full'
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
SET auth_version = snapshot.auth_version + 1,
    permission_codes = rebuilt_snapshot.permission_codes,
    snapshot_hash = 'permissions-v18:' || snapshot.tenant_id || ':' || snapshot.user_id,
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM rebuilt_snapshot
WHERE snapshot.id = rebuilt_snapshot.id;

WITH resolved_users AS (
    SELECT id, username
    FROM iam_user
    WHERE tenant_id = 0
      AND username IN ('super_admin', 'superadmin')
      AND deleted_at IS NULL
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
    resolved_users.id,
    18,
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
    'super_admin:tenant-lifecycle-retired:' || resolved_users.username || ':' || resolved_users.id,
    now(),
    'system',
    'system'
FROM resolved_users
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
