-- Restore full navigation entries now that the frontend provides real routes.
-- V1-V23 history remains unchanged; this migration repairs the active data state.

WITH menu_mapping(code, parent_code, sort_no) AS (
    VALUES
        ('party', 'base-data', 20),
        ('warehouse', 'base-data', 30),
        ('inventory', 'stock-management', 10),
        ('manufacturing', 'stock-management', 20),
        ('sales', 'sales-management', 10),
        ('finance', NULL::varchar, 80),
        ('document', NULL::varchar, 90),
        ('report', NULL::varchar, 110)
)
UPDATE iam_menu menu
SET parent_id = parent.id,
    sort_no = menu_mapping.sort_no,
    is_enabled = true,
    updated_by = 'system',
    updated_at = now()
FROM menu_mapping
LEFT JOIN iam_menu parent
  ON parent.code = menu_mapping.parent_code
 AND parent.deleted_at IS NULL
WHERE menu.code = menu_mapping.code
  AND menu.deleted_at IS NULL
  AND (
      menu.parent_id IS DISTINCT FROM parent.id
      OR menu.sort_no IS DISTINCT FROM menu_mapping.sort_no
      OR menu.is_enabled = false
  );

WITH menu_mapping(code) AS (
    VALUES
        ('party'),
        ('warehouse'),
        ('inventory'),
        ('manufacturing'),
        ('sales'),
        ('finance'),
        ('document'),
        ('report')
),
tenant_scope(tenant_id) AS (
    SELECT 0::bigint
    UNION
    SELECT DISTINCT tenant_id
    FROM iam_user
    WHERE deleted_at IS NULL
    UNION
    SELECT DISTINCT tenant_id
    FROM iam_authorization_snapshot
    UNION
    SELECT DISTINCT tenant_id
    FROM iam_tenant_menu
),
restored_menu AS (
    SELECT menu.id
    FROM iam_menu menu
    JOIN menu_mapping ON menu_mapping.code = menu.code
    WHERE menu.deleted_at IS NULL
      AND menu.is_enabled = true
)
INSERT INTO iam_tenant_menu (tenant_id, menu_id, is_enabled, created_by, updated_by)
SELECT tenant_scope.tenant_id,
       restored_menu.id,
       true,
       'system',
       'system'
FROM tenant_scope
CROSS JOIN restored_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_tenant_menu existing
    WHERE existing.tenant_id = tenant_scope.tenant_id
      AND existing.menu_id = restored_menu.id
      AND existing.deleted_at IS NULL
);

WITH RECURSIVE snapshot_direct_menu(snapshot_id, menu_id, parent_id) AS (
    SELECT snapshot.id,
           menu.id,
           menu.parent_id
    FROM iam_authorization_snapshot snapshot
    JOIN iam_menu menu
      ON menu.deleted_at IS NULL
     AND menu.is_enabled = true
    WHERE menu.permission_code IS NULL
       OR menu.permission_code = ''
       OR COALESCE(snapshot.permission_codes, '[]'::jsonb) ? '*'
       OR COALESCE(snapshot.permission_codes, '[]'::jsonb) ? menu.permission_code
),
snapshot_menu_tree(snapshot_id, menu_id, parent_id) AS (
    SELECT snapshot_id,
           menu_id,
           parent_id
    FROM snapshot_direct_menu
    UNION
    SELECT snapshot_menu_tree.snapshot_id,
           parent.id,
           parent.parent_id
    FROM snapshot_menu_tree
    JOIN iam_menu parent
      ON parent.id = snapshot_menu_tree.parent_id
     AND parent.deleted_at IS NULL
     AND parent.is_enabled = true
),
rebuilt_snapshot AS (
    SELECT snapshot_menu_tree.snapshot_id AS id,
           COALESCE((
               SELECT jsonb_agg(menu_code.code ORDER BY menu_code.sort_no, menu_code.code)
               FROM (
                   SELECT DISTINCT menu.code,
                          menu.sort_no
                   FROM snapshot_menu_tree tree_item
                   JOIN iam_menu menu ON menu.id = tree_item.menu_id
                   WHERE tree_item.snapshot_id = snapshot_menu_tree.snapshot_id
                     AND menu.deleted_at IS NULL
                     AND menu.is_enabled = true
               ) menu_code
           ), '[]'::jsonb) AS menu_codes
    FROM snapshot_menu_tree
    GROUP BY snapshot_menu_tree.snapshot_id
),
changed_snapshot AS (
    SELECT snapshot.id,
           rebuilt_snapshot.menu_codes
    FROM iam_authorization_snapshot snapshot
    JOIN rebuilt_snapshot ON rebuilt_snapshot.id = snapshot.id
    WHERE COALESCE((
              SELECT jsonb_agg(current_menu.code ORDER BY current_menu.code)
              FROM jsonb_array_elements_text(COALESCE(snapshot.menu_codes, '[]'::jsonb)) AS current_menu(code)
          ), '[]'::jsonb)
          IS DISTINCT FROM
          COALESCE((
              SELECT jsonb_agg(next_menu.code ORDER BY next_menu.code)
              FROM jsonb_array_elements_text(rebuilt_snapshot.menu_codes) AS next_menu(code)
          ), '[]'::jsonb)
)
UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    menu_codes = changed_snapshot.menu_codes,
    snapshot_hash = CASE
        WHEN snapshot.tenant_id = 0 THEN 'super_admin:restore-full-navigation-menus-v24:' || snapshot.user_id
        ELSE 'restore-full-navigation-menus-v24:' || snapshot.tenant_id || ':' || snapshot.user_id
    END,
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM changed_snapshot
WHERE snapshot.id = changed_snapshot.id;
