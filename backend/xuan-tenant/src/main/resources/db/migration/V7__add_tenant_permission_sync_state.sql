ALTER TABLE tenant_plan_assignment
    ADD COLUMN IF NOT EXISTS permission_sync_expected_hash varchar(128),
    ADD COLUMN IF NOT EXISTS permission_sync_status varchar(30) DEFAULT 'PENDING_REPAIR' NOT NULL,
    ADD COLUMN IF NOT EXISTS permission_sync_last_checked_at timestamptz,
    ADD COLUMN IF NOT EXISTS permission_sync_last_synced_at timestamptz,
    ADD COLUMN IF NOT EXISTS permission_sync_last_error_code varchar(64),
    ADD COLUMN IF NOT EXISTS permission_sync_last_error_message varchar(500);

ALTER TABLE tenant_plan_assignment
    DROP CONSTRAINT IF EXISTS chk_tenant_plan_assignment_permission_sync_status;

ALTER TABLE tenant_plan_assignment
    ADD CONSTRAINT chk_tenant_plan_assignment_permission_sync_status
        CHECK (permission_sync_status IN ('SYNCED', 'PENDING_REPAIR', 'REPAIRING', 'FAILED'));

COMMENT ON COLUMN tenant_plan_assignment.permission_sync_expected_hash IS 'Tenant 根据当前套餐页面权限模板和列权限模板计算出的期望权限指纹';
COMMENT ON COLUMN tenant_plan_assignment.permission_sync_status IS '套餐权限同步状态：SYNCED 正常，PENDING_REPAIR 待修复，REPAIRING 修复中，FAILED 修复失败';
COMMENT ON COLUMN tenant_plan_assignment.permission_sync_last_checked_at IS '最近一次检查权限同步状态的时间';
COMMENT ON COLUMN tenant_plan_assignment.permission_sync_last_synced_at IS '最近一次成功同步到 IAM 的时间';
COMMENT ON COLUMN tenant_plan_assignment.permission_sync_last_error_code IS '最近一次同步失败错误码';
COMMENT ON COLUMN tenant_plan_assignment.permission_sync_last_error_message IS '最近一次同步失败错误信息';

CREATE INDEX IF NOT EXISTS idx_tenant_plan_assignment_permission_sync_status
    ON tenant_plan_assignment (permission_sync_status, deleted_at);
