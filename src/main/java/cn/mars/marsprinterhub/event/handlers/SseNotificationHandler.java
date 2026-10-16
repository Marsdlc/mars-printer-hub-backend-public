package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.SseNotificationEvent;
import cn.mars.marsprinterhub.sse.service.SseService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SSE 通知事件处理器
 * 处理设备状态变化事件，向前端发送 SSE 通知。
 */
@Slf4j
@Component
public class SseNotificationHandler implements EventHandler<SseNotificationEvent> {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private SseService sseService;

    @Resource
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(SseNotificationEvent.class, this);
        log.info("SSE通知事件处理器已注册");
    }

    @Override
    public void handle(SseNotificationEvent event) {
        if (!isIntranet) {
            return;
        }
        try {
            sseService.notifyDeviceUpdate(event.getUserId(), event.getDeviceId());
        } catch (Exception e) {
            log.error("处理SSE通知事件失败: deviceId={}, error={}",
                    event.getDeviceId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
