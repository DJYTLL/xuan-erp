-- Backfill missing IAM tenant init-template bindings from active tenant plan assignments when
-- xuan-tenant tables are co-located in the same database. Then rebuild only those repaired tenants.

DO $$
BEGIN
    IF to_regclass('tenant_plan_assignment') IS NOT NULL
       AND to_regclass('tenant_plan') IS NOT NULL THEN
        EXECUTE $sql$
            WITH active_assignment AS (
                SELECT DISTINCT ON (assignment.tenant_id)
                       assignment.tenant_id,
                       assignment.plan_id
                FROM tenant_plan_assignment assignment
                WHERE assignment.deleted_at IS NULL
                  AND assignment.status = 'ACTIVE'
                  AND assignment.tenant_id > 0
                ORDER BY assignment.tenant_id,
                         assignment.effective_at DESC,
                         assignment.assigned_at DESC,
                         assignment.id DESC
            ),
            plan_template AS (
                SELECT active_assignment.tenant_id,
                       NULLIF(btrim(plan.feature_flags ->> 'iamInitTemplateCode'), '') AS init_template_code
                FROM active_assignment
                JOIN tenant_plan plan
                  ON plan.id = active_assignment.plan_id
                 AND plan.deleted_at IS NULL
                WHERE jsonb_exists(plan.feature_flags, 'iamInitTemplateCode')
            ),
            missing_binding AS (
                SELECT plan_template.tenant_id,
                       plan_template.init_template_code
                FROM plan_template
                JOIN iam_tenant_init_permission_template template
                  ON template.code = plan_template.init_template_code
                 AND template.deleted_at IS NULL
                 AND template.is_enabled = true
                WHERE plan_template.init_template_code IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM iam_tenant_init_template_binding binding
                      WHERE binding.tenant_id = plan_template.tenant_id
                        AND binding.deleted_at IS NULL
                        AND binding.is_enabled = true
                  )
            ),
            next_version AS (
                SELECT missing_binding.tenant_id,
                       COALESCE(MAX(entitlement.entitlement_version), 0) + 1 AS entitlement_version
                FROM missing_binding
                LEFT JOIN iam_tenant_permission_entitlement entitlement
                  ON entitlement.tenant_id = missing_binding.tenant_id
                GROUP BY missing_binding.tenant_id
            )
            INSERT INTO iam_tenant_init_template_binding (
                tenant_id, init_template_code, last_entitlement_version, is_enabled, created_by, updated_by
            )
            SELECT missing_binding.tenant_id,
                   missing_binding.init_template_code,
                   next_version.entitlement_version,
                   true,
                   'system-v28',
                   'system-v28'
            FROM missing_binding
            JOIN next_version ON next_version.tenant_id = missing_binding.tenant_id
            ON CONFLICT (tenant_id) WHERE deleted_at IS NULL DO UPDATE
            SET init_template_code = EXCLUDED.init_template_code,
                last_entitlement_version = EXCLUDED.last_entitlement_version,
                is_enabled = true,
                updated_by = 'system-v28',
                updated_at = now()
        $sql$;
    END IF;
END;
$$;

WITH repaired_binding AS (
    SELECT binding.tenant_id,
           binding.init_template_code
    FROM iam_tenant_init_template_binding binding
    JOIN iam_tenant_init_permission_template template
      ON template.code = binding.init_template_code
     AND template.deleted_at IS NULL
     AND template.is_enabled = true
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
      AND (binding.created_by = 'system-v28' OR binding.updated_by = 'system-v28')
),
next_version AS (
    SELECT repaired_binding.tenant_id,
           repaired_binding.init_template_code,
           COALESCE(MAX(entitlement.entitlement_version), 0) + 1 AS entitlement_version
    FROM repaired_binding
    LEFT JOIN iam_tenant_permission_entitlement entitlement
      ON entitlement.tenant_id = repaired_binding.tenant_id
    GROUP BY repaired_binding.tenant_id, repaired_binding.init_template_code
)
UPDATE iam_tenant_permission_entitlement entitlement
SET deleted_by = 'system-v28',
    delete_reason = 'rebuild by tenant plan template binding',
    deleted_at = now(),
    updated_by = 'system-v28',
    updated_at = now()
FROM next_version
WHERE entitlement.tenant_id = next_version.tenant_id
  AND entitlement.deleted_at IS NULL;

WITH repaired_binding AS (
    SELECT binding.tenant_id,
           binding.init_template_code,
           template.permission_codes
    FROM iam_tenant_init_template_binding binding
    JOIN iam_tenant_init_permission_template template
      ON template.code = binding.init_template_code
     AND template.deleted_at IS NULL
     AND template.is_enabled = true
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
      AND (binding.created_by = 'system-v28' OR binding.updated_by = 'system-v28')
),
next_version AS (
    SELECT repaired_binding.tenant_id,
           repaired_binding.init_template_code,
           COALESCE(MAX(entitlement.entitlement_version), 0) + 1 AS entitlement_version
    FROM repaired_binding
    LEFT JOIN iam_tenant_permission_entitlement entitlement
      ON entitlement.tenant_id = repaired_binding.tenant_id
    GROUP BY repaired_binding.tenant_id, repaired_binding.init_template_code
),
template_permission AS (
    SELECT DISTINCT repaired_binding.tenant_id,
           repaired_binding.init_template_code,
           permission_code.value AS permission_code
    FROM repaired_binding
    JOIN LATERAL jsonb_array_elements_text(COALESCE(repaired_binding.permission_codes, '[]'::jsonb)) AS permission_code(value) ON true
),
resolved_permission AS (
    SELECT template_permission.tenant_id,
           template_permission.init_template_code,
           permission.id AS permission_id
    FROM template_permission
    JOIN iam_permission permission
      ON permission.code = template_permission.permission_code
     AND permission.deleted_at IS NULL
     AND permission.is_enabled = true
)
INSERT INTO iam_tenant_permission_entitlement (
    tenant_id, init_template_code, permission_id, entitlement_version, is_enabled, created_by, updated_by
)
SELECT resolved_permission.tenant_id,
       resolved_permission.init_template_code,
       resolved_permission.permission_id,
       next_version.entitlement_version,
       true,
       'system-v28',
       'system-v28'
FROM resolved_permission
JOIN next_version
  ON next_version.tenant_id = resolved_permission.tenant_id
 AND next_version.init_template_code = resolved_permission.init_template_code
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_tenant_permission_entitlement existing
    WHERE existing.tenant_id = resolved_permission.tenant_id
      AND existing.permission_id = resolved_permission.permission_id
      AND existing.deleted_at IS NULL
);

WITH repaired_binding AS (
    SELECT binding.tenant_id
    FROM iam_tenant_init_template_binding binding
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
      AND (binding.created_by = 'system-v28' OR binding.updated_by = 'system-v28')
),
latest_version AS (
    SELECT entitlement.tenant_id,
           MAX(entitlement.entitlement_version) AS entitlement_version
    FROM iam_tenant_permission_entitlement entitlement
    JOIN repaired_binding ON repaired_binding.tenant_id = entitlement.tenant_id
    WHERE entitlement.deleted_at IS NULL
      AND entitlement.is_enabled = true
    GROUP BY entitlement.tenant_id
)
UPDATE iam_tenant_init_template_binding binding
SET last_entitlement_version = latest_version.entitlement_version,
    updated_by = 'system-v28',
    updated_at = now()
FROM latest_version
WHERE binding.tenant_id = latest_version.tenant_id
  AND binding.deleted_at IS NULL;

WITH repaired_binding AS (
    SELECT binding.tenant_id
    FROM iam_tenant_init_template_binding binding
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
      AND (binding.created_by = 'system-v28' OR binding.updated_by = 'system-v28')
)
UPDATE iam_role_permission role_permission
SET deleted_by = 'system-v28',
    delete_reason = 'tenant permission entitlement reduced',
    deleted_at = now(),
    updated_by = 'system-v28',
    updated_at = now()
FROM repaired_binding
WHERE role_permission.tenant_id = repaired_binding.tenant_id
  AND role_permission.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM iam_tenant_permission_entitlement entitlement
      WHERE entitlement.tenant_id = role_permission.tenant_id
        AND entitlement.permission_id = role_permission.permission_id
        AND entitlement.deleted_at IS NULL
        AND entitlement.is_enabled = true
  );

WITH RECURSIVE repaired_binding AS (
    SELECT binding.tenant_id
    FROM iam_tenant_init_template_binding binding
    WHERE binding.tenant_id > 0
      AND binding.deleted_at IS NULL
      AND binding.is_enabled = true
      AND (binding.created_by = 'system-v28' OR binding.updated_by = 'system-v28')
),
active_user AS (
    SELECT iam_user.tenant_id,
           iam_user.id AS user_id,
           iam_user.auth_version
    FROM iam_user
    JOIN repaired_binding ON repaired_binding.tenant_id = iam_user.tenant_id
    WHERE iam_user.deleted_at IS NULL
      AND iam_user.is_enabled = true
),
active_user_role AS (
    SELECT user_role.tenant_id,
           user_role.user_id,
           role.id AS role_id
    FROM iam_user_role user_role
    JOIN iam_role role
      ON role.id = user_role.role_id
     AND role.tenant_id = user_role.tenant_id
     AND role.deleted_at IS NULL
     AND role.is_enabled = true
    JOIN active_user
      ON active_user.tenant_id = user_role.tenant_id
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
    JOIN iam_menu menu
      ON menu.deleted_at IS NULL
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
    JOIN iam_menu parent
      ON parent.id = menu_tree.parent_id
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
       'tenant-entitlement-v28:' || rebuilt_snapshot.tenant_id || ':' || rebuilt_snapshot.user_id,
       now(),
       'system-v28',
       'system-v28'
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
