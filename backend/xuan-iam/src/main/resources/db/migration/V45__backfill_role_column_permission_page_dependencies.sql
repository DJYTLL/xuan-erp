-- 角色列权限页面运行时依赖角色查询和列权限资源查询接口。
-- 历史授权中如果只给了 iam-role-column-permission:view/update，会导致菜单可见但接口 403。
WITH trigger_permission(code) AS (
    VALUES
        ('iam-role-column-permission:view'),
        ('iam-role-column-permission:update')
),
dependency_permission(code) AS (
    VALUES
        ('iam-role:view'),
        ('iam-column-permission:view')
),
role_dependency_grant AS (
    SELECT DISTINCT role_permission.tenant_id,
           role_permission.role_id,
           dependency.id AS permission_id,
           COALESCE(role_permission.created_by, 'system-v45') AS created_by
    FROM iam_role_permission role_permission
    JOIN iam_permission trigger
      ON trigger.id = role_permission.permission_id
    JOIN trigger_permission
      ON trigger_permission.code = trigger.code
    JOIN iam_permission dependency
      ON dependency.code IN (SELECT code FROM dependency_permission)
    WHERE role_permission.deleted_at IS NULL
)
INSERT INTO iam_role_permission (
    tenant_id, role_id, permission_id, created_by, updated_by
)
SELECT tenant_id, role_id, permission_id, created_by, 'system-v45'
FROM role_dependency_grant
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v45',
    updated_at = now();

WITH trigger_permission(code) AS (
    VALUES
        ('iam-role-column-permission:view'),
        ('iam-role-column-permission:update')
),
dependency_permission(code) AS (
    VALUES
        ('iam-role:view'),
        ('iam-column-permission:view')
),
tenant_dependency_entitlement AS (
    SELECT entitlement.tenant_id,
           MIN(entitlement.init_template_code) AS init_template_code,
           dependency.id AS permission_id,
           MAX(entitlement.entitlement_version) AS entitlement_version,
           MIN(COALESCE(entitlement.created_by, 'system-v45')) AS created_by
    FROM iam_tenant_permission_entitlement entitlement
    JOIN iam_permission trigger
      ON trigger.id = entitlement.permission_id
    JOIN trigger_permission
      ON trigger_permission.code = trigger.code
    JOIN iam_permission dependency
      ON dependency.code IN (SELECT code FROM dependency_permission)
    WHERE entitlement.deleted_at IS NULL
    GROUP BY entitlement.tenant_id, dependency.id
)
INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, created_by, updated_by
)
SELECT tenant_id, init_template_code, permission_id, entitlement_version, created_by, 'system-v45'
FROM tenant_dependency_entitlement
ON CONFLICT (tenant_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET init_template_code = EXCLUDED.init_template_code,
    entitlement_version = EXCLUDED.entitlement_version,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v45',
    updated_at = now();

UPDATE iam_tenant_init_permission_template template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(template.permission_codes) AS code
                UNION ALL
                SELECT dependency_code.code
                FROM (
                    VALUES
                        ('iam-role:view'),
                        ('iam-column-permission:view')
                ) AS dependency_code(code)
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                    WHERE existing.code IN ('iam-role-column-permission:view', 'iam-role-column-permission:update')
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v45',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
    WHERE existing.code IN ('iam-role-column-permission:view', 'iam-role-column-permission:update')
);

UPDATE iam_tenant_init_role_template role_template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(role_template.permission_codes) AS code
                UNION ALL
                SELECT dependency_code.code
                FROM (
                    VALUES
                        ('iam-role:view'),
                        ('iam-column-permission:view')
                ) AS dependency_code(code)
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                    WHERE existing.code IN ('iam-role-column-permission:view', 'iam-role-column-permission:update')
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v45',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
    WHERE existing.code IN ('iam-role-column-permission:view', 'iam-role-column-permission:update')
);

UPDATE iam_authorization_snapshot snapshot
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT dependency_code.code
                FROM (
                    VALUES
                        ('iam-role:view'),
                        ('iam-column-permission:view')
                ) AS dependency_code(code)
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code IN ('iam-role-column-permission:view', 'iam-role-column-permission:update')
                )
            ) expanded
        ) distinct_code
    ),
    auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'role-column-dependencies-v45:' || snapshot.tenant_id || ':' || snapshot.user_id,
    updated_by = 'system-v45',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
    WHERE existing.code IN ('iam-role-column-permission:view', 'iam-role-column-permission:update')
);
