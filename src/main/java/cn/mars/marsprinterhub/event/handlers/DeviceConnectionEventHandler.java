package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.DeviceConnectionEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 设备连接事件处理器
 * 处理设备连接状态变化事件
 */
@Slf4j
@Component
public class DeviceConnectionEventHandler implements EventHandler<DeviceConnectionEvent> {

    @Resource
    @Lazy
    private BambuMqttService bambuMqttService;

    @Resource
    private cn.mars.marsprinterhub.event.core.EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(DeviceConnectionEvent.class, this);
        log.info("设备连接事件处理器已注册");
    }

    @Override
    public void handle(DeviceConnectionEvent event) {
        try {
            String deviceId = event.getDeviceId();
            DeviceConnectionEvent.ConnectionStatus status = event.getStatus();

            log.info("处理设备连接事件: deviceId={}, status={}, type={}",
                    deviceId, status, event.getConnectionType());

            switch (status) {
                case DISCONNECTED:
                    // 断开设备连接
                    bambuMqttService.disconnectPrinter(deviceId);
                    log.info("设备连接已断开: deviceId={}", deviceId);
                    break;

                case CONNECTING:
                case CONNECTION_FAILED:
                case CONNECTION_LOST:
                    log.error("设备连接异常: deviceId={}, status={}, error={}",
                            deviceId, status, event.getErrorMessage());
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            log.error("处理设备连接事件失败: deviceId={}, error={}",
                    event.getDeviceId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 20; // 较高优先级
    }

    @Override
    public boolean isAsync() {
        return true; // 异步处理
    }
}