-- Add tenant initialization role templates.
-- V1-V20 history remains unchanged; this migration adds the role-template fact source and replaces the main bootstrap function.

CREATE TABLE IF NOT EXISTS iam_tenant_init_role_template (
    id bigserial NOT NULL,
    init_template_code varchar(64) NOT NULL,
    role_code varchar(100) NOT NULL,
    role_name varchar(200) NOT NULL,
    role_description text,
    permission_codes jsonb DEFAULT '[]'::jsonb NOT NULL,
    assign_to_admin boolean DEFAULT false NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    is_enabled boolean DEFAULT true NOT NULL,
    created_by varchar(100) DEFAULT 'system' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_by varchar(100) DEFAULT 'system' NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    deleted_by varchar(100),
    delete_reason varchar(500),
    deleted_at timestamptz,
    CONSTRAINT pk_iam_tenant_init_role_template PRIMARY KEY (id)
);

COMMENT ON TABLE iam_tenant_init_role_template IS '租户初始化角色模板表，定义初始化模板下需要创建的默认角色、权限集合和初始管理员分配策略。';
COMMENT ON COLUMN iam_tenant_init_role_template.init_template_code IS '初始化模板编码，对应 iam_tenant_init_permission_template.code。';
COMMENT ON COLUMN iam_tenant_init_role_template.role_code IS '租户内角色编码。';
COMMENT ON COLUMN iam_tenant_init_role_template.role_name IS '租户内角色名称。';
COMMENT ON COLUMN iam_tenant_init_role_template.role_description IS '租户内角色描述。';
COMMENT ON COLUMN iam_tenant_init_role_template.permission_codes IS '角色模板权限编码集合 JSONB；实际授权会与初始化模板权限上限取交集。';
COMMENT ON COLUMN iam_tenant_init_role_template.assign_to_admin IS '是否默认分配给初始化管理员。';
COMMENT ON COLUMN iam_tenant_init_role_template.sort_order IS '模板内排序。';
COMMENT ON COLUMN iam_tenant_init_role_template.is_enabled IS '是否启用。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_iam_tenant_init_role_template_active
    ON iam_tenant_init_role_template (init_template_code, role_code)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_iam_tenant_init_role_template_code
    ON iam_tenant_init_role_template (init_template_code, is_enabled, sort_order)
    WHERE deleted_at IS NULL;

WITH permission_matrix(code, permission_codes) AS (
    VALUES
        ('readonly', jsonb_build_array(
            'product:view',
            'party:view',
            'warehouse:view',
            'inventory:view'
        )),
        ('standard', jsonb_build_array(
            'product:view', 'product:create', 'product:update', 'product:import', 'product:export',
            'party:view', 'party:create', 'party:update', 'party:import', 'party:export',
            'warehouse:view', 'warehouse:create', 'warehouse:update', 'warehouse:import', 'warehouse:export',
            'inventory:view', 'inventory:create', 'inventory:update', 'inventory:audit', 'inventory:import', 'inventory:export',
            'sales:view', 'sales:create', 'sales:update', 'sales:audit', 'sales:import', 'sales:export',
            'procurement:view', 'procurement:create', 'procurement:update', 'procurement:audit', 'procurement:import', 'procurement:export',
            'document:view', 'document:export'
        )),
        ('full', jsonb_build_array(
            'iam:view', 'iam:create', 'iam:update', 'iam:delete', 'iam:enable', 'iam:disable',
            'audit:view', 'audit:export', 'audit:log:view', 'audit:interface-cost:view', 'audit:sql-ranking:view',
            'product:view', 'product:create', 'product:update', 'product:delete', 'product:enable', 'product:disable', 'product:audit', 'product:import', 'product:export',
            'party:view', 'party:create', 'party:update', 'party:delete', 'party:enable', 'party:disable', 'party:import', 'party:export',
            'warehouse:view', 'warehouse:create', 'warehouse:update', 'warehouse:delete', 'warehouse:enable', 'warehouse:disable', 'warehouse:import', 'warehouse:export',
            'inventory:view', 'inventory:create', 'inventory:update', 'inventory:delete', 'inventory:enable', 'inventory:disable', 'inventory:audit', 'inventory:import', 'inventory:export',
            'sales:view', 'sales:create', 'sales:update', 'sales:delete', 'sales:enable', 'sales:disable', 'sales:audit', 'sales:import', 'sales:export',
            'procurement:view', 'procurement:create', 'procurement:update', 'procurement:delete', 'procurement:enable', 'procurement:disable', 'procurement:audit', 'procurement:import', 'procurement:export',
            'finance:view', 'finance:create', 'finance:update', 'finance:delete', 'finance:enable', 'finance:disable', 'finance:audit', 'finance:import', 'finance:export',
            'document:view', 'document:create', 'document:update', 'document:delete', 'document:enable', 'document:disable', 'document:export',
            'manufacturing:view', 'manufacturing:create', 'manufacturing:update', 'manufacturing:delete', 'manufacturing:enable', 'manufacturing:disable', 'manufacturing:audit', 'manufacturing:import', 'manufacturing:export',
            'query:view', 'query:create', 'query:update', 'query:delete', 'query:enable', 'query:disable', 'query:export'
        ))
),
template_permission(code, permission_codes) AS (
    SELECT 'basic', permission_codes
    FROM permission_matrix
    WHERE code = 'readonly'
    UNION ALL
    SELECT 'standard', permission_codes
    FROM permission_matrix
    WHERE code = 'standard'
    UNION ALL
    SELECT 'full', permission_codes
    FROM permission_matrix
    WHERE code = 'full'
)
UPDATE iam_tenant_init_permission_template template
SET permission_codes = template_permission.permission_codes,
    updated_by = 'system',
    updated_at = now()
FROM template_permission
WHERE template.code = template_permission.code
  AND template.deleted_at IS NULL;

WITH permission_matrix(code, permission_codes) AS (
    VALUES
        ('readonly', jsonb_build_array(
            'product:view',
            'party:view',
            'warehouse:view',
            'inventory:view'
        )),
        ('standard', jsonb_build_array(
            'product:view', 'product:create', 'product:update', 'product:import', 'product:export',
            'party:view', 'party:create', 'party:update', 'party:import', 'party:export',
            'warehouse:view', 'warehouse:create', 'warehouse:update', 'warehouse:import', 'warehouse:export',
            'inventory:view', 'inventory:create', 'inventory:update', 'inventory:audit', 'inventory:import', 'inventory:export',
            'sales:view', 'sales:create', 'sales:update', 'sales:audit', 'sales:import', 'sales:export',
            'procurement:view', 'procurement:create', 'procurement:update', 'procurement:audit', 'procurement:import', 'procurement:export',
            'document:view', 'document:export'
        )),
        ('full', jsonb_build_array(
            'iam:view', 'iam:create', 'iam:update', 'iam:delete', 'iam:enable', 'iam:disable',
            'audit:view', 'audit:export', 'audit:log:view', 'audit:interface-cost:view', 'audit:sql-ranking:view',
            'product:view', 'product:create', 'product:update', 'product:delete', 'product:enable', 'product:disable', 'product:audit', 'product:import', 'product:export',
            'party:view', 'party:create', 'party:update', 'party:delete', 'party:enable', 'party:disable', 'party:import', 'party:export',
            'warehouse:view', 'warehouse:create', 'warehouse:update', 'warehouse:delete', 'warehouse:enable', 'warehouse:disable', 'warehouse:import', 'warehouse:export',
            'inventory:view', 'inventory:create', 'inventory:update', 'inventory:delete', 'inventory:enable', 'inventory:disable', 'inventory:audit', 'inventory:import', 'inventory:export',
            'sales:view', 'sales:create', 'sales:update', 'sales:delete', 'sales:enable', 'sales:disable', 'sales:audit', 'sales:import', 'sales:export',
            'procurement:view', 'procurement:create', 'procurement:update', 'procurement:delete', 'procurement:enable', 'procurement:disable', 'procurement:audit', 'procurement:import', 'procurement:export',
            'finance:view', 'finance:create', 'finance:update', 'finance:delete', 'finance:enable', 'finance:disable', 'finance:audit', 'finance:import', 'finance:export',
            'document:view', 'document:create', 'document:update', 'document:delete', 'document:enable', 'document:disable', 'document:export',
            'manufacturing:view', 'manufacturing:create', 'manufacturing:update', 'manufacturing:delete', 'manufacturing:enable', 'manufacturing:disable', 'manufacturing:audit', 'manufacturing:import', 'manufacturing:export',
            'query:view', 'query:create', 'query:update', 'query:delete', 'query:enable', 'query:disable', 'query:export'
        ))
),
seed_role(init_template_code, role_code, role_name, role_description, permission_matrix_code, assign_to_admin, sort_order) AS (
    VALUES
        ('basic', 'tenant_readonly', '租户只读角色', '基础版默认角色，仅允许查看套餐范围内基础资料。', 'readonly', true, 10),
        ('standard', 'tenant_readonly', '租户只读角色', '标准版预置只读角色，可分配给普通查看人员。', 'readonly', false, 10),
        ('standard', 'tenant_admin', '租户管理员', '标准版默认管理员角色，允许维护常规业务资料。', 'standard', true, 20),
        ('full', 'tenant_readonly', '租户只读角色', '完整版预置只读角色，可分配给普通查看人员。', 'readonly', false, 10),
        ('full', 'tenant_admin', '租户管理员', '完整版预置管理员角色，可分配给业务管理员。', 'standard', false, 20),
        ('full', 'tenant_owner', '租户拥有者', '完整版默认拥有者角色，在套餐范围内拥有最高权限。', 'full', true, 30)
),
resolved_seed AS (
    SELECT seed_role.init_template_code,
           seed_role.role_code,
           seed_role.role_name,
           seed_role.role_description,
           permission_matrix.permission_codes,
           seed_role.assign_to_admin,
           seed_role.sort_order
    FROM seed_role
    JOIN permission_matrix ON permission_matrix.code = seed_role.permission_matrix_code
)
INSERT INTO iam_tenant_init_role_template (
    init_template_code, role_code, role_name, role_description, permission_codes,
    assign_to_admin, sort_order, is_enabled, created_by, updated_by
)
SELECT init_template_code,
       role_code,
       role_name,
       role_description,
       permission_codes,
       assign_to_admin,
       sort_order,
       true,
       'system',
       'system'
FROM resolved_seed
ON CONFLICT (init_template_code, role_code) WHERE deleted_at IS NULL DO UPDATE
SET role_name = EXCLUDED.role_name,
    role_description = EXCLUDED.role_description,
    permission_codes = EXCLUDED.permission_codes,
    assign_to_admin = EXCLUDED.assign_to_admin,
    sort_order = EXCLUDED.sort_order,
    is_enabled = EXCLUDED.is_enabled,
    updated_by = EXCLUDED.updated_by,
    updated_at = now();

CREATE OR REPLACE FUNCTION bootstrap_iam_tenant(
    p_tenant_id bigint,
    p_admin_username varchar,
    p_admin_password_hash varchar,
    p_admin_display_name varchar,
    p_admin_email varchar,
    p_admin_phone varchar,
    p_iam_init_template_code varchar,
    p_requested_by varchar
)
RETURNS integer
LANGUAGE plpgsql
AS $$
DECLARE
    v_inserted_count integer := 0;
    v_task_key varchar(160);
    v_idempotency_key varchar(200);
    v_event_id varchar(160);
    v_admin_username varchar(100);
    v_admin_display_name varchar(200);
    v_admin_user_id bigint;
    v_template_lookup_code varchar(64);
    v_template_code varchar(64);
    v_permission_codes jsonb := '[]'::jsonb;
BEGIN
    IF p_tenant_id IS NULL OR p_tenant_id <= 0 THEN
        RAISE EXCEPTION 'p_tenant_id must be a positive tenant id';
    END IF;
    IF p_admin_password_hash IS NULL OR btrim(p_admin_password_hash) = '' THEN
        RAISE EXCEPTION 'p_admin_password_hash must not be blank';
    END IF;

    v_task_key := 'IAM_TENANT_BOOTSTRAP';
    v_idempotency_key := 'iam:tenant:' || p_tenant_id || ':bootstrap:v1';
    v_event_id := v_idempotency_key || ':event';
    v_admin_username := COALESCE(NULLIF(btrim(p_admin_username), ''), 'admin');
    v_admin_display_name := COALESCE(NULLIF(btrim(p_admin_display_name), ''), '租户管理员');
    v_template_lookup_code := NULLIF(btrim(p_iam_init_template_code), '');

    SELECT template.code,
           template.permission_codes
    INTO v_template_code,
         v_permission_codes
    FROM iam_tenant_init_permission_template template
    WHERE template.deleted_at IS NULL
      AND template.is_enabled = true
      AND (
          (v_template_lookup_code IS NOT NULL AND template.code = v_template_lookup_code)
          OR (v_template_lookup_code IS NULL AND template.is_default = true)
      )
    ORDER BY template.id
    LIMIT 1;

    IF v_template_code IS NULL THEN
        RAISE EXCEPTION 'iam tenant init permission template not found or disabled: %',
            COALESCE(v_template_lookup_code, 'default');
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM iam_tenant_init_role_template role_template
        WHERE role_template.init_template_code = v_template_code
          AND role_template.is_enabled = true
          AND role_template.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'iam tenant init role template not found or disabled: %', v_template_code;
    END IF;

    WITH RECURSIVE selected_permission(code) AS (
        SELECT DISTINCT value
        FROM jsonb_array_elements_text(COALESCE(v_permission_codes, '[]'::jsonb)) AS item(value)
    ),
    direct_menu AS (
        SELECT menu.id,
               menu.parent_id
        FROM iam_menu menu
        WHERE menu.deleted_at IS NULL
          AND menu.is_enabled = true
          AND (
              menu.permission_code IS NULL
              OR EXISTS (
                  SELECT 1
                  FROM selected_permission
                  WHERE selected_permission.code = menu.permission_code
              )
          )
    ),
    menu_tree(id, parent_id) AS (
        SELECT direct_menu.id,
               direct_menu.parent_id
        FROM direct_menu
        UNION
        SELECT parent.id,
               parent.parent_id
        FROM iam_menu parent
        JOIN menu_tree child ON child.parent_id = parent.id
        WHERE parent.deleted_at IS NULL
          AND parent.is_enabled = true
    )
    UPDATE iam_tenant_menu tenant_menu
    SET deleted_by = p_requested_by,
        delete_reason = 'sync by iam init role template ' || v_template_code,
        deleted_at = now(),
        updated_by = p_requested_by,
        updated_at = now()
    WHERE tenant_menu.tenant_id = p_tenant_id
      AND tenant_menu.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM menu_tree
          WHERE menu_tree.id = tenant_menu.menu_id
      );

    WITH RECURSIVE selected_permission(code) AS (
        SELECT DISTINCT value
        FROM jsonb_array_elements_text(COALESCE(v_permission_codes, '[]'::jsonb)) AS item(value)
    ),
    direct_menu AS (
        SELECT menu.id,
               menu.parent_id
        FROM iam_menu menu
        WHERE menu.deleted_at IS NULL
          AND menu.is_enabled = true
          AND (
              menu.permission_code IS NULL
              OR EXISTS (
                  SELECT 1
                  FROM selected_permission
                  WHERE selected_permission.code = menu.permission_code
              )
          )
    ),
    menu_tree(id, parent_id) AS (
        SELECT direct_menu.id,
               direct_menu.parent_id
        FROM direct_menu
        UNION
        SELECT parent.id,
               parent.parent_id
        FROM iam_menu parent
        JOIN menu_tree child ON child.parent_id = parent.id
        WHERE parent.deleted_at IS NULL
          AND parent.is_enabled = true
    )
    INSERT INTO iam_tenant_menu (tenant_id, menu_id, is_enabled, created_by, updated_by)
    SELECT DISTINCT p_tenant_id, menu_tree.id, true, p_requested_by, p_requested_by
    FROM menu_tree
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_tenant_menu existing
        WHERE existing.tenant_id = p_tenant_id
          AND existing.menu_id = menu_tree.id
          AND existing.deleted_at IS NULL
    );

    GET DIAGNOSTICS v_inserted_count = ROW_COUNT;

    INSERT INTO iam_user (
        tenant_id, username, password_hash, display_name, email, phone,
        is_enabled, account_non_expired, account_non_locked, credentials_non_expired,
        password_changed_at, auth_version, created_by, updated_by
    )
    SELECT
        p_tenant_id,
        v_admin_username,
        p_admin_password_hash,
        v_admin_display_name,
        NULLIF(btrim(p_admin_email), ''),
        NULLIF(btrim(p_admin_phone), ''),
        true,
        true,
        true,
        true,
        now(),
        1,
        p_requested_by,
        p_requested_by
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_user existing
        WHERE existing.tenant_id = p_tenant_id
          AND existing.username = v_admin_username
          AND existing.deleted_at IS NULL
    )
    RETURNING id INTO v_admin_user_id;

    IF v_admin_user_id IS NULL THEN
        SELECT id INTO v_admin_user_id
        FROM iam_user
        WHERE tenant_id = p_tenant_id
          AND username = v_admin_username
          AND deleted_at IS NULL
        ORDER BY id
        LIMIT 1;
    END IF;

    INSERT INTO iam_user_tenant (
        tenant_id, user_id, user_type, membership_status, is_tenant_admin, created_by, updated_by
    )
    SELECT p_tenant_id, v_admin_user_id, 'TENANT_USER', 'ACTIVE', true, p_requested_by, p_requested_by
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_user_tenant existing
        WHERE existing.tenant_id = p_tenant_id
          AND existing.user_id = v_admin_user_id
          AND existing.deleted_at IS NULL
    );

    UPDATE iam_role role
    SET name = role_template.role_name,
        description = role_template.role_description,
        is_enabled = true,
        updated_by = p_requested_by,
        updated_at = now()
    FROM iam_tenant_init_role_template role_template
    WHERE role.tenant_id = p_tenant_id
      AND role.code = role_template.role_code
      AND role.deleted_at IS NULL
      AND role_template.init_template_code = v_template_code
      AND role_template.is_enabled = true
      AND role_template.deleted_at IS NULL;

    INSERT INTO iam_role (tenant_id, code, name, description, is_enabled, created_by, updated_by)
    SELECT p_tenant_id,
           role_template.role_code,
           role_template.role_name,
           role_template.role_description,
           true,
           p_requested_by,
           p_requested_by
    FROM iam_tenant_init_role_template role_template
    WHERE role_template.init_template_code = v_template_code
      AND role_template.is_enabled = true
      AND role_template.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM iam_role existing
          WHERE existing.tenant_id = p_tenant_id
            AND existing.code = role_template.role_code
            AND existing.deleted_at IS NULL
      );

    WITH managed_role_code(role_code) AS (
        SELECT DISTINCT role_template.role_code
        FROM iam_tenant_init_role_template role_template
        WHERE role_template.deleted_at IS NULL
    ),
    assignable_role(role_id) AS (
        SELECT role.id
        FROM iam_tenant_init_role_template role_template
        JOIN iam_role role ON role.tenant_id = p_tenant_id
                          AND role.code = role_template.role_code
                          AND role.deleted_at IS NULL
        WHERE role_template.init_template_code = v_template_code
          AND role_template.is_enabled = true
          AND role_template.deleted_at IS NULL
          AND role_template.assign_to_admin = true
    )
    UPDATE iam_user_role user_role
    SET deleted_by = p_requested_by,
        delete_reason = 'sync by iam init role template ' || v_template_code,
        deleted_at = now(),
        updated_by = p_requested_by,
        updated_at = now()
    FROM iam_role role
    WHERE user_role.tenant_id = p_tenant_id
      AND user_role.user_id = v_admin_user_id
      AND user_role.role_id = role.id
      AND user_role.deleted_at IS NULL
      AND role.tenant_id = p_tenant_id
      AND role.deleted_at IS NULL
      AND role.code IN (SELECT role_code FROM managed_role_code)
      AND NOT EXISTS (
          SELECT 1
          FROM assignable_role
          WHERE assignable_role.role_id = user_role.role_id
      );

    INSERT INTO iam_user_role (tenant_id, user_id, role_id, created_by, updated_by)
    SELECT p_tenant_id, v_admin_user_id, role.id, p_requested_by, p_requested_by
    FROM iam_tenant_init_role_template role_template
    JOIN iam_role role ON role.tenant_id = p_tenant_id
                      AND role.code = role_template.role_code
                      AND role.deleted_at IS NULL
    WHERE role_template.init_template_code = v_template_code
      AND role_template.is_enabled = true
      AND role_template.deleted_at IS NULL
      AND role_template.assign_to_admin = true
      AND NOT EXISTS (
          SELECT 1
          FROM iam_user_role existing
          WHERE existing.tenant_id = p_tenant_id
            AND existing.user_id = v_admin_user_id
            AND existing.role_id = role.id
            AND existing.deleted_at IS NULL
      );

    WITH template_permission(code) AS (
        SELECT DISTINCT value
        FROM jsonb_array_elements_text(COALESCE(v_permission_codes, '[]'::jsonb)) AS item(value)
    ),
    current_role_permission(role_code, permission_code) AS (
        SELECT DISTINCT role_template.role_code,
               role_permission_code.value
        FROM iam_tenant_init_role_template role_template
        JOIN LATERAL jsonb_array_elements_text(COALESCE(role_template.permission_codes, '[]'::jsonb)) AS role_permission_code(value) ON true
        JOIN template_permission ON template_permission.code = role_permission_code.value
        WHERE role_template.init_template_code = v_template_code
          AND role_template.is_enabled = true
          AND role_template.deleted_at IS NULL
    ),
    managed_role_code(role_code) AS (
        SELECT DISTINCT role_template.role_code
        FROM iam_tenant_init_role_template role_template
        WHERE role_template.deleted_at IS NULL
    )
    UPDATE iam_role_permission role_permission
    SET deleted_by = p_requested_by,
        delete_reason = 'sync by iam init role template ' || v_template_code,
        deleted_at = now(),
        updated_by = p_requested_by,
        updated_at = now()
    FROM iam_role role,
         iam_permission permission
    WHERE role_permission.tenant_id = p_tenant_id
      AND role_permission.role_id = role.id
      AND role_permission.permission_id = permission.id
      AND role_permission.deleted_at IS NULL
      AND role.tenant_id = p_tenant_id
      AND role.deleted_at IS NULL
      AND role.code IN (SELECT role_code FROM managed_role_code)
      AND permission.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM current_role_permission
          WHERE current_role_permission.role_code = role.code
            AND current_role_permission.permission_code = permission.code
      );

    WITH template_permission(code) AS (
        SELECT DISTINCT value
        FROM jsonb_array_elements_text(COALESCE(v_permission_codes, '[]'::jsonb)) AS item(value)
    ),
    current_role_permission(role_code, permission_code) AS (
        SELECT DISTINCT role_template.role_code,
               role_permission_code.value
        FROM iam_tenant_init_role_template role_template
        JOIN LATERAL jsonb_array_elements_text(COALESCE(role_template.permission_codes, '[]'::jsonb)) AS role_permission_code(value) ON true
        JOIN template_permission ON template_permission.code = role_permission_code.value
        WHERE role_template.init_template_code = v_template_code
          AND role_template.is_enabled = true
          AND role_template.deleted_at IS NULL
    )
    INSERT INTO iam_role_permission (tenant_id, role_id, permission_id, created_by, updated_by)
    SELECT p_tenant_id, role.id, permission.id, p_requested_by, p_requested_by
    FROM current_role_permission
    JOIN iam_role role ON role.tenant_id = p_tenant_id
                      AND role.code = current_role_permission.role_code
                      AND role.deleted_at IS NULL
    JOIN iam_permission permission ON permission.code = current_role_permission.permission_code
                                  AND permission.is_enabled = true
                                  AND permission.deleted_at IS NULL
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_role_permission existing
        WHERE existing.tenant_id = p_tenant_id
          AND existing.role_id = role.id
          AND existing.permission_id = permission.id
          AND existing.deleted_at IS NULL
    );

    INSERT INTO iam_authorization_snapshot (
        tenant_id, user_id, auth_version, role_ids, permission_codes, menu_codes, column_settings,
        snapshot_hash, built_at, created_by, updated_by
    )
    SELECT
        p_tenant_id,
        v_admin_user_id,
        1,
        COALESCE((
            SELECT jsonb_agg(role.id ORDER BY role.code)
            FROM iam_user_role user_role
            JOIN iam_role role ON role.id = user_role.role_id
            WHERE user_role.tenant_id = p_tenant_id
              AND user_role.user_id = v_admin_user_id
              AND user_role.deleted_at IS NULL
              AND role.deleted_at IS NULL
        ), '[]'::jsonb),
        COALESCE((
            SELECT jsonb_agg(code ORDER BY code)
            FROM (
                SELECT DISTINCT permission.code
                FROM iam_user_role user_role
                JOIN iam_role role ON role.id = user_role.role_id
                JOIN iam_role_permission role_permission ON role_permission.tenant_id = user_role.tenant_id
                                                        AND role_permission.role_id = role.id
                                                        AND role_permission.deleted_at IS NULL
                JOIN iam_permission permission ON permission.id = role_permission.permission_id
                                              AND permission.deleted_at IS NULL
                                              AND permission.is_enabled = true
                WHERE user_role.tenant_id = p_tenant_id
                  AND user_role.user_id = v_admin_user_id
                  AND user_role.deleted_at IS NULL
                  AND role.deleted_at IS NULL
            ) active_permission
        ), '[]'::jsonb),
        COALESCE((
            SELECT jsonb_agg(menu.code ORDER BY menu.sort_no, menu.code)
            FROM iam_tenant_menu tenant_menu
            JOIN iam_menu menu ON menu.id = tenant_menu.menu_id
            WHERE tenant_menu.tenant_id = p_tenant_id
              AND tenant_menu.deleted_at IS NULL
              AND menu.deleted_at IS NULL
        ), '[]'::jsonb),
        '{}'::jsonb,
        'tenant_init_role_template:' || v_template_code || ':' || p_tenant_id || ':' || v_admin_user_id,
        now(),
        p_requested_by,
        p_requested_by
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

    INSERT INTO iam_tenant_bootstrap_task (
        tenant_id,
        task_key,
        status,
        idempotency_key,
        requested_by,
        menu_grant_count,
        started_at,
        finished_at,
        created_by,
        updated_by
    )
    SELECT
        p_tenant_id,
        v_task_key,
        'SUCCEEDED',
        v_idempotency_key,
        p_requested_by,
        v_inserted_count,
        now(),
        now(),
        p_requested_by,
        p_requested_by
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_tenant_bootstrap_task existing
        WHERE existing.idempotency_key = v_idempotency_key
    );

    INSERT INTO iam_outbox_event (
        tenant_id,
        event_id,
        aggregate_type,
        aggregate_id,
        event_type,
        topic,
        payload,
        headers,
        status,
        next_retry_at,
        created_by,
        updated_by
    )
    VALUES (
        p_tenant_id,
        v_event_id,
        'IAM_TENANT',
        p_tenant_id::text,
        'IamTenantBootstrapped',
        'iam.tenant.bootstrapped',
        jsonb_build_object(
            'tenantId', p_tenant_id,
            'menuGrantCount', v_inserted_count,
            'adminUsername', v_admin_username,
            'iamInitTemplateCode', v_template_code,
            'permissionCodes', v_permission_codes,
            'assignedRoleCodes', COALESCE((
                SELECT jsonb_agg(role_template.role_code ORDER BY role_template.sort_order, role_template.role_code)
                FROM iam_tenant_init_role_template role_template
                WHERE role_template.init_template_code = v_template_code
                  AND role_template.is_enabled = true
                  AND role_template.deleted_at IS NULL
                  AND role_template.assign_to_admin = true
            ), '[]'::jsonb),
            'idempotencyKey', v_idempotency_key,
            'requestedBy', p_requested_by
        ),
        jsonb_build_object(
            'eventName', 'IamTenantBootstrapped',
            'source', 'xuan-iam'
        ),
        'PENDING',
        now(),
        p_requested_by,
        p_requested_by
    )
    ON CONFLICT (event_id) DO NOTHING;

    RETURN v_inserted_count;
EXCEPTION WHEN OTHERS THEN
    INSERT INTO iam_tenant_bootstrap_task (
        tenant_id,
        task_key,
        status,
        idempotency_key,
        requested_by,
        menu_grant_count,
        started_at,
        finished_at,
        last_error_message,
        created_by,
        updated_by
    )
    VALUES (
        p_tenant_id,
        COALESCE(v_task_key, 'IAM_TENANT_BOOTSTRAP'),
        'FAILED',
        COALESCE(v_idempotency_key, 'iam:tenant:' || COALESCE(p_tenant_id::text, 'null') || ':bootstrap:v1'),
        p_requested_by,
        0,
        now(),
        now(),
        SQLERRM,
        p_requested_by,
        p_requested_by
    );
    RAISE;
END;
$$;

COMMENT ON FUNCTION bootstrap_iam_tenant(bigint, varchar, varchar, varchar, varchar, varchar, varchar, varchar)
    IS '按初始化角色模板同步 IAM 租户菜单、默认角色、角色权限、管理员角色绑定、授权快照和 outbox 事件；模板切换时会收敛旧角色绑定和多余授权。';
