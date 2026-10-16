package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import cn.mars.marsprinterhub.model.enums.PrinterStatusEnum;
import lombok.Getter;

/**
 * 打印任务完成事件
 * 当打印任务完成（成功或失败）时发布此事件
 */
@Getter
public class PrintTaskCompletedEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    /**
     * 完成时的打印进度
     */
    private final Integer finalProgress;

    /**
     * 完成状态（FINISH/FAILED）
     */
    private final String completionStatus;

    /**
     * 是否成功完成
     */
    private final boolean isSuccess;

    public PrintTaskCompletedEvent(String deviceId, Integer finalProgress, String completionStatus) {
        super("PRINT_TASK_COMPLETED", "DeviceStatusCacheService");
        this.deviceId = deviceId;
        this.finalProgress = finalProgress;
        this.completionStatus = completionStatus;
        this.isSuccess = PrinterStatusEnum.FINISH.getValue().equals(completionStatus);
    }
}