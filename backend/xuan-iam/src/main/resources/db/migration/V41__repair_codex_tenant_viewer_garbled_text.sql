-- 修复 dev 环境历史写入的 codex_tenant_viewer 乱码文本。
-- 只在字段中已经出现问号坏数据时覆盖，避免影响后续已经人工修正过的记录。

UPDATE iam_role
SET name = CASE
               WHEN position('?' in coalesce(name, '')) > 0 OR coalesce(name, '') = ''
                   THEN 'Codex 租户查看员'
               ELSE name
    END,
    description = CASE
                      WHEN position('?' in coalesce(description, '')) > 0 OR coalesce(description, '') = ''
                          THEN 'Codex 租户查看员，默认拥有 tenant:view 权限'
                      ELSE description
        END,
    updated_by = 'system-v41',
    updated_at = now()
WHERE code = 'codex_tenant_viewer'
  AND deleted_at IS NULL
  AND (
    position('?' in coalesce(name, '')) > 0
        OR position('?' in coalesce(description, '')) > 0
        OR coalesce(name, '') = ''
        OR coalesce(description, '') = ''
    );

UPDATE iam_user
SET display_name = CASE
                       WHEN position('?' in coalesce(display_name, '')) > 0 OR coalesce(display_name, '') = ''
                           THEN 'Codex 租户查看员'
                       ELSE display_name
    END,
    remark = CASE
                 WHEN position('?' in coalesce(remark, '')) > 0 OR coalesce(remark, '') = ''
                     THEN 'Codex 租户查看员备注'
                 ELSE remark
        END,
    updated_by = 'system-v41',
    updated_at = now()
WHERE username = 'codex_tenant_viewer'
  AND deleted_at IS NULL
  AND (
    position('?' in coalesce(display_name, '')) > 0
        OR position('?' in coalesce(remark, '')) > 0
        OR coalesce(display_name, '') = ''
        OR coalesce(remark, '') = ''
    );
