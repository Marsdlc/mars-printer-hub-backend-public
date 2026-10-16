package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.AmsHumidityTooHighEvent;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.notification.template.PrinterNotificationTemplate;
import cn.mars.marsprinterhub.service.PrinterService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AMS湿度过高处理器
 * 当检测到AMS湿度过高时发送通知
 */
@Slf4j
@Component
public class AmsHumidityTooHighEventHandler implements EventHandler<AmsHumidityTooHighEvent> {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private PrinterService printerService;

    @Resource
    private PrinterNotificationTemplate printerNotificationTemplate;

    @Resource
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(AmsHumidityTooHighEvent.class, this);
        log.info("Ams湿度过高事件处理器已注册");
    }

    @Override
    public void handle(AmsHumidityTooHighEvent event) {
        if (!isIntranet) {
            return;
        }
        try {
            String deviceId = event.getDeviceId();
            log.info("处理Ams湿度过高事件事件，发送消息: deviceId={}", deviceId);
            Printer printer = printerService.getByDeviceId(deviceId);
            printerNotificationTemplate.sendAmsHumidityTooHighWarning(
                    printer.getName(), event.getAmsNo());

        } catch (Exception e) {
            log.error("处理Ams湿度过高事件失败: deviceId={}, error={}",
                    event.getDeviceId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }

    @Override
    public boolean isAsync() {
        return true; // 异步处理，不阻塞主流程
    }
}