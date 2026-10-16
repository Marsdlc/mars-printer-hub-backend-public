-- V4__add_cost_columns_to_order_info.sql
ALTER TABLE `order_info`
    ADD COLUMN `electricity_cost` DECIMAL(10, 2) DEFAULT '0.00' COMMENT '电力成本' AFTER `material_cost`,
    ADD COLUMN `logistics_cost` DECIMAL(10, 2) DEFAULT '0.00' COMMENT '物流成本' AFTER `electricity_cost`,
    ADD COLUMN `design_cost`    DECIMAL(10, 2) DEFAULT '0.00' COMMENT '设计成本' AFTER `logistics_cost`,
    ADD COLUMN `labor_cost`     DECIMAL(10, 2) DEFAULT '0.00' COMMENT '人工成本' AFTER `design_cost`;