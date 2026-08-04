-- 追加列权限模板管理页面、页面权限和默认授权；V33 已创建列权限业务表，本迁移不新增表结构。
WITH page_permission(code, name, menu_code, description) AS (
    VALUES
        ('iam-column-permission:view', '列权限模板查看', 'iam-column-permission-management', '查看列权限模板页面、资源字段和角色绑定'),
        ('iam-column-permission:create', '列权限模板新增', 'iam-column-permission-management', '新增列权限模板'),
        ('iam-column-permission:update', '列权限模板修改', 'iam-column-permission-management', '修改列权限模板、保存字段规则和绑定角色模板')
)
INSERT INTO iam_permission (
    code, name, service_name, menu_code, description, is_enabled, created_by, updated_by
)
SELECT code, name, 'xuan-iam', menu_code, description, true, 'system-v34', 'system-v34'
FROM page_permission
ON CONFLICT (code) WHERE deleted_at IS NULL DO UPDATE
SET name = EXCLUDED.name,
    service_name = EXCLUDED.service_name,
    menu_code = EXCLUDED.menu_code,
    description = EXCLUDED.description,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v34',
    updated_at = now();

WITH permission_center AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system-permission-center'
      AND deleted_at IS NULL
),
column_permission_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('iam-column-permission-management', '列权限模板', 'route.iamColumnPermissions', '/system/iam/column-permissions', 'Columns3', 'iam-column-permission:view', 35)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT
    column_permission_menu.code,
    permission_center.id,
    column_permission_menu.title,
    column_permission_menu.i18n_key,
    column_permission_menu.path,
    column_permission_menu.icon,
    column_permission_menu.permission_code,
    column_permission_menu.sort_no,
    true,
    'system-v34',
    'system-v34'
FROM column_permission_menu
CROSS JOIN permission_center
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = column_permission_menu.code
      AND existing.deleted_at IS NULL
);

WITH permission_center AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system-permission-center'
      AND deleted_at IS NULL
),
column_permission_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('iam-column-permission-management', '列权限模板', 'route.iamColumnPermissions', '/system/iam/column-permissions', 'Columns3', 'iam-column-permission:view', 35)
)
UPDATE iam_menu menu
SET parent_id = permission_center.id,
    title = column_permission_menu.title,
    i18n_key = column_permission_menu.i18n_key,
    path = column_permission_menu.path,
    icon = column_permission_menu.icon,
    permission_code = column_permission_menu.permission_code,
    sort_no = column_permission_menu.sort_no,
    is_enabled = true,
    updated_by = 'system-v34',
    updated_at = now()
FROM column_permission_menu
CROSS JOIN permission_center
WHERE menu.code = column_permission_menu.code
  AND menu.deleted_at IS NULL;

WITH source_grant(old_code, new_code) AS (
    VALUES
        ('iam-role:view', 'iam-column-permission:view'),
        ('iam-role:create', 'iam-column-permission:create'),
        ('iam-role:update', 'iam-column-permission:update')
),
role_grant AS (
    SELECT DISTINCT role_permission.tenant_id,
           role_permission.role_id,
           new_permission.id AS permission_id,
           COALESCE(role_permission.created_by, 'system-v34') AS created_by
    FROM iam_role_permission role_permission
    JOIN iam_permission old_permission
      ON old_permission.id = role_permission.permission_id
    JOIN source_grant
      ON source_grant.old_code = old_permission.code
    JOIN iam_permission new_permission
      ON new_permission.code = source_grant.new_code
    WHERE role_permission.deleted_at IS NULL
)
INSERT INTO iam_role_permission (
    tenant_id, role_id, permission_id, created_by, updated_by
)
SELECT tenant_id, role_id, permission_id, created_by, 'system-v34'
FROM role_grant
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v34',
    updated_at = now();

WITH source_entitlement(old_code, new_code) AS (
    VALUES
        ('iam-role:view', 'iam-column-permission:view'),
        ('iam-role:create', 'iam-column-permission:create'),
        ('iam-role:update', 'iam-column-permission:update')
),
tenant_entitlement AS (
    SELECT DISTINCT entitlement.tenant_id,
           entitlement.init_template_code,
           new_permission.id AS permission_id,
           entitlement.entitlement_version,
           COALESCE(entitlement.created_by, 'system-v34') AS created_by
    FROM iam_tenant_permission_entitlement entitlement
    JOIN iam_permission old_permission
      ON old_permission.id = entitlement.permission_id
    JOIN source_entitlement
      ON source_entitlement.old_code = old_permission.code
    JOIN iam_permission new_permission
      ON new_permission.code = source_entitlement.new_code
    WHERE entitlement.deleted_at IS NULL
)
INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, created_by, updated_by
)
SELECT tenant_id, init_template_code, permission_id, entitlement_version, created_by, 'system-v34'
FROM tenant_entitlement
ON CONFLICT (tenant_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET init_template_code = EXCLUDED.init_template_code,
    entitlement_version = EXCLUDED.entitlement_version,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v34',
    updated_at = now();

UPDATE iam_tenant_init_permission_template template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(template.permission_codes) AS code
                UNION ALL
                SELECT source_permission.new_code
                FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                JOIN (
                    VALUES
                        ('iam-role:view', 'iam-column-permission:view'),
                        ('iam-role:create', 'iam-column-permission:create'),
                        ('iam-role:update', 'iam-column-permission:update')
                ) AS source_permission(old_code, new_code)
                  ON source_permission.old_code = existing.code
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v34',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
    WHERE existing.code IN ('iam-role:view', 'iam-role:create', 'iam-role:update')
);

UPDATE iam_tenant_init_role_template role_template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(role_template.permission_codes) AS code
                UNION ALL
                SELECT source_permission.new_code
                FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                JOIN (
                    VALUES
                        ('iam-role:view', 'iam-column-permission:view'),
                        ('iam-role:create', 'iam-column-permission:create'),
                        ('iam-role:update', 'iam-column-permission:update')
                ) AS source_permission(old_code, new_code)
                  ON source_permission.old_code = existing.code
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v34',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
    WHERE existing.code IN ('iam-role:view', 'iam-role:create', 'iam-role:update')
);

INSERT INTO iam_tenant_menu (tenant_id, menu_id, is_enabled, created_by, updated_by)
SELECT DISTINCT tenant_menu.tenant_id,
       menu.id,
       true,
       'system-v34',
       'system-v34'
FROM iam_tenant_menu tenant_menu
CROSS JOIN iam_menu menu
WHERE tenant_menu.deleted_at IS NULL
  AND menu.code = 'iam-column-permission-management'
  AND menu.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_tenant_menu existing
      WHERE existing.tenant_id = tenant_menu.tenant_id
        AND existing.menu_id = menu.id
        AND existing.deleted_at IS NULL
  );

UPDATE iam_authorization_snapshot snapshot
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT source_permission.new_code
                FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                JOIN (
                    VALUES
                        ('iam-role:view', 'iam-column-permission:view'),
                        ('iam-role:create', 'iam-column-permission:create'),
                        ('iam-role:update', 'iam-column-permission:update')
                ) AS source_permission(old_code, new_code)
                  ON source_permission.old_code = existing.code
            ) expanded
        ) distinct_code
    ),
    menu_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(snapshot.menu_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT 'iam-column-permission-management'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code = 'iam-role:view'
                )
            ) expanded
        ) distinct_code
    ),
    auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'permissions-v34:' || snapshot.tenant_id || ':' || snapshot.user_id,
    updated_by = 'system-v34',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
    WHERE existing.code IN ('iam-role:view', 'iam-role:create', 'iam-role:update')
);
