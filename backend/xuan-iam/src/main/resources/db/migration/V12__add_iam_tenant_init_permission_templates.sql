-- Add IAM tenant initialization permission templates.
-- This migration is append-only: V1-V11 history remains unchanged.

CREATE TABLE IF NOT EXISTS iam_tenant_init_permission_template (
    id BIGSERIAL,
    code varchar(64) NOT NULL,
    name varchar(120) NOT NULL,
    description varchar(500),
    permission_codes jsonb DEFAULT '[]'::jsonb NOT NULL,
    is_default boolean DEFAULT false NOT NULL,
    is_enabled boolean DEFAULT true NOT NULL,
    created_by varchar(64) DEFAULT 'system' NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by varchar(64) DEFAULT 'system' NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_by varchar(64),
    delete_reason varchar(500),
    deleted_at timestamptz,
    CONSTRAINT pk_iam_tenant_init_permission_template PRIMARY KEY (id)
);

COMMENT ON TABLE iam_tenant_init_permission_template IS '租户初始化权限模板表，保存新租户初始化管理员或角色的默认权限集合。';
COMMENT ON COLUMN iam_tenant_init_permission_template.code IS '模板编码';
COMMENT ON COLUMN iam_tenant_init_permission_template.name IS '模板名称';
COMMENT ON COLUMN iam_tenant_init_permission_template.permission_codes IS '权限编码集合 JSONB';
COMMENT ON COLUMN iam_tenant_init_permission_template.is_default IS '是否默认模板';
COMMENT ON COLUMN iam_tenant_init_permission_template.is_enabled IS '是否启用';

CREATE UNIQUE INDEX IF NOT EXISTS uk_iam_tenant_init_permission_template_code_active
    ON iam_tenant_init_permission_template (code)
    WHERE deleted_at IS NULL;

WITH seed_template(code, name, description, permission_codes, is_default, sort_order) AS (
    VALUES
        ('basic', '基础模板', '适用于只读基础资料和租户自助查看的初始化权限', jsonb_build_array('iam:view', 'tenant:view', 'product:view'), true, 1),
        ('standard', '标准模板', '适用于常规业务租户管理员的初始化权限', jsonb_build_array('iam:view', 'tenant:view', 'product:view', 'procurement:view'), false, 2),
        ('full', '完整模板', '适用于需要开通全部现有业务查看权限的初始化权限', jsonb_build_array('iam:view', 'tenant:view', 'product:view', 'procurement:view', 'audit:log:view'), false, 3)
)
INSERT INTO iam_tenant_init_permission_template (
    code, name, description, permission_codes, is_default, is_enabled, created_by, updated_by
)
SELECT seed_template.code,
       seed_template.name,
       seed_template.description,
       seed_template.permission_codes,
       seed_template.is_default,
       true,
       'system',
       'system'
FROM seed_template
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_tenant_init_permission_template existing
    WHERE existing.code = seed_template.code
      AND existing.deleted_at IS NULL
);

WITH system_menu AS (
    SELECT id
    FROM iam_menu
    WHERE code = 'system'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
),
template_menu(code, title, i18n_key, path, icon, permission_code, sort_no) AS (
    VALUES
        ('iam-init-template-management', '初始化模板', 'route.iamInitTemplates', '/system/iam/init-templates', 'ShieldCheck', 'iam:view', 125)
)
INSERT INTO iam_menu (
    code, parent_id, title, i18n_key, path, icon, permission_code, sort_no, is_enabled, created_by, updated_by
)
SELECT template_menu.code,
       system_menu.id,
       template_menu.title,
       template_menu.i18n_key,
       template_menu.path,
       template_menu.icon,
       template_menu.permission_code,
       template_menu.sort_no,
       true,
       'system',
       'system'
FROM template_menu
CROSS JOIN system_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_menu existing
    WHERE existing.code = template_menu.code
      AND existing.deleted_at IS NULL
);

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
    12,
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
    'super_admin:iam-init-template-management:' || resolved_user.id,
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
