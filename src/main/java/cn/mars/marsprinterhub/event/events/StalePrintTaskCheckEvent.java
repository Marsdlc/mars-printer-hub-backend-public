package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 旧打印任务兜底检查事件
 * 当检测到新任务开始时，异步检查当前设备上遗留的旧 PRINTING 记录是否需要补收尾
 */
@Getter
public class StalePrintTaskCheckEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    public StalePrintTaskCheckEvent(String deviceId) {
        super("STALE_PRINT_TASK_CHECK", "AutoSpoolProcessor");
        this.deviceId = deviceId;
    }
}
