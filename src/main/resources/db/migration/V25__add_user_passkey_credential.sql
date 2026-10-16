CREATE TABLE IF NOT EXISTS user_passkey_credential
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id           BIGINT                             NOT NULL COMMENT '用户ID',
    credential_id     VARCHAR(512)                       NOT NULL COMMENT 'Base64Url 编码的 credentialId',
    public_key_cose   TEXT                               NOT NULL COMMENT 'Base64Url 编码的公钥 COSE',
    user_handle       VARCHAR(255)                       NOT NULL COMMENT 'Base64Url 编码的用户句柄',
    signature_count   BIGINT   DEFAULT 0                 NOT NULL COMMENT '签名计数器',
    aaguid            VARCHAR(64)                        NULL COMMENT '认证器 AAGUID',
    transports        VARCHAR(255)                       NULL COMMENT '支持的 transports，逗号分隔',
    attachment        VARCHAR(32)                        NULL COMMENT '认证器附件类型',
    nickname          VARCHAR(100)                       NULL COMMENT '凭证备注名',
    last_used_time    DATETIME                           NULL COMMENT '最后使用时间',
    is_delete         TINYINT  DEFAULT 0                 NULL COMMENT '是否删除(0:未删除,1:已删除)',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_upc_credential_id (credential_id),
    KEY idx_upc_user_id (user_id)
) COMMENT ='用户 Passkey 凭证表' CHARSET = utf8mb4;
