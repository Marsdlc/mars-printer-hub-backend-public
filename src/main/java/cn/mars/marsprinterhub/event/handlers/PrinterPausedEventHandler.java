package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.PrinterPausedEvent;
import cn.mars.marsprinterhub.event.events.SseNotificationEvent;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.notification.template.PrinterNotificationTemplate;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;

/**
 * 打印暂停事件处理器
 * 当检测到打印状态由 RUNNING 变为 PAUSE 时发送通知
 */
@Slf4j
@Component
public class PrinterPausedEventHandler implements EventHandler<PrinterPausedEvent> {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private PrinterService printerService;

    @Resource
    private PrinterNotificationTemplate printerNotificationTemplate;

    @Resource
    private EventBus eventBus;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private RedisUtils redisUtils;

    @PostConstruct
    public void init() {
        eventBus.register(PrinterPausedEvent.class, this);
        log.info("打印暂停事件处理器已注册");
    }

    @Override
    public void handle(PrinterPausedEvent event) {
        if (!isIntranet) {
            return;
        }
        try {
            String deviceId = event.getDeviceId();
            Printer printer = printerService.getByDeviceId(deviceId);
            if (printer == null) {
                log.warn("未找到设备ID为 {} 的打印机", deviceId);
                return;
            }

            printerNotificationTemplate.sendPrinterPausedNotification(printer.getName());
            publishSseNotification(deviceId);
        } catch (Exception e) {
            log.error("处理打印暂停事件失败: deviceId={}, error={}",
                    event.getDeviceId(), e.getMessage(), e);
        }
    }

    private void publishSseNotification(String deviceId) {
        Object userId = redisUtils.get(DEVICE_USER_PREFIX + deviceId);
        if (userId != null) {
            SseNotificationEvent sseNotificationEvent = new SseNotificationEvent(
                    Long.parseLong(userId.toString()), deviceId);
            eventPublisher.publish(sseNotificationEvent);
        }
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
