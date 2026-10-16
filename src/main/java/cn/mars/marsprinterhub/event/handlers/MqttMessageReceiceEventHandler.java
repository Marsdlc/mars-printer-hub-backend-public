package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.MqttMessageReceivedEvent;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MQTT消息事件处理器
 * 处理MQTT消息接收事件，更新设备状态缓存
 */
@Slf4j
@Component
public class MqttMessageReceiceEventHandler implements EventHandler<MqttMessageReceivedEvent> {

    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;

    @Resource
    private EventBus eventBus;


    @PostConstruct
    public void init() {
        eventBus.register(MqttMessageReceivedEvent.class, this);
        log.info("设备状态事件处理器已注册");
    }

    @Override
    public void handle(MqttMessageReceivedEvent event) {
        try {
            String deviceId = event.getDeviceId();

            // 保存新状态
            deviceStatusCacheService.saveDeviceStatusOnly(deviceId, event.getMessage());
        } catch (Exception e) {
            log.error("处理设备状态事件失败: deviceId={}, error={}",
                    event.getDeviceId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级，优先处理状态更新
    }

    @Override
    public boolean isAsync() {
        return false; // 同步处理，确保状态更新的时序性
    }
}