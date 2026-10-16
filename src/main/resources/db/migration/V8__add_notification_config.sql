CREATE TABLE `sys_notification_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `channel` varchar(32) NOT NULL COMMENT '渠道标识',
    `name` varchar(64) NOT NULL COMMENT '渠道名称',
    `config` text COMMENT '配置内容(JSON)',
    `enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用',
    `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知配置表';

-- 初始化默认数据
INSERT INTO `sys_notification_config` (`channel`, `name`, `config`, `enabled`, `sort`) VALUES
('bark', 'Bark', '{"serverUrl":"https://api.day.app","deviceKey":""}', 0, 10),
('feishu', '飞书', '{"webhookUrl":"","secret":""}', 0, 20),
('telegram', 'Telegram', '{"botToken":"","chatId":"","proxy":{"enabled":false,"host":"","port":null}}', 0, 30),
('gotify', 'Gotify', '{"serverUrl":"","appToken":"","priority":5}', 0, 40);
