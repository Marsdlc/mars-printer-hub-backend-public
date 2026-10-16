package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 打印任务完成事件
 * 当检测到打印状态由 RUNNING 变为 FINISH 时发布此事件
 */
@Getter
public class PrinterFinishedEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    public PrinterFinishedEvent(String deviceId) {
        super("PRINT_FINISHED", "DeviceStatusCacheService");
        this.deviceId = deviceId;
    }
}
