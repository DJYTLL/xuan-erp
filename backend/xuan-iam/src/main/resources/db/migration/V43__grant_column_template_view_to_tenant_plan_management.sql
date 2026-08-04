-- 套餐管理页需要读取平台列权限模板，用于把列权限模板直接绑定到套餐能力边界。
-- 因此拥有套餐维护能力的角色，也需要具备列权限模板只读能力。
INSERT INTO iam_role_permission (
    tenant_id, role_id, permission_id, created_by, updated_by
)
SELECT DISTINCT role_permission.tenant_id,
       role_permission.role_id,
       column_view_permission.id,
       COALESCE(role_permission.created_by, 'system-v43'),
       'system-v43'
FROM iam_role_permission role_permission
JOIN iam_permission tenant_plan_manage_permission
  ON tenant_plan_manage_permission.id = role_permission.permission_id
 AND tenant_plan_manage_permission.code = 'tenant-plan:manage'
JOIN iam_permission column_view_permission
  ON column_view_permission.code = 'iam-column-permission:view'
WHERE role_permission.deleted_at IS NULL
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v43',
    updated_at = now();

INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, created_by, updated_by
)
SELECT DISTINCT entitlement.tenant_id,
       entitlement.init_template_code,
       column_view_permission.id,
       entitlement.entitlement_version,
       COALESCE(entitlement.created_by, 'system-v43'),
       'system-v43'
FROM iam_tenant_permission_entitlement entitlement
JOIN iam_permission tenant_plan_manage_permission
  ON tenant_plan_manage_permission.id = entitlement.permission_id
 AND tenant_plan_manage_permission.code = 'tenant-plan:manage'
JOIN iam_permission column_view_permission
  ON column_view_permission.code = 'iam-column-permission:view'
WHERE entitlement.deleted_at IS NULL
ON CONFLICT (tenant_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET init_template_code = EXCLUDED.init_template_code,
    entitlement_version = EXCLUDED.entitlement_version,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v43',
    updated_at = now();

UPDATE iam_tenant_init_permission_template template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(template.permission_codes) AS code
                UNION ALL
                SELECT 'iam-column-permission:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant-plan:manage'
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v43',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
    WHERE existing.code = 'tenant-plan:manage'
);

UPDATE iam_tenant_init_role_template role_template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(role_template.permission_codes) AS code
                UNION ALL
                SELECT 'iam-column-permission:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant-plan:manage'
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v43',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
    WHERE existing.code = 'tenant-plan:manage'
);

UPDATE iam_authorization_snapshot snapshot
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT 'iam-column-permission:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code = 'tenant-plan:manage'
                )
            ) expanded
        ) distinct_code
    ),
    auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'permissions-v43:' || snapshot.tenant_id || ':' || snapshot.user_id,
    updated_by = 'system-v43',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
    WHERE existing.code = 'tenant-plan:manage'
);
