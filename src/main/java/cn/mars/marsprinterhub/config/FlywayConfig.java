package cn.mars.marsprinterhub.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.output.RepairResult;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 配置类
 * <p>
 * 启动时先执行 repair() 再执行 migrate()，
 * 用于自动修复因迁移脚本内容变更导致的 checksum 不匹配问题。
 * repair() 仅更新 flyway_schema_history 表中的 checksum，不会修改数据库表结构和数据。
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("执行 Flyway repair 以修复可能的 checksum 不匹配...");
            RepairResult repairResult = flyway.repair();
            if (!repairResult.migrationsAligned.isEmpty()) {
                log.info("Flyway repair 已修复 {} 个迁移记录的 checksum",
                        repairResult.migrationsAligned.size());
            }
            flyway.migrate();
        };
    }
}
