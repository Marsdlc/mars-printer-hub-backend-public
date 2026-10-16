/*
 * Add ams_ref column to spool_used_records
 * Add unique constraint on (task_id, ams_ref)
 */

-- Add ams_ref column
ALTER TABLE spool_used_records
    ADD COLUMN ams_ref INT COMMENT 'AMS槽位索引(0-15)' AFTER task_id;

-- Add unique constraint
ALTER TABLE spool_used_records
    ADD UNIQUE KEY uk_task_ams (task_id, ams_ref);
