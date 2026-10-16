package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.bambu.dto.BambuDeviceDTO;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.PrinterRefreshEvent;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCache;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static cn.mars.marsprinterhub.exception.ErrorCode.NOT_FOUND_ERROR;

/**
 * 刷新打印机事件处理器
 */
@Slf4j
@Component
public class PrinterRefreshEventHandler implements EventHandler<PrinterRefreshEvent> {

    @Resource
    private PrinterService printerService;

    @Resource
    private BambuCloudService bambuCloudService;

    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;

    @Resource
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(PrinterRefreshEvent.class, this);
        log.info("刷新打印机事件处理器已注册");
    }

    @Override
    public void handle(PrinterRefreshEvent event) {
        try {
            Long printerId = event.getPrinterId();
            // 查询打印机
            Printer printer = printerService.getById(printerId);
            String deviceId = printer.getDeviceId();
            // 获取访问码
            BambuDeviceDTO bambuDeviceDTO = bambuCloudService.getDeviceById(deviceId);
            ThrowUtils.throwIf(bambuDeviceDTO == null, NOT_FOUND_ERROR);
            // 获取IP地址
            DeviceStatusCache deviceStatus = deviceStatusCacheService.getDeviceStatus(deviceId);
            String ip = deviceStatus.getIp();
            // 更新打印机信息
            if (!Objects.equals(printer.getIpAddress(), ip) || !Objects.equals(printer.getAccessCode(), bambuDeviceDTO.getDeviceAccessCode())) {
                Printer update = new Printer();
                update.setId(printerId);
                update.setIpAddress(ip);
                update.setAccessCode(bambuDeviceDTO.getDeviceAccessCode());
                printerService.updateById(update);
            }
        } catch (Exception e) {
            log.error("处理刷新打印机事件失败: printerId={}, error={}",
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
}