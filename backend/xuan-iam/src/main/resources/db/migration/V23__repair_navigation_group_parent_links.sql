-- Repair navigation group parent links left orphaned by the historical V8 insert.
-- V1-V22 history remains unchanged; this migration only restores the intended menu hierarchy.

WITH parent_mapping(child_code, parent_code, sort_no) AS (
    VALUES
        ('base-data', 'inventory-root', 10),
        ('stock-management', 'inventory-root', 20),
        ('purchase-management', 'inventory-root', 30),
        ('sales-management', 'inventory-root', 40)
)
UPDATE iam_menu child
SET parent_id = parent.id,
    sort_no = parent_mapping.sort_no,
    is_enabled = true,
    updated_by = 'system',
    updated_at = now()
FROM parent_mapping
JOIN iam_menu parent
  ON parent.code = parent_mapping.parent_code
 AND parent.deleted_at IS NULL
WHERE child.code = parent_mapping.child_code
  AND child.deleted_at IS NULL
  AND (
      child.parent_id IS DISTINCT FROM parent.id
      OR child.sort_no IS DISTINCT FROM parent_mapping.sort_no
      OR child.is_enabled = false
  );

WITH required_menu(code, sort_no) AS (
    VALUES
        ('inventory-root', 10),
        ('base-data', 20),
        ('stock-management', 30),
        ('purchase-management', 40),
        ('sales-management', 50)
),
snapshot_needing_repair AS (
    SELECT snapshot.id
    FROM iam_authorization_snapshot snapshot
    WHERE EXISTS (
        SELECT 1
        FROM required_menu
        WHERE NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? required_menu.code)
    )
)
UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    menu_codes = COALESCE(snapshot.menu_codes, '[]'::jsonb)
        || COALESCE((
            SELECT jsonb_agg(required_menu.code ORDER BY required_menu.sort_no)
            FROM required_menu
            WHERE NOT (COALESCE(snapshot.menu_codes, '[]'::jsonb) ? required_menu.code)
        ), '[]'::jsonb),
    snapshot_hash = snapshot.snapshot_hash || '|navigation-group-parent-links-v23',
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM snapshot_needing_repair
WHERE snapshot.id = snapshot_needing_repair.id;
