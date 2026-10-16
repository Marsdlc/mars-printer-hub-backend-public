ALTER TABLE user
    ADD COLUMN bambu_region VARCHAR(16) NOT NULL DEFAULT 'China' COMMENT '拓竹云区域（China / Other）' AFTER bambu_token_expire_time;
