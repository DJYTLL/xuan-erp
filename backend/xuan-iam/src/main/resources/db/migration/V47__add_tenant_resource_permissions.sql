-- 补齐租户域名、联系人和通用资源控制器的最终权限码归属。
-- 域名、联系人不再复用 tenant:view / tenant:update，而是切到独立资源权限；
-- 同时平滑回填历史角色、租户权限池、初始化模板和授权快照，避免升级后旧租户失权。

INSERT INTO iam_permission (
    code, name, service_name, menu_code, description, is_enabled, created_by, updated_by
)
VALUES
    ('tenant-domain:view', '租户域名查看', 'xuan-tenant', 'tenant-management', '查看租户域名、验证状态和主域名信息', true, 'system-v47', 'system-v47'),
    ('tenant-domain:manage', '租户域名管理', 'xuan-tenant', 'tenant-management', '创建、修改、验证和删除租户域名', true, 'system-v47', 'system-v47'),
    ('tenant-contact:view', '租户联系人查看', 'xuan-tenant', 'tenant-management', '查看租户联系人、主联系人标记和联系方式', true, 'system-v47', 'system-v47'),
    ('tenant-contact:manage', '租户联系人管理', 'xuan-tenant', 'tenant-management', '创建、修改和删除租户联系人', true, 'system-v47', 'system-v47')
ON CONFLICT (code) WHERE deleted_at IS NULL DO UPDATE
SET name = EXCLUDED.name,
    service_name = EXCLUDED.service_name,
    menu_code = EXCLUDED.menu_code,
    description = EXCLUDED.description,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v47',
    updated_at = now();

UPDATE iam_permission
SET description = CASE code
                      WHEN 'tenant:view' THEN '查看租户主档、生命周期历史和下拉引用数据'
                      WHEN 'tenant:update' THEN '修改租户基础资料，不含域名、联系人和配置写操作'
                      WHEN 'tenant-config:view' THEN '查看租户配置和公开配置'
                      WHEN 'tenant-config:manage' THEN '创建、修改和删除租户配置'
                      ELSE description
                  END,
    updated_by = 'system-v47',
    updated_at = now()
WHERE code IN ('tenant:view', 'tenant:update', 'tenant-config:view', 'tenant-config:manage')
  AND deleted_at IS NULL;

WITH permission_mapping(source_code, mapped_code) AS (
    VALUES
        ('tenant:view', 'tenant-domain:view'),
        ('tenant:view', 'tenant-contact:view'),
        ('tenant:update', 'tenant-domain:manage'),
        ('tenant:update', 'tenant-contact:manage')
)
INSERT INTO iam_role_permission (
    tenant_id, role_id, permission_id, created_by, updated_by
)
SELECT DISTINCT role_permission.tenant_id,
       role_permission.role_id,
       mapped_permission.id,
       COALESCE(role_permission.created_by, 'system-v47'),
       'system-v47'
FROM iam_role_permission role_permission
JOIN iam_permission source_permission
  ON source_permission.id = role_permission.permission_id
JOIN permission_mapping
  ON permission_mapping.source_code = source_permission.code
JOIN iam_permission mapped_permission
  ON mapped_permission.code = permission_mapping.mapped_code
WHERE role_permission.deleted_at IS NULL
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v47',
    updated_at = now();

WITH permission_mapping(source_code, mapped_code) AS (
    VALUES
        ('tenant:view', 'tenant-domain:view'),
        ('tenant:view', 'tenant-contact:view'),
        ('tenant:update', 'tenant-domain:manage'),
        ('tenant:update', 'tenant-contact:manage')
)
INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, created_by, updated_by
)
SELECT DISTINCT entitlement.tenant_id,
       entitlement.init_template_code,
       mapped_permission.id,
       entitlement.entitlement_version,
       COALESCE(entitlement.created_by, 'system-v47'),
       'system-v47'
FROM iam_tenant_permission_entitlement entitlement
JOIN iam_permission source_permission
  ON source_permission.id = entitlement.permission_id
JOIN permission_mapping
  ON permission_mapping.source_code = source_permission.code
JOIN iam_permission mapped_permission
  ON mapped_permission.code = permission_mapping.mapped_code
WHERE entitlement.deleted_at IS NULL
ON CONFLICT (tenant_id, permission_id) WHERE deleted_at IS NULL DO UPDATE
SET init_template_code = EXCLUDED.init_template_code,
    entitlement_version = EXCLUDED.entitlement_version,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v47',
    updated_at = now();

UPDATE iam_tenant_init_permission_template template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(template.permission_codes) AS code
                UNION ALL
                SELECT 'tenant-domain:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:view'
                )
                UNION ALL
                SELECT 'tenant-contact:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:view'
                )
                UNION ALL
                SELECT 'tenant-domain:manage'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
                UNION ALL
                SELECT 'tenant-contact:manage'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v47',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(template.permission_codes) existing(code)
    WHERE existing.code IN ('tenant:view', 'tenant:update')
);

UPDATE iam_tenant_init_role_template role_template
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(role_template.permission_codes) AS code
                UNION ALL
                SELECT 'tenant-domain:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:view'
                )
                UNION ALL
                SELECT 'tenant-contact:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:view'
                )
                UNION ALL
                SELECT 'tenant-domain:manage'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
                UNION ALL
                SELECT 'tenant-contact:manage'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
            ) expanded
        ) distinct_code
    ),
    updated_by = 'system-v47',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(role_template.permission_codes) existing(code)
    WHERE existing.code IN ('tenant:view', 'tenant:update')
);

UPDATE iam_authorization_snapshot snapshot
SET permission_codes = (
        SELECT COALESCE(jsonb_agg(code ORDER BY code), '[]'::jsonb)
        FROM (
            SELECT DISTINCT code
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) AS code
                UNION ALL
                SELECT 'tenant-domain:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code = 'tenant:view'
                )
                UNION ALL
                SELECT 'tenant-contact:view'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code = 'tenant:view'
                )
                UNION ALL
                SELECT 'tenant-domain:manage'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
                UNION ALL
                SELECT 'tenant-contact:manage'
                WHERE EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
                    WHERE existing.code = 'tenant:update'
                )
            ) expanded
        ) distinct_code
    ),
    auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'permissions-v47:' || snapshot.tenant_id || ':' || snapshot.user_id,
    updated_by = 'system-v47',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements_text(COALESCE(snapshot.permission_codes, '[]'::jsonb)) existing(code)
    WHERE existing.code IN ('tenant:view', 'tenant:update')
);
