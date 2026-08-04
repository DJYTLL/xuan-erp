-- 为租户管理页补充独立的重置租户 admin 管理员密码权限，并统一“用户授权”为“用户管理”。
INSERT INTO iam_permission (
    code, name, service_name, menu_code, description, is_enabled, created_by, updated_by
)
VALUES (
    'tenant:admin-password:reset',
    '租户管理员密码重置',
    'xuan-iam',
    'tenant-management',
    '在租户管理页重置指定租户 admin 管理员账号密码',
    true,
    'system-v42',
    'system-v42'
)
ON CONFLICT (code) WHERE deleted_at IS NULL DO UPDATE
SET name = EXCLUDED.name,
    service_name = EXCLUDED.service_name,
    menu_code = EXCLUDED.menu_code,
    description = EXCLUDED.description,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v42',
    updated_at = now();

UPDATE iam_menu
SET title = '用户管理',
    updated_by = 'system-v42',
    updated_at = now()
WHERE code = 'iam-user-management'
  AND deleted_at IS NULL;

UPDATE iam_permission
SET name = REPLACE(name, '用户授权', '用户管理'),
    description = REPLACE(description, '用户授权', '用户管理'),
    updated_by = 'system-v42',
    updated_at = now()
WHERE menu_code = 'iam-user-management'
  AND deleted_at IS NULL
  AND (name LIKE '%用户授权%' OR description LIKE '%用户授权%');

INSERT INTO iam_role_permission (
    tenant_id, role_id, permission_id, created_by, updated_by
)
SELECT DISTINCT role_permission.tenant_id,
       role_permission.role_id,
       reset_permission.id,
       COALESCE(role_permission.created_by, 'system-v42'),
       'system-v42'
FROM iam_role_permission role_permission
JOIN iam_permission update_permission
  ON update_permission.id = role_permission.permission_id
 AND update_permission.code = 'tenant:update'
JOIN iam_permission reset_permission
  ON reset_permission.code = 'tenant:admin-password:reset'
WHERE role_permission.deleted_at IS NULL
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v42',
    updated_at = now();

INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, created_by, updated_by
)
SELECT DISTINCT entitlement.tenant_id,
       entitlement.init_template_code,
       reset_permission.id,
       entitlement.entitlement_version,
       COALESCE(entitlement.created_by, 'system-v42'),
       'system-v42'
FROM iam_tenant_permission_entitlement entitlement
JOIN iam_permission update_permission
  ON update_permission.id = entitlement.permission_id
 AND update_permission.code = 'tenant:update'
JOIN iam_permission reset_permission
  ON reset_permission.code = 'tenant:admin-password:reset'
WHERE entitlement.deleted_at IS NULL
ON CONFLICT (tenant_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET init_template_code = EXCLUDED.init_template_code,
    entitlement_version = EXCLUDED.entitlement_version,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v42',
    updated_at = now();

UPDATE iam_tenant_init_permission_template template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(template.permission_codes) AS code
                UNION ALL
                SELECT 'tenant:admin-password:reset'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v42',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
    WHERE existing.code = 'tenant:update'
);

UPDATE iam_tenant_init_role_template role_template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(role_template.permission_codes) AS code
                UNION ALL
                SELECT 'tenant:admin-password:reset'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v42',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
    WHERE existing.code = 'tenant:update'
);

UPDATE iam_authorization_snapshot snapshot
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT 'tenant:admin-password:reset'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
            ) expanded
        ) distinct_code
    ),
    auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'permissions-v42:' || snapshot.tenant_id || ':' || snapshot.user_id,
    updated_by = 'system-v42',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
    WHERE existing.code = 'tenant:update'
);
