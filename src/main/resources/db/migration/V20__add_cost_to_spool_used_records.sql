ALTER TABLE spool_used_records
    ADD COLUMN cost DECIMAL(10, 2) NULL COMMENT '记录成本(元)' AFTER actual_weight;
