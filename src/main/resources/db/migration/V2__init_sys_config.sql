-- 使用 IF NOT EXISTS 是为了兼容那些已经通过旧版 Java 代码创建过此表的用户
-- 如果表存在，这句话会被忽略，不会报错；如果不存在，则创建。
CREATE TABLE IF NOT EXISTS sys_app_config (
                                              config_key VARCHAR(64) NOT NULL PRIMARY KEY,
    config_value VARCHAR(1024)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;