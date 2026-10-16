-- Add task_id column
ALTER TABLE spool_used_records
    ADD COLUMN task_id BIGINT COMMENT '任务ID' AFTER spool_id;

-- Modify spool_id to be nullable
ALTER TABLE spool_used_records
    MODIFY COLUMN spool_id BIGINT NULL COMMENT '耗材id';
