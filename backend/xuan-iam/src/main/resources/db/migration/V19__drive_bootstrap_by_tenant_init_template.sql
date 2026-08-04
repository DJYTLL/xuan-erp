-- Drive IAM tenant bootstrap permissions by tenant init template.
-- V1-V18 history remains unchanged; this migration only replaces bootstrap functions.

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
    v_admin_role_id bigint;
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

    INSERT INTO iam_role (tenant_id, code, name, description, is_enabled, created_by, updated_by)
    SELECT p_tenant_id, 'tenant_admin', '租户管理员',
           '租户内最高管理员角色，由租户开通流程按初始化模板 ' || v_template_code || ' 初始化',
           true, p_requested_by, p_requested_by
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_role existing
        WHERE existing.tenant_id = p_tenant_id
          AND existing.code = 'tenant_admin'
          AND existing.deleted_at IS NULL
    )
    RETURNING id INTO v_admin_role_id;

    IF v_admin_role_id IS NULL THEN
        SELECT id INTO v_admin_role_id
        FROM iam_role
        WHERE tenant_id = p_tenant_id
          AND code = 'tenant_admin'
          AND deleted_at IS NULL
        ORDER BY id
        LIMIT 1;
    END IF;

    INSERT INTO iam_user_role (tenant_id, user_id, role_id, created_by, updated_by)
    SELECT p_tenant_id, v_admin_user_id, v_admin_role_id, p_requested_by, p_requested_by
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_user_role existing
        WHERE existing.tenant_id = p_tenant_id
          AND existing.user_id = v_admin_user_id
          AND existing.role_id = v_admin_role_id
          AND existing.deleted_at IS NULL
    );

    WITH selected_permission(code) AS (
        SELECT DISTINCT value
        FROM jsonb_array_elements_text(COALESCE(v_permission_codes, '[]'::jsonb)) AS item(value)
    )
    INSERT INTO iam_role_permission (tenant_id, role_id, permission_id, created_by, updated_by)
    SELECT p_tenant_id, v_admin_role_id, permission.id, p_requested_by, p_requested_by
    FROM selected_permission
    JOIN iam_permission permission ON permission.code = selected_permission.code
    WHERE permission.is_enabled = true
      AND permission.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM iam_role_permission existing
          WHERE existing.tenant_id = p_tenant_id
            AND existing.role_id = v_admin_role_id
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
        jsonb_build_array(v_admin_role_id),
        COALESCE((
            SELECT jsonb_agg(permission.code ORDER BY permission.code)
            FROM iam_role_permission role_permission
            JOIN iam_permission permission ON permission.id = role_permission.permission_id
            WHERE role_permission.tenant_id = p_tenant_id
              AND role_permission.role_id = v_admin_role_id
              AND role_permission.deleted_at IS NULL
              AND permission.deleted_at IS NULL
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
        'tenant_admin:' || p_tenant_id || ':' || v_admin_user_id,
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
    IS '按 xuan-tenant 传入的真实 tenantId、管理员账号信息和初始化模板编码，幂等初始化 IAM 租户菜单、管理员、角色权限、授权快照和 outbox 事件。';

CREATE OR REPLACE FUNCTION bootstrap_iam_tenant(
    p_tenant_id bigint,
    p_admin_username varchar DEFAULT 'admin',
    p_admin_password_hash varchar DEFAULT '{bcrypt}$2a$10$WzNYvhB.uKmkGUNZMm.rm.f5eh7rH.3RdyWVqtxBquzkacNX0fK0a',
    p_admin_display_name varchar DEFAULT '租户管理员',
    p_admin_email varchar DEFAULT NULL,
    p_admin_phone varchar DEFAULT NULL,
    p_requested_by varchar DEFAULT 'tenant-provision'
)
RETURNS integer
LANGUAGE sql
AS $$
    SELECT bootstrap_iam_tenant(
        p_tenant_id,
        p_admin_username,
        p_admin_password_hash,
        p_admin_display_name,
        p_admin_email,
        p_admin_phone,
        NULL,
        p_requested_by
    );
$$;

COMMENT ON FUNCTION bootstrap_iam_tenant(bigint, varchar, varchar, varchar, varchar, varchar, varchar)
    IS '兼容旧调用的 IAM 租户初始化入口；未传模板编码时使用默认初始化模板。';

CREATE OR REPLACE FUNCTION bootstrap_iam_tenant(
    p_tenant_id bigint,
    p_requested_by varchar DEFAULT 'tenant-provision'
)
RETURNS integer
LANGUAGE sql
AS $$
    SELECT bootstrap_iam_tenant(
        p_tenant_id,
        'admin',
        '{bcrypt}$2a$10$WzNYvhB.uKmkGUNZMm.rm.f5eh7rH.3RdyWVqtxBquzkacNX0fK0a',
        '租户管理员',
        NULL,
        NULL,
        NULL,
        p_requested_by
    );
$$;

COMMENT ON FUNCTION bootstrap_iam_tenant(bigint, varchar)
    IS '兼容旧调用的 IAM 租户初始化入口，默认创建 admin/123456 租户管理员并使用默认初始化模板。';
