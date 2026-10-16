-- 给 spools 表添加 location 字段
-- 放在 price 字段之后
ALTER TABLE spools
    ADD COLUMN location VARCHAR(50) NULL COMMENT '存放位置' AFTER price;