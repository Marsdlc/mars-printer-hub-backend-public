package cn.mars.marsprinterhub.event.config;

import cn.mars.marsprinterhub.event.core.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

/**
 * 事件系统配置类
 * 统一使用 taskExecutor，减少线程池数量
 */
@Slf4j
@Configuration
public class EventConfig {

    /**
     * 配置事件总线
     * 使用 @Primary 标记的 taskExecutor，统一所有异步任务
     */
    @Bean
    public EventBus eventBus(Executor taskExecutor) {
        log.info("初始化事件总线（统一使用 taskExecutor）");
        return new EventBus(taskExecutor);
    }
}
