-- Retire legacy navigation entries that do not have real frontend pages.
-- V1-V21 history remains unchanged; this migration aligns IAM active menus with implemented routes.

WITH retired_menu(code) AS (
    VALUES
        ('party'),
        ('warehouse'),
        ('inventory'),
        ('sales'),
        ('finance'),
        ('document'),
        ('manufacturing'),
        ('report')
)
UPDATE iam_menu menu
SET is_enabled = false,
    updated_by = 'system',
    updated_at = now()
FROM retired_menu
WHERE menu.code = retired_menu.code
  AND menu.deleted_at IS NULL
  AND menu.is_enabled = true;

WITH retired_menu(code) AS (
    VALUES
        ('party'),
        ('warehouse'),
        ('inventory'),
        ('sales'),
        ('finance'),
        ('document'),
        ('manufacturing'),
        ('report')
)
UPDATE iam_tenant_menu tenant_menu
SET deleted_by = 'system',
    delete_reason = 'retired by V22; menu has no implemented frontend route',
    deleted_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM iam_menu menu
JOIN retired_menu ON retired_menu.code = menu.code
WHERE tenant_menu.menu_id = menu.id
  AND tenant_menu.deleted_at IS NULL
  AND menu.deleted_at IS NULL;

WITH retired_menu(code) AS (
    VALUES
        ('party'),
        ('warehouse'),
        ('inventory'),
        ('sales'),
        ('finance'),
        ('document'),
        ('manufacturing'),
        ('report')
),
snapshot_with_retired_menu AS (
    SELECT snapshot.id
    FROM iam_authorization_snapshot snapshot
    WHERE EXISTS (
        SELECT 1
        FROM retired_menu
        WHERE COALESCE(snapshot.menu_codes, '[]'::jsonb) ? retired_menu.code
    )
),
rebuilt_snapshot AS (
    SELECT snapshot.id,
           COALESCE((
               SELECT jsonb_agg(active_menu.code ORDER BY active_menu.sort_no, active_menu.code)
               FROM (
                   SELECT DISTINCT menu.code,
                          menu.sort_no
                   FROM jsonb_array_elements_text(COALESCE(snapshot.menu_codes, '[]'::jsonb)) AS snapshot_menu(code)
                   JOIN iam_menu menu ON menu.code = snapshot_menu.code
                   WHERE menu.deleted_at IS NULL
                     AND menu.is_enabled = true
                     AND NOT EXISTS (
                         SELECT 1
                         FROM retired_menu
                         WHERE retired_menu.code = snapshot_menu.code
                     )
               ) active_menu
           ), '[]'::jsonb) AS menu_codes
    FROM iam_authorization_snapshot snapshot
    JOIN snapshot_with_retired_menu ON snapshot_with_retired_menu.id = snapshot.id
)
UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    menu_codes = rebuilt_snapshot.menu_codes,
    snapshot_hash = CASE
        WHEN snapshot.tenant_id = 0 THEN 'super_admin:retire-unimplemented-navigation-menus:' || snapshot.user_id
        ELSE 'retire-unimplemented-navigation-menus:' || snapshot.tenant_id || ':' || snapshot.user_id
    END,
    built_at = now(),
    updated_by = 'system',
    updated_at = now()
FROM rebuilt_snapshot
WHERE snapshot.id = rebuilt_snapshot.id;
