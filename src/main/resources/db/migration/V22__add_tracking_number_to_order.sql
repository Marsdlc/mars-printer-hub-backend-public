ALTER TABLE order_info
    ADD COLUMN tracking_number varchar(255) DEFAULT NULL COMMENT '物流单号' AFTER logistics_cost;
