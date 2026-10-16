-- 添加 VIP 过期时间字段
ALTER TABLE user ADD COLUMN vip_expire_time DATETIME DEFAULT NULL COMMENT 'VIP过期时间';
