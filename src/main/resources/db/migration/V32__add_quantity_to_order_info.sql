ALTER TABLE order_info
    ADD COLUMN quantity INT NOT NULL DEFAULT 0 COMMENT '数量' AFTER total_income;
