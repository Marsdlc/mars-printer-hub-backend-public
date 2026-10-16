package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.AmsSlotChangeEmptyEvent;
import cn.mars.marsprinterhub.event.events.SseNotificationEvent;
import cn.mars.marsprinterhub.model.entity.Ams;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.notification.template.PrinterNotificationTemplate;
import cn.mars.marsprinterhub.service.AmsService;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;

/**
 * AMS 槽位变为空处理器
 * 当检测到 AMS 槽位变为空时进行发送消息提醒
 */
@Slf4j
@Component
public class AmsSlotChangeEmptyEventHandler implements EventHandler<AmsSlotChangeEmptyEvent> {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private PrinterService printerService;

    @Resource
    private AmsService amsService;

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
        eventBus.register(AmsSlotChangeEmptyEvent.class, this);
        log.info("AMS 槽位变为空处理器");
    }

    @Override
    public void handle(AmsSlotChangeEmptyEvent event) {
        if (!isIntranet) {
            return;
        }
        try {
            String deviceId = event.getDeviceId();
            Integer amsNo = event.getAmsNo();
            Integer slotNo = event.getSlotNo();
            Printer printer = printerService.getByDeviceId(deviceId);
            Ams ams = amsService.getAmsByPrinterIdAndAmsNo(printer.getId(), amsNo);
            if (ams == null) {
                log.warn("未找到AMS设备: deviceId={}, amsNo={}", deviceId, amsNo);
                return;
            }

            // 发送通知
            printerNotificationTemplate.sendAmsSlotChangeEmptyWarning(
                    printer.getName(), amsNo, slotNo
            );

            // 发送 SSE 通知
            Object userId = redisUtils.get(DEVICE_USER_PREFIX + deviceId);
            if (userId != null) {
                SseNotificationEvent sseNotificationEvent = new SseNotificationEvent(
                        Long.parseLong(userId.toString()), deviceId);
                eventPublisher.publish(sseNotificationEvent);
            }

        } catch (Exception e) {
            log.error("处理AMS槽位变为空失败: deviceId={}, amsNo={}, slotNo={}, error={}",
                    event.getDeviceId(), event.getAmsNo(), event.getSlotNo(), e.getMessage(), e);
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
