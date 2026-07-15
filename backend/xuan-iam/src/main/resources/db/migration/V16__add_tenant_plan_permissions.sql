-- Add dedicated tenant plan permissions and align the tenant plan menu.
-- This migration is append-only: V1-V15 history remains unchanged.

WITH tenant_plan_permission(code, name, service_name, menu_code, description) AS (
    VALUES
        ('tenant-plan:view', '租户套餐查看', 'xuan-tenant', 'tenant-plan-management', '查看租户套餐和套餐分配'),
        ('tenant-plan:manage', '租户套餐管理', 'xuan-tenant', 'tenant-plan-management', '创建、修改、启用、停用和删除租户套餐'),
        ('tenant-plan:assign', '租户套餐分配', 'xuan-tenant', 'tenant-plan-management', '维护租户套餐分配关系')
)
INSERT INTO iam_permission (code, name, service_name, menu_code, description, is_enabled, created_by, updated_by)
SELECT tenant_plan_permission.code,
       tenant_plan_permission.name,
       tenant_plan_permission.service_name,
       tenant_plan_permission.menu_code,
       tenant_plan_permission.description,
       true,
       'system',
       'system'
FROM tenant_plan_permission
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_permission existing
    WHERE existing.code = tenant_plan_permission.code
      AND existing.deleted_at IS NULL
);

UPDATE iam_menu
SET permission_code = 'tenant-plan:view',
    updated_by = 'system',
    updated_at = now()
WHERE code = 'tenant-plan-management'
  AND deleted_at IS NULL;

WITH tenant_plan_permission(code) AS (
    VALUES
        ('tenant-plan:view'),
        ('tenant-plan:manage'),
        ('tenant-plan:assign')
)
INSERT INTO iam_role_permission (tenant_id, role_id, permission_id, created_by, updated_by)
SELECT role.tenant_id,
       role.id,
       permission.id,
       'system',
       'system'
FROM iam_role role
CROSS JOIN tenant_plan_permission
JOIN iam_permission permission ON permission.code = tenant_plan_permission.code
WHERE role.code IN ('tenant_admin', 'super_admin')
  AND role.deleted_at IS NULL
  AND role.is_enabled = true
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

WITH tenant_plan_permission(code) AS (
    VALUES
        ('tenant-plan:view'),
        ('tenant-plan:manage'),
        ('tenant-plan:assign')
)
UPDATE iam_tenant_init_permission_template template
SET permission_codes = COALESCE(template.permission_codes, '[]'::jsonb)
    || COALESCE((
        SELECT jsonb_agg(tenant_plan_permission.code ORDER BY tenant_plan_permission.code)
        FROM tenant_plan_permission
        WHERE NOT (COALESCE(template.permission_codes, '[]'::jsonb) ? tenant_plan_permission.code)
    ), '[]'::jsonb),
    updated_by = 'system',
    updated_at = now()
WHERE template.code = 'full'
  AND template.deleted_at IS NULL;

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
           ), '[]'::jsonb) AS permission_codes,
           COALESCE((
               SELECT jsonb_agg(code ORDER BY sort_no, code)
               FROM (
                   SELECT DISTINCT menu.code, menu.sort_no
                   FROM iam_tenant_menu tenant_menu
                   JOIN iam_menu menu
                     ON menu.id = tenant_menu.menu_id
                    AND menu.deleted_at IS NULL
                    AND menu.is_enabled = true
                   WHERE tenant_menu.tenant_id = snapshot.tenant_id
                     AND tenant_menu.deleted_at IS NULL
               ) active_menu
           ), '[]'::jsonb) AS menu_codes
    FROM iam_authorization_snapshot snapshot
    WHERE snapshot.tenant_id <> 0
)
UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    permission_codes = rebuilt_snapshot.permission_codes,
    menu_codes = rebuilt_snapshot.menu_codes,
    snapshot_hash = 'permissions-v16:' || snapshot.tenant_id || ':' || snapshot.user_id,
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM rebuilt_snapshot
WHERE snapshot.id = rebuilt_snapshot.id;

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
    16,
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
    'super_admin:tenant-plan-permissions:' || resolved_user.id,
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
