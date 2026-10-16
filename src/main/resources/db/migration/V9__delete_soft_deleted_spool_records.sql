-- 删除耗材使用记录中的逻辑删除数据
-- 由于已移除逻辑删除功能，需要清理历史软删除数据

DELETE FROM spool_used_records WHERE is_delete = 1;
