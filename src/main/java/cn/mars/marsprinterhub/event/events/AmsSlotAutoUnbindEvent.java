package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * AMS槽位自动解绑
 * 当检测到AMS槽位变为空时发布此事件
 */
@Getter
public class AmsSlotAutoUnbindEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    /**
     * AMS编号
     */
    private final Integer amsNo;

    /**
     * 槽位编号
     */
    private final Integer slotNo;

    public AmsSlotAutoUnbindEvent(String deviceId, Integer amsNo, Integer slotNo) {
        super("AMS_SLOT_AUTO_UNBIND", "DeviceStatusCacheService");
        this.deviceId = deviceId;
        this.amsNo = amsNo;
        this.slotNo = slotNo;
    }
}