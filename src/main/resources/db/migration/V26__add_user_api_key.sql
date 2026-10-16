CREATE TABLE IF NOT EXISTS user_api_key
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id        BIGINT                             NOT NULL COMMENT '所属用户ID',
    name           VARCHAR(100)                       NOT NULL COMMENT 'API Key 名称',
    key_prefix     VARCHAR(20)                        NOT NULL COMMENT 'Key前缀，用于展示',
    secret_hash    VARCHAR(128)                       NOT NULL COMMENT 'Key哈希，不存明文',
    status         TINYINT  DEFAULT 1                 NOT NULL COMMENT '状态(0:禁用,1:启用)',
    last_used_time DATETIME                           NULL COMMENT '最后使用时间',
    last_used_ip   VARCHAR(45)                        NULL COMMENT '最后使用IP',
    create_user_id BIGINT                             NULL COMMENT '创建人ID',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_uak_status (status),
    KEY idx_uak_prefix (key_prefix),
    UNIQUE KEY uk_uak_user_id (user_id),
    UNIQUE KEY uk_uak_secret_hash (secret_hash)
) COMMENT ='用户API Key表' CHARSET = utf8mb4;
