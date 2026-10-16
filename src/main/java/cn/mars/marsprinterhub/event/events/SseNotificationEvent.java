package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 设备 SSE 通知事件
 */
@Getter
public class SseNotificationEvent extends Event {

    private final Long userId;

    private final String deviceId;

    public SseNotificationEvent(Long userId, String deviceId) {
        super("SSE_NOTIFICATION", "DeviceStatusCacheService");
        this.userId = userId;
        this.deviceId = deviceId;
    }
}
