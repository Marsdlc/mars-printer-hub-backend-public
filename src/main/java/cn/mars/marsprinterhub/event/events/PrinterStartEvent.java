package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 打印任务开始事件
 * 当检测到打印任务开始时发布此事件
 */
@Getter
public class PrinterStartEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    public PrinterStartEvent(String deviceId) {
        super("PRINT_STARTED", "");
        this.deviceId = deviceId;
    }
}