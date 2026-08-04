CREATE TABLE IF NOT EXISTS iam_tenant_permission_sync_state (
    id bigserial,
    tenant_id bigint NOT NULL,
    last_synced_permission_hash varchar(128),
    last_synced_at timestamptz,
    last_sync_source varchar(64),
    last_error_code varchar(64),
    last_error_message varchar(500),
    created_by varchar(64),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_by varchar(64),
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pk_iam_tenant_permission_sync_state PRIMARY KEY (id),
    CONSTRAINT uk_iam_tenant_permission_sync_state_tenant UNIQUE (tenant_id)
);

COMMENT ON TABLE iam_tenant_permission_sync_state IS 'IAM 租户权限同步状态表，记录 Tenant 套餐权限指纹最后一次成功同步情况。';
COMMENT ON COLUMN iam_tenant_permission_sync_state.tenant_id IS '租户 ID';
COMMENT ON COLUMN iam_tenant_permission_sync_state.last_synced_permission_hash IS 'IAM 最后一次成功同步的 Tenant 套餐权限指纹';
COMMENT ON COLUMN iam_tenant_permission_sync_state.last_synced_at IS '最后一次成功同步时间';
COMMENT ON COLUMN iam_tenant_permission_sync_state.last_sync_source IS '最后一次同步来源';
COMMENT ON COLUMN iam_tenant_permission_sync_state.last_error_code IS '预留：最后一次同步失败错误码';
COMMENT ON COLUMN iam_tenant_permission_sync_state.last_error_message IS '预留：最后一次同步失败错误信息';

CREATE INDEX IF NOT EXISTS idx_iam_tenant_permission_sync_state_hash
    ON iam_tenant_permission_sync_state (last_synced_permission_hash);
