package cn.mars.marsprinterhub.event.handlers;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.PrinterUnbindEvent;
import cn.mars.marsprinterhub.model.entity.Ams;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.service.AmsService;
import cn.mars.marsprinterhub.service.AmsSlotsService;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;
import static cn.mars.marsprinterhub.constant.RedisConstants.USER_MQTT_AUTO_DEVICES_PREFIX;

/**
 * 解绑打印机事件处理器
 */
@Slf4j
@Component
public class PrinterUnbindEventHandler implements EventHandler<PrinterUnbindEvent> {

    @Resource
    private BambuMqttService bambuMqttService;

    @Resource
    private PrinterService printerService;

    @Resource
    private AmsService amsService;

    @Resource
    private AmsSlotsService amsSlotsService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(PrinterUnbindEvent.class, this);
        log.info("解绑打印机事件处理器已注册");
    }

    @Override
    public void handle(PrinterUnbindEvent event) {
        try {
            Long printerId = event.getPrinterId();
            // 查询打印机
            Printer printer = printerService.getById(printerId);
            if (printer == null) {
                return;
            }
            String deviceId = printer.getDeviceId();
            // 先断开 MQTT 连接，避免清理过程中仍有活跃设备连接
            bambuMqttService.disconnectPrinter(deviceId);
            // 删除打印机
            printerService.removeById(printerId);
            // 删除缓存
            redisUtils.delete(DEVICE_USER_PREFIX + deviceId);
            removeAutoConnectDevice(printer.getUserId(), deviceId);
            // 获取该打印机下的 AMS
            List<Ams> amsList = amsService.getListByPrinterId(printerId);
            if (CollUtil.isEmpty(amsList)) {
                return;
            }
            List<Long> amsIds = amsList.stream()
                    .map(Ams::getId)
                    .toList();
            // 删除 AMS
            amsService.removeByIds(amsIds);
            // 删除 AMS 槽位
            amsSlotsService.removeByAmsIds(amsIds);
        } catch (Exception e) {
            log.error("处理解绑打印机事件失败: printerId={}, error={}",
                    event.getPrinterId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }

    @Override
    public boolean isAsync() {
        return false; // 同步处理
    }

    private void removeAutoConnectDevice(Long userId, String deviceId) {
        if (userId == null || StrUtil.isBlank(deviceId)) {
            return;
        }
        String key = USER_MQTT_AUTO_DEVICES_PREFIX + userId;
        redisUtils.zRemove(key, deviceId);
    }
}
