ALTER TABLE order_info ADD COLUMN images JSON COMMENT '订单相关图片URL列表';
ALTER TABLE order_info ADD COLUMN models JSON COMMENT '订单关联模型URL列表';
