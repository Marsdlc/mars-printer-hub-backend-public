package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 设备快照请求事件
 * 当需要保存设备快照时发布此事件
 */
@Getter
public class DeviceSnapshotRequestEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    /**
     * 打印进度
     */
    private final Integer progress;

    /**
     * 快照类型
     */
    private final SnapshotType snapshotType;

    public DeviceSnapshotRequestEvent(String deviceId, Integer progress, SnapshotType snapshotType) {
        super("DEVICE_SNAPSHOT_REQUEST", "BambuMqttMessageProcessor");
        this.deviceId = deviceId;
        this.progress = progress;
        this.snapshotType = snapshotType;
    }

    /**
     * 快照类型枚举
     */
    public enum SnapshotType {
        PROGRESS_MILESTONE,  // 进度里程碑快照
        TASK_COVER,         // 任务封面快照
        ERROR_CAPTURE       // 错误捕获快照
    }
}