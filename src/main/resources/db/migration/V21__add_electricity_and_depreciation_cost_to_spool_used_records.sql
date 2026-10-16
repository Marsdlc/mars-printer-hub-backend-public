ALTER TABLE spool_used_records
    ADD COLUMN electricity_cost DECIMAL(10, 2) NULL COMMENT '电费(元)' AFTER cost,
    ADD COLUMN depreciation_cost DECIMAL(10, 2) NULL COMMENT '折旧费(元)' AFTER electricity_cost;
