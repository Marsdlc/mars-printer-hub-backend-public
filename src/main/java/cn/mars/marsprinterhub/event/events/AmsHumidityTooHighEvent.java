package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * AMS湿度过高
 * 当检测到AMS湿度过高时发布此事件
 */
@Getter
public class AmsHumidityTooHighEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    /**
     * AMS编号
     */
    private final Integer amsNo;

    public AmsHumidityTooHighEvent(String deviceId, Integer amsNo) {
        super("AMS_HUMIDITY_TOO_HIGH", "DeviceStatusCacheService");
        this.deviceId = deviceId;
        this.amsNo = amsNo;
    }
}