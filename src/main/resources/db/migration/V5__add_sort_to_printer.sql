ALTER TABLE printers ADD COLUMN sort INT DEFAULT 0 COMMENT '排序字段，值越小越靠前' AFTER `ip_address`;
