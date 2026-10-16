package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 打印任务暂停事件
 * 当检测到打印状态由 RUNNING 变为 PAUSE 时发布此事件
 */
@Getter
public class PrinterPausedEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    public PrinterPausedEvent(String deviceId) {
        super("PRINT_PAUSED", "DeviceStatusCacheService");
        this.deviceId = deviceId;
    }
}
