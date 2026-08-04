-- 为用户管理页补充独立的重置密码权限，避免把敏感操作混在普通用户资料修改权限中。
INSERT INTO iam_permission (
    code, name, service_name, menu_code, description, is_enabled, created_by, updated_by
)
VALUES (
    'iam-user:reset-password',
    '用户授权重置密码',
    'xuan-iam',
    'iam-user-management',
    '重置租户内 IAM 用户密码',
    true,
    'system-v32',
    'system-v32'
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
    updated_by = 'system-v32',
    updated_at = now();

INSERT INTO iam_role_permission (
    tenant_id, role_id, permission_id, created_by, updated_by
)
SELECT DISTINCT role_permission.tenant_id,
       role_permission.role_id,
       reset_permission.id,
       COALESCE(role_permission.created_by, 'system-v32'),
       'system-v32'
FROM iam_role_permission role_permission
JOIN iam_permission update_permission
  ON update_permission.id = role_permission.permission_id
 AND update_permission.code = 'iam-user:update'
JOIN iam_permission reset_permission
  ON reset_permission.code = 'iam-user:reset-password'
WHERE role_permission.deleted_at IS NULL
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v32',
    updated_at = now();

INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, created_by, updated_by
)
SELECT DISTINCT entitlement.tenant_id,
       entitlement.init_template_code,
       reset_permission.id,
       entitlement.entitlement_version,
       COALESCE(entitlement.created_by, 'system-v32'),
       'system-v32'
FROM iam_tenant_permission_entitlement entitlement
JOIN iam_permission update_permission
  ON update_permission.id = entitlement.permission_id
 AND update_permission.code = 'iam-user:update'
JOIN iam_permission reset_permission
  ON reset_permission.code = 'iam-user:reset-password'
WHERE entitlement.deleted_at IS NULL
ON CONFLICT (tenant_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET init_template_code = EXCLUDED.init_template_code,
    entitlement_version = EXCLUDED.entitlement_version,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v32',
    updated_at = now();

UPDATE iam_tenant_init_permission_template template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(template.permission_codes) AS code
                UNION ALL
                SELECT 'iam-user:reset-password'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                    WHERE existing.code = 'iam-user:update'
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v32',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
    WHERE existing.code = 'iam-user:update'
);

UPDATE iam_tenant_init_role_template role_template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(role_template.permission_codes) AS code
                UNION ALL
                SELECT 'iam-user:reset-password'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                    WHERE existing.code = 'iam-user:update'
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v32',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
    WHERE existing.code = 'iam-user:update'
);

UPDATE iam_authorization_snapshot snapshot
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT 'iam-user:reset-password'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code = 'iam-user:update'
                )
            ) expanded
        ) distinct_code
    ),
    auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'permissions-v32:' || snapshot.tenant_id || ':' || snapshot.user_id,
    updated_by = 'system-v32',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
    WHERE existing.code = 'iam-user:update'
);
