-- Keep the tenant administrator role aligned with the tenant permission entitlement pool.
-- Tenant permission entitlements are the tenant's upper bound; tenant_admin receives that full set automatically.
-- Other tenant roles remain manually assigned by the tenant administrator and are still trimmed by entitlement scope.

CREATE OR REPLACE FUNCTION sync_tenant_admin_role_permissions_with_entitlements(
    p_tenant_id bigint,
    p_operator varchar DEFAULT 'system'
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_admin_role_id bigint;
    v_operator varchar(100) := COALESCE(NULLIF(btrim(p_operator), ''), 'system');
BEGIN
    IF p_tenant_id IS NULL OR p_tenant_id <= 0 THEN
        RETURN;
    END IF;

    INSERT INTO iam_role (tenant_id, code, name, description, is_enabled, created_by, updated_by)
    SELECT p_tenant_id,
           'tenant_admin',
           '租户管理员',
           '租户内管理员角色，自动拥有租户权限池全部权限。',
           true,
           v_operator,
           v_operator
    WHERE NOT EXISTS (
        SELECT 1
        FROM iam_role existing
        WHERE existing.tenant_id = p_tenant_id
          AND existing.code = 'tenant_admin'
          AND existing.deleted_at IS NULL
    );

    UPDATE iam_role
    SET name = '租户管理员',
        description = '租户内管理员角色，自动拥有租户权限池全部权限。',
        is_enabled = true,
        updated_by = v_operator,
        updated_at = now()
    WHERE tenant_id = p_tenant_id
      AND code = 'tenant_admin'
      AND deleted_at IS NULL;

    SELECT role.id
    INTO v_admin_role_id
    FROM iam_role role
    WHERE role.tenant_id = p_tenant_id
      AND role.code = 'tenant_admin'
      AND role.deleted_at IS NULL
    ORDER BY role.id
    LIMIT 1;

    IF v_admin_role_id IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO iam_user_role (tenant_id, user_id, role_id, created_by, updated_by)
    SELECT user_tenant.tenant_id,
           user_tenant.user_id,
           v_admin_role_id,
           v_operator,
           v_operator
    FROM iam_user_tenant user_tenant
    JOIN iam_user iam_user ON iam_user.id = user_tenant.user_id
                          AND iam_user.tenant_id = user_tenant.tenant_id
                          AND iam_user.deleted_at IS NULL
                          AND iam_user.is_enabled = true
    WHERE user_tenant.tenant_id = p_tenant_id
      AND user_tenant.is_tenant_admin = true
      AND user_tenant.membership_status = 'ACTIVE'
      AND user_tenant.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM iam_user_role existing
          WHERE existing.tenant_id = user_tenant.tenant_id
            AND existing.user_id = user_tenant.user_id
            AND existing.role_id = v_admin_role_id
            AND existing.deleted_at IS NULL
      );

    UPDATE iam_role_permission role_permission
    SET deleted_by = v_operator,
        delete_reason = 'tenant admin sync by tenant permission entitlement',
        deleted_at = now(),
        updated_by = v_operator,
        updated_at = now()
    WHERE role_permission.tenant_id = p_tenant_id
      AND role_permission.role_id = v_admin_role_id
      AND role_permission.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM iam_tenant_permission_entitlement entitlement
          WHERE entitlement.tenant_id = role_permission.tenant_id
            AND entitlement.permission_id = role_permission.permission_id
            AND entitlement.deleted_at IS NULL
            AND entitlement.is_enabled = true
      );

    INSERT INTO iam_role_permission (tenant_id, role_id, permission_id, created_by, updated_by)
    SELECT entitlement.tenant_id,
           v_admin_role_id,
           entitlement.permission_id,
           v_operator,
           v_operator
    FROM iam_tenant_permission_entitlement entitlement
    JOIN iam_permission permission ON permission.id = entitlement.permission_id
                                  AND permission.deleted_at IS NULL
                                  AND permission.is_enabled = true
    WHERE entitlement.tenant_id = p_tenant_id
      AND entitlement.deleted_at IS NULL
      AND entitlement.is_enabled = true
      AND NOT EXISTS (
          SELECT 1
          FROM iam_role_permission existing
          WHERE existing.tenant_id = entitlement.tenant_id
            AND existing.role_id = v_admin_role_id
            AND existing.permission_id = entitlement.permission_id
            AND existing.deleted_at IS NULL
      );

    UPDATE iam_role_permission role_permission
    SET deleted_by = v_operator,
        delete_reason = 'tenant permission entitlement reduced',
        deleted_at = now(),
        updated_by = v_operator,
        updated_at = now()
    WHERE role_permission.tenant_id = p_tenant_id
      AND role_permission.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM iam_tenant_permission_entitlement entitlement
          WHERE entitlement.tenant_id = role_permission.tenant_id
            AND entitlement.permission_id = role_permission.permission_id
            AND entitlement.deleted_at IS NULL
            AND entitlement.is_enabled = true
      );

    WITH RECURSIVE active_user AS (
        SELECT iam_user.tenant_id,
               iam_user.id AS user_id,
               iam_user.auth_version
        FROM iam_user
        WHERE iam_user.tenant_id = p_tenant_id
          AND iam_user.deleted_at IS NULL
          AND iam_user.is_enabled = true
    ),
    active_user_role AS (
        SELECT user_role.tenant_id,
               user_role.user_id,
               role.id AS role_id
        FROM iam_user_role user_role
        JOIN iam_role role ON role.id = user_role.role_id
                           AND role.tenant_id = user_role.tenant_id
                           AND role.deleted_at IS NULL
                           AND role.is_enabled = true
        JOIN active_user ON active_user.tenant_id = user_role.tenant_id
                        AND active_user.user_id = user_role.user_id
        WHERE user_role.deleted_at IS NULL
    ),
    user_permission AS (
        SELECT DISTINCT active_user_role.tenant_id,
               active_user_role.user_id,
               permission.code
        FROM active_user_role
        JOIN iam_role_permission role_permission
          ON role_permission.tenant_id = active_user_role.tenant_id
         AND role_permission.role_id = active_user_role.role_id
         AND role_permission.deleted_at IS NULL
        JOIN iam_tenant_permission_entitlement entitlement
          ON entitlement.tenant_id = role_permission.tenant_id
         AND entitlement.permission_id = role_permission.permission_id
         AND entitlement.deleted_at IS NULL
         AND entitlement.is_enabled = true
        JOIN iam_permission permission
          ON permission.id = role_permission.permission_id
         AND permission.deleted_at IS NULL
         AND permission.is_enabled = true
    ),
    direct_menu AS (
        SELECT DISTINCT active_user.tenant_id,
               active_user.user_id,
               menu.id AS menu_id,
               menu.parent_id
        FROM active_user
        JOIN iam_menu menu ON menu.deleted_at IS NULL
                          AND menu.is_enabled = true
        WHERE menu.permission_code IS NULL
           OR menu.permission_code = ''
           OR EXISTS (
               SELECT 1
               FROM user_permission
               WHERE user_permission.tenant_id = active_user.tenant_id
                 AND user_permission.user_id = active_user.user_id
                 AND user_permission.code = menu.permission_code
           )
    ),
    menu_tree(tenant_id, user_id, menu_id, parent_id) AS (
        SELECT direct_menu.tenant_id,
               direct_menu.user_id,
               direct_menu.menu_id,
               direct_menu.parent_id
        FROM direct_menu
        UNION
        SELECT menu_tree.tenant_id,
               menu_tree.user_id,
               parent.id,
               parent.parent_id
        FROM menu_tree
        JOIN iam_menu parent ON parent.id = menu_tree.parent_id
                            AND parent.deleted_at IS NULL
                            AND parent.is_enabled = true
    ),
    rebuilt_snapshot AS (
        SELECT active_user.tenant_id,
               active_user.user_id,
               COALESCE(existing_snapshot.auth_version, active_user.auth_version, 0) + 1 AS auth_version,
               COALESCE((
                   SELECT jsonb_agg(role_id ORDER BY role_id)
                   FROM (
                       SELECT DISTINCT active_user_role.role_id
                       FROM active_user_role
                       WHERE active_user_role.tenant_id = active_user.tenant_id
                         AND active_user_role.user_id = active_user.user_id
                   ) role_item
               ), '[]'::jsonb) AS role_ids,
               COALESCE((
                   SELECT jsonb_agg(code ORDER BY code)
                   FROM (
                       SELECT DISTINCT user_permission.code
                       FROM user_permission
                       WHERE user_permission.tenant_id = active_user.tenant_id
                         AND user_permission.user_id = active_user.user_id
                   ) permission_item
               ), '[]'::jsonb) AS permission_codes,
               COALESCE((
                   SELECT jsonb_agg(menu_code.code ORDER BY menu_code.sort_no, menu_code.code)
                   FROM (
                       SELECT DISTINCT menu.code,
                              menu.sort_no
                       FROM menu_tree
                       JOIN iam_menu menu ON menu.id = menu_tree.menu_id
                       WHERE menu_tree.tenant_id = active_user.tenant_id
                         AND menu_tree.user_id = active_user.user_id
                         AND menu.deleted_at IS NULL
                         AND menu.is_enabled = true
                   ) menu_code
               ), '[]'::jsonb) AS menu_codes,
               COALESCE(existing_snapshot.column_settings, '{}'::jsonb) AS column_settings
        FROM active_user
        LEFT JOIN iam_authorization_snapshot existing_snapshot
          ON existing_snapshot.tenant_id = active_user.tenant_id
         AND existing_snapshot.user_id = active_user.user_id
    )
    INSERT INTO iam_authorization_snapshot (
        tenant_id, user_id, auth_version, role_ids, permission_codes, menu_codes, column_settings,
        snapshot_hash, built_at, created_by, updated_by
    )
    SELECT rebuilt_snapshot.tenant_id,
           rebuilt_snapshot.user_id,
           rebuilt_snapshot.auth_version,
           rebuilt_snapshot.role_ids,
           rebuilt_snapshot.permission_codes,
           rebuilt_snapshot.menu_codes,
           rebuilt_snapshot.column_settings,
           'tenant-admin-entitlement-v29:' || rebuilt_snapshot.tenant_id || ':' || rebuilt_snapshot.user_id,
           now(),
           v_operator,
           v_operator
    FROM rebuilt_snapshot
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
END;
$$;

COMMENT ON FUNCTION sync_tenant_admin_role_permissions_with_entitlements(bigint, varchar)
    IS '将租户权限池全集自动同步给 tenant_admin 角色，并把该角色授予租户管理员成员。普通角色仍由租户管理员手动分配。';

CREATE OR REPLACE FUNCTION sync_tenant_admin_role_permissions_after_bootstrap_task()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.tenant_id > 0 AND NEW.status = 'SUCCEEDED' THEN
        PERFORM sync_tenant_admin_role_permissions_with_entitlements(
            NEW.tenant_id,
            COALESCE(NEW.updated_by, NEW.created_by, NEW.requested_by, 'system-v29')
        );
    END IF;
    RETURN NEW;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_sync_tenant_admin_role_permissions_after_bootstrap_task'
    ) THEN
        EXECUTE 'CREATE TRIGGER trg_sync_tenant_admin_role_permissions_after_bootstrap_task
            AFTER INSERT OR UPDATE OF status ON iam_tenant_bootstrap_task
            FOR EACH ROW
            EXECUTE FUNCTION sync_tenant_admin_role_permissions_after_bootstrap_task()';
    END IF;
END;
$$;

WITH active_entitlement_tenant AS (
    SELECT DISTINCT entitlement.tenant_id
    FROM iam_tenant_permission_entitlement entitlement
    WHERE entitlement.tenant_id > 0
      AND entitlement.deleted_at IS NULL
      AND entitlement.is_enabled = true
)
SELECT sync_tenant_admin_role_permissions_with_entitlements(tenant_id, 'system-v29')
FROM active_entitlement_tenant;
