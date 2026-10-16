ALTER TABLE spool_used_records
    ADD COLUMN device_id VARCHAR(100) NULL COMMENT '设备ID' AFTER task_id;
