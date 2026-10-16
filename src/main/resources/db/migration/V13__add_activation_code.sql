-- VIP 激活码表
CREATE TABLE vip_activation_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE COMMENT '激活码',
    duration_days INT NOT NULL COMMENT '激活时长（天）',
    is_used TINYINT(1) DEFAULT 0 COMMENT '是否已使用',
    used_by BIGINT DEFAULT NULL COMMENT '使用者用户ID',
    used_time DATETIME DEFAULT NULL COMMENT '使用时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP激活码表';
