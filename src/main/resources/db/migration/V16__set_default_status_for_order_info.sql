ALTER TABLE `order_info`
    MODIFY COLUMN `status` TINYINT DEFAULT 1 COMMENT '订单状态 1-进行中 2-已完成 3-已取消 4-库存中';
