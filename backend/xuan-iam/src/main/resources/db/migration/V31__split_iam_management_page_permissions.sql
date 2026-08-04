-- 将系统设置中的 IAM 管理页拆成页面级权限，避免多个页面共用 iam:* 造成授权外溢。
WITH page_permission(code, name, menu_code, description) AS (
    VALUES
        ('component-center:view', '组件中心查看', 'components', '查看组件中心页面'),
        ('iam-menu:view', '菜单管理查看', 'iam-menu-management', '查看菜单管理页面和菜单清单'),
        ('iam-menu:create', '菜单管理新增', 'iam-menu-management', '新增菜单和导航分组'),
        ('iam-menu:update', '菜单管理修改', 'iam-menu-management', '修改、启用或停用菜单'),
        ('iam-permission:view', '权限管理查看', 'iam-permission-management', '查看权限管理页面和权限清单'),
        ('iam-permission:create', '权限管理新增', 'iam-permission-management', '新增权限定义'),
        ('iam-permission:update', '权限管理修改', 'iam-permission-management', '修改、启用或停用权限定义'),
        ('iam-role:view', '角色授权查看', 'iam-role-management', '查看角色授权页面、角色列表和角色权限'),
        ('iam-role:create', '角色授权新增', 'iam-role-management', '新增角色'),
        ('iam-role:update', '角色授权修改', 'iam-role-management', '修改角色、启停角色和保存角色权限'),
        ('iam-user:view', '用户授权查看', 'iam-user-management', '查看用户授权页面、用户列表和用户角色'),
        ('iam-user:create', '用户授权新增', 'iam-user-management', '新增用户'),
        ('iam-user:update', '用户授权修改', 'iam-user-management', '保存用户角色'),
        ('iam-user:delete', '用户授权删除', 'iam-user-management', '停用用户'),
        ('iam-init-template:view', '初始化模板查看', 'iam-init-template-management', '查看初始化模板页面和模板清单'),
        ('iam-init-template:create', '初始化模板新增', 'iam-init-template-management', '新增初始化模板'),
        ('iam-init-template:update', '初始化模板修改', 'iam-init-template-management', '修改初始化模板和保存模板权限')
)
INSERT INTO iam_permission (
    code, name, service_name, menu_code, description, is_enabled, created_by, updated_by
)
SELECT code, name, 'xuan-iam', menu_code, description, true, 'system-v31', 'system-v31'
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
    updated_by = 'system-v31',
    updated_at = now();

WITH menu_permission(code, permission_code) AS (
    VALUES
        ('components', 'component-center:view'),
        ('iam-menu-management', 'iam-menu:view'),
        ('iam-permission-management', 'iam-permission:view'),
        ('iam-role-management', 'iam-role:view'),
        ('iam-user-management', 'iam-user:view'),
        ('iam-init-template-management', 'iam-init-template:view')
)
UPDATE iam_menu menu
SET permission_code = menu_permission.permission_code,
    updated_by = 'system-v31',
    updated_at = now()
FROM menu_permission
WHERE menu.code = menu_permission.code;

UPDATE iam_menu
SET permission_code = NULL,
    updated_by = 'system-v31',
    updated_at = now()
WHERE code = 'system'
  AND permission_code = 'iam:view';

WITH permission_alias(old_code, new_code) AS (
    VALUES
        ('iam:view', 'component-center:view'),
        ('iam:view', 'iam-menu:view'),
        ('iam:create', 'iam-menu:create'),
        ('iam:update', 'iam-menu:update'),
        ('iam:view', 'iam-permission:view'),
        ('iam:create', 'iam-permission:create'),
        ('iam:update', 'iam-permission:update'),
        ('iam:view', 'iam-role:view'),
        ('iam:create', 'iam-role:create'),
        ('iam:update', 'iam-role:update'),
        ('iam:view', 'iam-user:view'),
        ('iam:create', 'iam-user:create'),
        ('iam:update', 'iam-user:update'),
        ('iam:delete', 'iam-user:delete'),
        ('iam:view', 'iam-init-template:view'),
        ('iam:create', 'iam-init-template:create'),
        ('iam:update', 'iam-init-template:update')
),
source_grant AS (
    SELECT DISTINCT role_permission.tenant_id,
           role_permission.role_id,
           new_permission.id AS permission_id,
           COALESCE(role_permission.created_by, 'system-v31') AS created_by
    FROM iam_role_permission role_permission
    JOIN iam_permission old_permission
      ON old_permission.id = role_permission.permission_id
     AND old_permission.code IN ('iam:view', 'iam:create', 'iam:update', 'iam:delete')
    JOIN permission_alias alias_map
      ON alias_map.old_code = old_permission.code
    JOIN iam_permission new_permission
      ON new_permission.code = alias_map.new_code
    WHERE role_permission.deleted_at IS NULL
)
INSERT INTO iam_role_permission (
    tenant_id, role_id, permission_id, created_by, updated_by
)
SELECT tenant_id, role_id, permission_id, created_by, 'system-v31'
FROM source_grant
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v31',
    updated_at = now();

WITH permission_alias(old_code, new_code) AS (
    VALUES
        ('iam:view', 'component-center:view'),
        ('iam:view', 'iam-menu:view'),
        ('iam:create', 'iam-menu:create'),
        ('iam:update', 'iam-menu:update'),
        ('iam:view', 'iam-permission:view'),
        ('iam:create', 'iam-permission:create'),
        ('iam:update', 'iam-permission:update'),
        ('iam:view', 'iam-role:view'),
        ('iam:create', 'iam-role:create'),
        ('iam:update', 'iam-role:update'),
        ('iam:view', 'iam-user:view'),
        ('iam:create', 'iam-user:create'),
        ('iam:update', 'iam-user:update'),
        ('iam:delete', 'iam-user:delete'),
        ('iam:view', 'iam-init-template:view'),
        ('iam:create', 'iam-init-template:create'),
        ('iam:update', 'iam-init-template:update')
),
tenant_entitlement_source AS (
    SELECT DISTINCT entitlement.tenant_id,
           entitlement.init_template_code,
           new_permission.id AS permission_id,
           entitlement.entitlement_version,
           COALESCE(entitlement.created_by, 'system-v31') AS created_by
    FROM iam_tenant_permission_entitlement entitlement
    JOIN iam_permission old_permission
      ON old_permission.id = entitlement.permission_id
     AND old_permission.code IN ('iam:view', 'iam:create', 'iam:update', 'iam:delete')
    JOIN permission_alias alias_map
      ON alias_map.old_code = old_permission.code
    JOIN iam_permission new_permission
      ON new_permission.code = alias_map.new_code
    WHERE entitlement.deleted_at IS NULL
)
INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, created_by, updated_by
)
SELECT tenant_id, init_template_code, permission_id, entitlement_version, created_by, 'system-v31'
FROM tenant_entitlement_source
ON CONFLICT (tenant_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET init_template_code = EXCLUDED.init_template_code,
    entitlement_version = EXCLUDED.entitlement_version,
    updated_by = 'system-v31',
    updated_at = now();

UPDATE iam_tenant_init_permission_template template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(template.permission_codes) AS code
                UNION ALL
                SELECT alias_map.new_code
                FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                JOIN (
                    VALUES
                        ('iam:view', 'component-center:view'),
                        ('iam:view', 'iam-menu:view'),
                        ('iam:create', 'iam-menu:create'),
                        ('iam:update', 'iam-menu:update'),
                        ('iam:view', 'iam-permission:view'),
                        ('iam:create', 'iam-permission:create'),
                        ('iam:update', 'iam-permission:update'),
                        ('iam:view', 'iam-role:view'),
                        ('iam:create', 'iam-role:create'),
                        ('iam:update', 'iam-role:update'),
                        ('iam:view', 'iam-user:view'),
                        ('iam:create', 'iam-user:create'),
                        ('iam:update', 'iam-user:update'),
                        ('iam:delete', 'iam-user:delete'),
                        ('iam:view', 'iam-init-template:view'),
                        ('iam:create', 'iam-init-template:create'),
                        ('iam:update', 'iam-init-template:update')
                ) AS alias_map(old_code, new_code)
                  ON alias_map.old_code = existing.code
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v31',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
    WHERE existing.code IN ('iam:view', 'iam:create', 'iam:update', 'iam:delete')
);

UPDATE iam_tenant_init_role_template role_template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(role_template.permission_codes) AS code
                UNION ALL
                SELECT alias_map.new_code
                FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                JOIN (
                    VALUES
                        ('iam:view', 'component-center:view'),
                        ('iam:view', 'iam-menu:view'),
                        ('iam:create', 'iam-menu:create'),
                        ('iam:update', 'iam-menu:update'),
                        ('iam:view', 'iam-permission:view'),
                        ('iam:create', 'iam-permission:create'),
                        ('iam:update', 'iam-permission:update'),
                        ('iam:view', 'iam-role:view'),
                        ('iam:create', 'iam-role:create'),
                        ('iam:update', 'iam-role:update'),
                        ('iam:view', 'iam-user:view'),
                        ('iam:create', 'iam-user:create'),
                        ('iam:update', 'iam-user:update'),
                        ('iam:delete', 'iam-user:delete'),
                        ('iam:view', 'iam-init-template:view'),
                        ('iam:create', 'iam-init-template:create'),
                        ('iam:update', 'iam-init-template:update')
                ) AS alias_map(old_code, new_code)
                  ON alias_map.old_code = existing.code
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v31',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
    WHERE existing.code IN ('iam:view', 'iam:create', 'iam:update', 'iam:delete')
);

UPDATE iam_authorization_snapshot snapshot
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT alias_map.new_code
                FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                JOIN (
                    VALUES
                        ('iam:view', 'component-center:view'),
                        ('iam:view', 'iam-menu:view'),
                        ('iam:create', 'iam-menu:create'),
                        ('iam:update', 'iam-menu:update'),
                        ('iam:view', 'iam-permission:view'),
                        ('iam:create', 'iam-permission:create'),
                        ('iam:update', 'iam-permission:update'),
                        ('iam:view', 'iam-role:view'),
                        ('iam:create', 'iam-role:create'),
                        ('iam:update', 'iam-role:update'),
                        ('iam:view', 'iam-user:view'),
                        ('iam:create', 'iam-user:create'),
                        ('iam:update', 'iam-user:update'),
                        ('iam:delete', 'iam-user:delete'),
                        ('iam:view', 'iam-init-template:view'),
                        ('iam:create', 'iam-init-template:create'),
                        ('iam:update', 'iam-init-template:update')
                ) AS alias_map(old_code, new_code)
                  ON alias_map.old_code = existing.code
            ) expanded
        ) distinct_code
    ),
    auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'permissions-v31:' || snapshot.tenant_id || ':' || snapshot.user_id,
    updated_by = 'system-v31',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
    WHERE existing.code IN ('iam:view', 'iam:create', 'iam:update', 'iam:delete')
);
