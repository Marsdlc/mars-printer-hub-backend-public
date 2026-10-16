ALTER TABLE user
    ADD COLUMN is_banned TINYINT DEFAULT 0 NULL COMMENT '是否封禁(0:否,1:是)' AFTER last_login_time,
    ADD COLUMN ban_reason VARCHAR(255) NULL COMMENT '封禁原因' AFTER is_banned,
    ADD COLUMN ban_time DATETIME NULL COMMENT '封禁时间' AFTER ban_reason;
