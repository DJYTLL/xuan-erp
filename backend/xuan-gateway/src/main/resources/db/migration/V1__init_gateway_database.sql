-- xuan-gateway initial database objects
-- Generated from the legacy wms_backend structure and current Xuan ERP service boundaries.
-- Run this migration after connecting Flyway to this service's own database.

-- Seata AT undo log table
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id bigint NOT NULL,
    xid varchar(128) NOT NULL,
    context varchar(128) NOT NULL,
    rollback_info bytea NOT NULL,
    log_status integer NOT NULL,
    log_created timestamp(6) NOT NULL,
    log_modified timestamp(6) NOT NULL,
    CONSTRAINT pk_undo_log PRIMARY KEY (branch_id, xid)
);
COMMENT ON TABLE undo_log IS 'Seata AT 模式回滚日志表。';
COMMENT ON COLUMN undo_log.branch_id IS 'Seata 分支事务 ID';
COMMENT ON COLUMN undo_log.xid IS 'Seata 全局事务 ID';
COMMENT ON COLUMN undo_log.context IS '上下文';
COMMENT ON COLUMN undo_log.rollback_info IS '回滚信息';
COMMENT ON COLUMN undo_log.log_status IS '日志状态';
COMMENT ON COLUMN undo_log.log_created IS '创建时间';
COMMENT ON COLUMN undo_log.log_modified IS '更新时间';
CREATE INDEX IF NOT EXISTS idx_undo_log_log_created ON undo_log (log_created);
