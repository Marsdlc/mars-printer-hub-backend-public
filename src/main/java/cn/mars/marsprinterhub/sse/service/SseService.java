package cn.mars.marsprinterhub.sse.service;

import cn.mars.marsprinterhub.sse.dto.DeviceUpdateNotification;
import cn.mars.marsprinterhub.sse.manager.SseConnectionManager;
import cn.mars.marsprinterhub.sse.message.SseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 服务类
 * 保持通知-拉取模式，只发送轻量设备更新信号。
 */
@Service
@RequiredArgsConstructor
public class SseService {

    private final SseConnectionManager connectionManager;

    public SseEmitter connect(Long userId) {
        return connectionManager.addConnection(userId);
    }

    public void notifyDeviceUpdate(Long userId, String deviceId) {
        if (userId == null) {
            return;
        }

        DeviceUpdateNotification notification = DeviceUpdateNotification.builder()
                .deviceId(deviceId)
                .timestamp(System.currentTimeMillis())
                .build();

        SseMessage<DeviceUpdateNotification> message =
                SseMessage.createNotificationMessage("device_update", notification);

        connectionManager.sendToUser(userId, message);
    }
}
