package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.AmsSlotAutoUnbindEvent;
import cn.mars.marsprinterhub.event.events.SseNotificationEvent;
import cn.mars.marsprinterhub.model.entity.Ams;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.enums.DeviceConfigItem;
import cn.mars.marsprinterhub.notification.template.PrinterNotificationTemplate;
import cn.mars.marsprinterhub.service.AmsService;
import cn.mars.marsprinterhub.service.AmsSlotsService;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.SysConfigService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;

/**
 * AMS槽位自动解绑处理器
 * 当检测到AMS槽位变为空时进行解绑操作
 */
@Slf4j
@Component
public class AmsSlotAutoUnbindEventHandler implements EventHandler<AmsSlotAutoUnbindEvent> {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private PrinterService printerService;

    @Resource
    private AmsService amsService;

    @Resource
    private AmsSlotsService amsSlotsService;

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
        eventBus.register(AmsSlotAutoUnbindEvent.class, this);
        log.info("AMS 槽位自动解绑处理器已注册");
    }

    @Override
    public void handle(AmsSlotAutoUnbindEvent event) {
        if (!isIntranet) {
            return;
        }
        if (!sysConfigService.isDeviceConfigEnabled(DeviceConfigItem.AMS_SLOT_AUTO_UNBIND)) {
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
            // 解绑
            amsSlotsService.bindSlotSpool(ams.getId(), slotNo, 0L);

            // 发送通知
            printerNotificationTemplate.sendAmsSlotAutoUnbindWarning(
                    printer.getName(), amsNo, slotNo);

            // 发送 SSE 通知
            Object userId = redisUtils.get(DEVICE_USER_PREFIX + deviceId);
            if (userId != null) {
                SseNotificationEvent sseNotificationEvent = new SseNotificationEvent(
                        Long.parseLong(userId.toString()), deviceId);
                eventPublisher.publish(sseNotificationEvent);
            }

        } catch (Exception e) {
            log.error("处理AMS槽位自动解绑失败: deviceId={}, amsNo={}, slotNo={}, error={}",
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
