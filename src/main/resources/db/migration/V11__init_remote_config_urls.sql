-- 初始化远程配置 URL
-- 使用 JSON 格式存储三个远程配置地址，只需一次查询
INSERT INTO sys_app_config (config_key, config_value)
VALUES ('remote_config_urls', '{"colorUrl":"https://spool-r2.bambu.cc/color.json","filamentsUrl":"https://spool-r2.bambu.cc/filaments.json","spoolUrl":"https://spool-r2.bambu.cc/spoolConfig.json"}')
ON DUPLICATE KEY UPDATE config_value = config_value;
