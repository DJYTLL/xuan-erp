-- 放宽授权快照哈希字段，避免历史长快照或未来摘要格式超过 128 字符。
ALTER TABLE iam_authorization_snapshot
    ALTER COLUMN snapshot_hash TYPE varchar(256);
