ALTER TABLE user
    ADD COLUMN last_login_time DATETIME NULL COMMENT '最后登录时间' AFTER vip_expire_time;
