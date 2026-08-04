-- Rebuild tenant permission entitlement pools from the stable init-template binding.
-- V25 allowed historical role grants to seed the entitlement pool; that can preserve stale full permissions.
-- This migration makes the bound init template the authority, then prunes role grants and snapshots.

WITH active_binding AS (
    SELECT binding.tenant_id,
           binding.init_template_code
    FROM iam_tenant_init_template_binding binding
    JOIN iam_tenant_init_permission_template template
      ON template.code = binding.init_template_code
     AND template.deleted_at IS NULL
     AND template.is_enabled = true
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
),
next_version AS (
    SELECT active_binding.tenant_id,
           active_binding.init_template_code,
           COALESCE(MAX(entitlement.entitlement_version), 0) + 1 AS entitlement_version
    FROM active_binding
    LEFT JOIN iam_tenant_permission_entitlement entitlement
      ON entitlement.tenant_id = active_binding.tenant_id
    GROUP BY active_binding.tenant_id, active_binding.init_template_code
)
UPDATE iam_tenant_permission_entitlement entitlement
SET deleted_by = 'system',
    delete_reason = 'rebuild by iam init template binding',
    deleted_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM next_version
WHERE entitlement.tenant_id = next_version.tenant_id
  AND entitlement.deleted_at IS NULL;

WITH active_binding AS (
    SELECT binding.tenant_id,
           binding.init_template_code,
           template.permission_codes
    FROM iam_tenant_init_template_binding binding
    JOIN iam_tenant_init_permission_template template
      ON template.code = binding.init_template_code
     AND template.deleted_at IS NULL
     AND template.is_enabled = true
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
),
next_version AS (
    SELECT active_binding.tenant_id,
           active_binding.init_template_code,
           COALESCE(MAX(entitlement.entitlement_version), 0) + 1 AS entitlement_version
    FROM active_binding
    LEFT JOIN iam_tenant_permission_entitlement entitlement
      ON entitlement.tenant_id = active_binding.tenant_id
    GROUP BY active_binding.tenant_id, active_binding.init_template_code
),
template_permission AS (
    SELECT DISTINCT active_binding.tenant_id,
           active_binding.init_template_code,
           permission_code.value AS permission_code
    FROM active_binding
    JOIN LATERAL jsonb_array_elements_text(COALESCE(active_binding.permission_codes, '[]'::jsonb)) AS permission_code(value) ON true
),
resolved_permission AS (
    SELECT template_permission.tenant_id,
           template_permission.init_template_code,
           permission.id AS permission_id
    FROM template_permission
    JOIN iam_permission permission
      ON permission.code = template_permission.permission_code
     AND permission.deleted_at IS NULL
     AND permission.is_enabled = true
)
INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, is_enabled, created_by, updated_by
)
SELECT resolved_permission.tenant_id,
       resolved_permission.init_template_code,
       resolved_permission.permission_id,
       next_version.entitlement_version,
       true,
       'system',
       'system'
FROM resolved_permission
JOIN next_version
  ON next_version.tenant_id = resolved_permission.tenant_id
 AND next_version.init_template_code = resolved_permission.init_template_code
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_tenant_permission_entitlement existing
    WHERE existing.tenant_id = resolved_permission.tenant_id
      AND existing.permission_id = resolved_permission.permission_id
      AND existing.deleted_at IS NULL
);

WITH active_binding AS (
    SELECT binding.tenant_id,
           binding.init_template_code
    FROM iam_tenant_init_template_binding binding
    JOIN iam_tenant_init_permission_template template
      ON template.code = binding.init_template_code
     AND template.deleted_at IS NULL
     AND template.is_enabled = true
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
),
latest_version AS (
    SELECT entitlement.tenant_id,
           MAX(entitlement.entitlement_version) AS entitlement_version
    FROM iam_tenant_permission_entitlement entitlement
    JOIN active_binding ON active_binding.tenant_id = entitlement.tenant_id
    WHERE entitlement.deleted_at IS NULL
      AND entitlement.is_enabled = true
    GROUP BY entitlement.tenant_id
)
UPDATE iam_tenant_init_template_binding binding
SET last_entitlement_version = latest_version.entitlement_version,
    updated_by = 'system',
    updated_at = now()
FROM latest_version
WHERE binding.tenant_id = latest_version.tenant_id
  AND binding.deleted_at IS NULL;

WITH active_binding AS (
    SELECT binding.tenant_id
    FROM iam_tenant_init_template_binding binding
    JOIN iam_tenant_init_permission_template template
      ON template.code = binding.init_template_code
     AND template.deleted_at IS NULL
     AND template.is_enabled = true
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
)
UPDATE iam_role_permission role_permission
SET deleted_by = 'system',
    delete_reason = 'tenant permission entitlement reduced',
    deleted_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM active_binding
WHERE role_permission.tenant_id = active_binding.tenant_id
  AND role_permission.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_tenant_permission_entitlement entitlement
      WHERE entitlement.tenant_id = role_permission.tenant_id
        AND entitlement.permission_id = role_permission.permission_id
        AND entitlement.deleted_at IS NULL
        AND entitlement.is_enabled = true
  );

WITH RECURSIVE active_binding AS (
    SELECT binding.tenant_id
    FROM iam_tenant_init_template_binding binding
    JOIN iam_tenant_init_permission_template template
      ON template.code = binding.init_template_code
     AND template.deleted_at IS NULL
     AND template.is_enabled = true
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
),
active_user AS (
    SELECT iam_user.tenant_id,
           iam_user.id AS user_id,
           iam_user.auth_version
    FROM iam_user
    JOIN active_binding ON active_binding.tenant_id = iam_user.tenant_id
    WHERE iam_user.deleted_at IS NULL
      AND iam_user.is_enabled = true
),
active_user_role AS (
    SELECT user_role.tenant_id,
           user_role.user_id,
           role.id AS role_id
    FROM iam_user_role user_role
    JOIN iam_role role
      ON role.id = user_role.role_id
     AND role.tenant_id = user_role.tenant_id
     AND role.deleted_at IS NULL
     AND role.is_enabled = true
    JOIN active_user
      ON active_user.tenant_id = user_role.tenant_id
     AND active_user.user_id = user_role.user_id
    WHERE user_role.deleted_at IS NULL
),
user_permission AS (
    SELECT DISTINCT active_user_role.tenant_id,
           active_user_role.user_id,
           permission.code
    FROM active_user_role
    JOIN iam_role_permission role_permission
      ON role_permission.tenant_id = active_user_role.tenant_id
     AND role_permission.role_id = active_user_role.role_id
     AND role_permission.deleted_at IS NULL
    JOIN iam_tenant_permission_entitlement entitlement
      ON entitlement.tenant_id = role_permission.tenant_id
     AND entitlement.permission_id = role_permission.permission_id
     AND entitlement.deleted_at IS NULL
     AND entitlement.is_enabled = true
    JOIN iam_permission permission
      ON permission.id = role_permission.permission_id
     AND permission.deleted_at IS NULL
     AND permission.is_enabled = true
),
direct_menu AS (
    SELECT DISTINCT active_user.tenant_id,
           active_user.user_id,
           menu.id AS menu_id,
           menu.parent_id
    FROM active_user
    JOIN iam_menu menu
      ON menu.deleted_at IS NULL
     AND menu.is_enabled = true
    WHERE menu.permission_code IS NULL
       OR menu.permission_code = ''
       OR EXISTS (
           SELECT 1
           FROM user_permission
           WHERE user_permission.tenant_id = active_user.tenant_id
             AND user_permission.user_id = active_user.user_id
             AND user_permission.code = menu.permission_code
       )
),
menu_tree(tenant_id, user_id, menu_id, parent_id) AS (
    SELECT direct_menu.tenant_id,
           direct_menu.user_id,
           direct_menu.menu_id,
           direct_menu.parent_id
    FROM direct_menu
    UNION
    SELECT menu_tree.tenant_id,
           menu_tree.user_id,
           parent.id,
           parent.parent_id
    FROM menu_tree
    JOIN iam_menu parent
      ON parent.id = menu_tree.parent_id
     AND parent.deleted_at IS NULL
     AND parent.is_enabled = true
),
rebuilt_snapshot AS (
    SELECT active_user.tenant_id,
           active_user.user_id,
           COALESCE(existing_snapshot.auth_version, active_user.auth_version, 0) + 1 AS auth_version,
           COALESCE((
               SELECT jsonb_agg(role_id ORDER BY role_id)
               FROM (
                   SELECT DISTINCT active_user_role.role_id
                   FROM active_user_role
                   WHERE active_user_role.tenant_id = active_user.tenant_id
                     AND active_user_role.user_id = active_user.user_id
               ) role_item
           ), '[]'::jsonb) AS role_ids,
           COALESCE((
               SELECT jsonb_agg(code ORDER BY code)
               FROM (
                   SELECT DISTINCT user_permission.code
                   FROM user_permission
                   WHERE user_permission.tenant_id = active_user.tenant_id
                     AND user_permission.user_id = active_user.user_id
               ) permission_item
           ), '[]'::jsonb) AS permission_codes,
           COALESCE((
               SELECT jsonb_agg(menu_code.code ORDER BY menu_code.sort_no, menu_code.code)
               FROM (
                   SELECT DISTINCT menu.code,
                          menu.sort_no
                   FROM menu_tree
                   JOIN iam_menu menu ON menu.id = menu_tree.menu_id
                   WHERE menu_tree.tenant_id = active_user.tenant_id
                     AND menu_tree.user_id = active_user.user_id
                     AND menu.deleted_at IS NULL
                     AND menu.is_enabled = true
               ) menu_code
           ), '[]'::jsonb) AS menu_codes,
           COALESCE(existing_snapshot.column_settings, '{}'::jsonb) AS column_settings
    FROM active_user
    LEFT JOIN iam_authorization_snapshot existing_snapshot
      ON existing_snapshot.tenant_id = active_user.tenant_id
     AND existing_snapshot.user_id = active_user.user_id
)
INSERT INTO iam_authorization_snapshot (
    tenant_id, user_id, auth_version, role_ids, permission_codes, menu_codes, column_settings,
    snapshot_hash, built_at, created_by, updated_by
)
SELECT rebuilt_snapshot.tenant_id,
       rebuilt_snapshot.user_id,
       rebuilt_snapshot.auth_version,
       rebuilt_snapshot.role_ids,
       rebuilt_snapshot.permission_codes,
       rebuilt_snapshot.menu_codes,
       rebuilt_snapshot.column_settings,
       'tenant-entitlement-v27:' || rebuilt_snapshot.tenant_id || ':' || rebuilt_snapshot.user_id,
       now(),
       'system',
       'system'
FROM rebuilt_snapshot
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
