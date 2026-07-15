-- Add IAM admin management menus. This migration is append-only: V1-V4 history remains unchanged.

WITH system_menu AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
),
admin_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('iam-menu-management', '菜单管理', 'menu.iamMenus', '/system/iam/menus', 'ListTree', 'iam:view', 121),
        ('iam-permission-management', '权限管理', 'menu.iamPermissions', '/system/iam/permissions', 'KeyRound', 'iam:view', 122),
        ('iam-role-management', '角色授权', 'menu.iamRoles', '/system/iam/roles', 'ShieldCheck', 'iam:view', 123)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT admin_menu.code,
       system_menu.id,
       admin_menu.title,
       admin_menu.i18n_key,
       admin_menu.path,
       admin_menu.icon,
       admin_menu.permission_code,
       admin_menu.sort_no,
       true,
       'system',
       'system'
FROM admin_menu
CROSS JOIN system_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = admin_menu.code
      AND existing.deleted_at IS NULL
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
    3,
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
    'super_admin:iam-admin-management-menus:' || resolved_user.id,
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
