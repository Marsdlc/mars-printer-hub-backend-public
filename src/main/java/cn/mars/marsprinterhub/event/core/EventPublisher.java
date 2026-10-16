package cn.mars.marsprinterhub.event.core;

import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 事件发布器
 * 提供便捷的事件发布接口
 */
@Slf4j
@Component
public class EventPublisher {

    @Resource
    private EventBus eventBus;

    /**
     * 发布事件
     */
    public <T extends Event> void publish(T event) {
        try {
            eventBus.publish(event);
        } catch (Exception e) {
            log.error("[EventPublisher] 发布事件异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}