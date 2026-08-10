-- 为租户状态流转追加状态动作权限种子。
-- 基础权限仍由 tenant:enable / tenant:disable / tenant:delete 控制；状态动作权限只进一步限制“当前状态下是否允许执行该动作”。

WITH tenant_states(resource_key, state_code, state_name, description, sort_no) AS (
    VALUES
        ('tenant', 'PROVISIONING', '初始化中', '租户正在初始化，暂不允许人工启停和删除', 10),
        ('tenant', 'PROVISIONED', '已开通', '租户已完成初始化，可启用或删除', 20),
        ('tenant', 'ENABLED', '启用', '租户正在正常使用，可停用', 30),
        ('tenant', 'SUSPENDED', '暂停', '租户被暂停，后续按业务规则扩展动作', 40),
        ('tenant', 'DISABLED', '停用', '租户已停用，可再次启用或删除', 50)
)
INSERT INTO iam_resource_state (
    tenant_id, resource_key, state_code, state_name, description, sort_no, is_enabled, created_by, updated_by
)
SELECT 0, resource_key, state_code, state_name, description, sort_no, true, 'system-v49', 'system-v49'
FROM tenant_states
ON CONFLICT (tenant_id, resource_key, state_code) WHERE deleted_at IS NULL DO UPDATE
SET state_name = EXCLUDED.state_name,
    description = EXCLUDED.description,
    sort_no = EXCLUDED.sort_no,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v49',
    updated_at = now();

WITH tenant_actions(resource_key, action_code, action_name, permission_code, description, sort_no) AS (
    VALUES
        ('tenant', 'enable', '启用租户', 'tenant:enable', '把已开通或已停用租户切换为启用', 10),
        ('tenant', 'disable', '停用租户', 'tenant:disable', '把启用中的租户切换为停用', 20),
        ('tenant', 'delete', '删除租户', 'tenant:delete', '软删除非启用状态租户', 30)
)
INSERT INTO iam_resource_action (
    tenant_id, resource_key, action_code, action_name, permission_code, description, sort_no, is_enabled, created_by, updated_by
)
SELECT 0, resource_key, action_code, action_name, permission_code, description, sort_no, true, 'system-v49', 'system-v49'
FROM tenant_actions
ON CONFLICT (tenant_id, resource_key, action_code) WHERE deleted_at IS NULL DO UPDATE
SET action_name = EXCLUDED.action_name,
    permission_code = EXCLUDED.permission_code,
    description = EXCLUDED.description,
    sort_no = EXCLUDED.sort_no,
    is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v49',
    updated_at = now();

WITH allowed_state_action(resource_state_key, action_code) AS (
    VALUES
        ('tenant:PROVISIONED', 'enable'),
        ('tenant:DISABLED', 'enable'),
        ('tenant:ENABLED', 'disable'),
        ('tenant:PROVISIONED', 'delete'),
        ('tenant:DISABLED', 'delete')
),
expanded_allowed_state_action(resource_key, state_code, action_code) AS (
    SELECT split_part(resource_state_key, ':', 1), split_part(resource_state_key, ':', 2), action_code
    FROM allowed_state_action
),
role_allowed_state_action AS (
    SELECT DISTINCT role_permission.tenant_id,
           role_permission.role_id,
           expanded.resource_key,
           expanded.state_code,
           expanded.action_code,
           COALESCE(role_permission.created_by, 'system-v49') AS created_by
    FROM expanded_allowed_state_action expanded
    JOIN iam_resource_action action
      ON action.tenant_id = 0
     AND action.resource_key = expanded.resource_key
     AND action.action_code = expanded.action_code
     AND action.deleted_at IS NULL
     AND action.is_enabled = true
    JOIN iam_permission permission
      ON permission.code = action.permission_code
     AND permission.deleted_at IS NULL
     AND permission.is_enabled = true
    JOIN iam_role_permission role_permission
      ON role_permission.permission_id = permission.id
     AND role_permission.deleted_at IS NULL
)
INSERT INTO iam_role_state_action_rule (
    tenant_id, role_id, resource_key, state_code, action_code, is_enabled, created_by, updated_by
)
SELECT tenant_id, role_id, resource_key, state_code, action_code, true, created_by, 'system-v49'
FROM role_allowed_state_action
ON CONFLICT (tenant_id, role_id, resource_key, state_code, action_code) WHERE deleted_at IS NULL DO UPDATE
SET is_enabled = true,
    deleted_by = NULL,
    delete_reason = NULL,
    deleted_at = NULL,
    updated_by = 'system-v49',
    updated_at = now();

UPDATE iam_authorization_snapshot snapshot
SET auth_version = snapshot.auth_version + 1,
    snapshot_hash = 'state-action-tenant-v49:' || snapshot.tenant_id || ':' || snapshot.user_id,
    updated_by = 'system-v49',
    updated_at = now()
WHERE EXISTS (
    SELECT 1
    FROM iam_user_role user_role
    JOIN iam_role_state_action_rule rule
      ON rule.tenant_id = user_role.tenant_id
     AND rule.role_id = user_role.role_id
     AND rule.deleted_at IS NULL
     AND rule.is_enabled = true
    WHERE user_role.tenant_id = snapshot.tenant_id
      AND user_role.user_id = snapshot.user_id
      AND user_role.deleted_at IS NULL
);
