package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.PrintTaskCompletedEvent;
import cn.mars.marsprinterhub.processor.AutoSpoolProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自动线轴处理事件处理器
 * 处理打印任务完成事件，执行自动线轴处理
 */
@Slf4j
@Component
public class PrintTaskCompletedEventHandler implements EventHandler<PrintTaskCompletedEvent> {

    @Resource
    private AutoSpoolProcessor autoSpoolProcessor;

    @Resource
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(PrintTaskCompletedEvent.class, this);
        log.info("打印任务结束处理事件处理器已注册");
    }

    @Override
    public void handle(PrintTaskCompletedEvent event) {
        try {
            String deviceId = event.getDeviceId();
            Integer finalProgress = event.getFinalProgress();

            log.info("处理打印任务完成事件，执行自动线轴处理: deviceId={}, progress={}, status={}",
                    deviceId, finalProgress, event.getCompletionStatus());

            autoSpoolProcessor.execute(deviceId, finalProgress, null);
        } catch (Exception e) {
            log.error("处理自动线轴事件失败: deviceId={}, error={}",
                    event.getDeviceId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean isAsync() {
        return true; // 异步处理
    }
}