-- Add IAM user management menu. This migration is append-only: V1-V6 history remains unchanged.

WITH system_menu AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
),
user_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('iam-user-management', '用户授权', 'menu.iamUsers', '/system/iam/users', 'Users', 'iam:view', 124)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT user_menu.code,
       system_menu.id,
       user_menu.title,
       user_menu.i18n_key,
       user_menu.path,
       user_menu.icon,
       user_menu.permission_code,
       user_menu.sort_no,
       true,
       'system',
       'system'
FROM user_menu
CROSS JOIN system_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = user_menu.code
      AND existing.deleted_at IS NULL
);

UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    menu_codes = COALESCE(snapshot.menu_codes, '[]'::jsonb) || jsonb_build_array('iam-user-management'),
    snapshot_hash = snapshot.snapshot_hash || '|iam-user-management',
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
WHERE (snapshot.permission_codes ? 'iam:view' OR snapshot.permission_codes ? '*')
  AND NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? 'iam-user-management');
