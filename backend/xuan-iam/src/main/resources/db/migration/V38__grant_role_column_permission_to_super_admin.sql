-- 修复 super_admin 角色缺少独立角色列权限页面授权的问题。
-- V37 只从旧列权限模板权限复制授权；如果 super_admin 没有旧权限，就会漏掉新页面。

WITH role_column_permission(code) AS (
    VALUES
        ('iam-role-column-permission:view'),
        ('iam-role-column-permission:update')
),
super_admin_grant AS (
    SELECT role.tenant_id,
           role.id AS role_id,
           permission.id AS permission_id
    FROM iam_role role
    JOIN role_column_permission required_permission ON true
    JOIN iam_permission permission
      ON permission.code = required_permission.code
     AND permission.deleted_at IS NULL
     AND permission.is_enabled = true
    WHERE role.code = 'super_admin'
      AND role.deleted_at IS NULL
      AND role.is_enabled = true
)
INSERT INTO iam_role_permission (
    tenant_id, role_id, permission_id, created_by, updated_by
)
SELECT tenant_id,
       role_id,
       permission_id,
       'system-v38',
       'system-v38'
FROM super_admin_grant
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v38',
    updated_at = now();

UPDATE iam_tenant_init_role_template role_template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(role_template.permission_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT 'iam-role-column-permission:view'
                UNION ALL
                SELECT 'iam-role-column-permission:update'
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v38',
    updated_at = now()
WHERE role_template.role_code = 'super_admin'
  AND role_template.deleted_at IS NULL;

WITH patched_snapshot AS (
    SELECT snapshot.id,
           (
               SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
               FROM (
                   SELECT DISTINCT code
                   FROM (
                       SELECT jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) AS code
                       UNION ALL
                       SELECT 'iam-role-column-permission:view'
                       UNION ALL
                       SELECT 'iam-role-column-permission:update'
                   ) expanded_permission
               ) distinct_permission
           ) AS permission_codes,
           (
               SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
               FROM (
                   SELECT DISTINCT code
                   FROM (
                       SELECT jsonb_array_elements_text(COALESCE(snapshot.menu_codes, '[]'::jsonb)) AS code
                       UNION ALL
                       SELECT 'iam-role-column-permission-management'
                   ) expanded_menu
               ) distinct_menu
           ) AS menu_codes
    FROM iam_authorization_snapshot snapshot
    WHERE EXISTS (
            SELECT 1
            FROM jsonb_array_elements_text(COALESCE(snapshot.role_ids, '[]'::jsonb)) AS role_id(value)
            JOIN iam_role role
              ON role.id = role_id.value::bigint
             AND role.tenant_id = snapshot.tenant_id
             AND role.code = 'super_admin'
             AND role.deleted_at IS NULL
             AND role.is_enabled = true
        )
       OR EXISTS (
            SELECT 1
            FROM iam_user_role user_role
            JOIN iam_role role
              ON role.tenant_id = user_role.tenant_id
             AND role.id = user_role.role_id
             AND role.code = 'super_admin'
             AND role.deleted_at IS NULL
             AND role.is_enabled = true
            WHERE user_role.tenant_id = snapshot.tenant_id
              AND user_role.user_id = snapshot.user_id
              AND user_role.deleted_at IS NULL
        )
)
UPDATE iam_authorization_snapshot snapshot
SET permission_codes = patched_snapshot.permission_codes,
    menu_codes = patched_snapshot.menu_codes,
    auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'super-admin-role-column-permission-v38:' || snapshot.tenant_id || ':' || snapshot.user_id,
    built_at = now(),
    updated_by = 'system-v38',
    updated_at = now()
FROM patched_snapshot
WHERE snapshot.id = patched_snapshot.id;
