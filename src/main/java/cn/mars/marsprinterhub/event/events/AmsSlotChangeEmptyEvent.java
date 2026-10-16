package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * AMS槽位变为空事件
 * 当检测到AMS槽位变为空时发布此事件
 */
@Getter
public class AmsSlotChangeEmptyEvent extends Event {

    /**
     * 设备 ID
     */
    private final String deviceId;

    /**
     * AMS 编号
     */
    private final Integer amsNo;

    /**
     * 槽位编号
     */
    private final Integer slotNo;

    public AmsSlotChangeEmptyEvent(String deviceId, Integer amsNo, Integer slotNo) {
        super("AMS_SLOT_CHANGE_EMPTY", "DeviceStatusCacheService");
        this.deviceId = deviceId;
        this.amsNo = amsNo;
        this.slotNo = slotNo;
    }
}