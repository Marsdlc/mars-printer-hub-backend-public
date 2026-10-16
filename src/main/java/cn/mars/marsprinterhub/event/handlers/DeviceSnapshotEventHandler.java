package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.DeviceSnapshotRequestEvent;
import cn.mars.marsprinterhub.model.enums.DeviceConfigItem;
import cn.mars.marsprinterhub.processor.AutoImageProcessor;
import cn.mars.marsprinterhub.service.SysConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 快照事件处理器
 * 处理设备快照请求事件
 */
@Slf4j
@Component
public class DeviceSnapshotEventHandler implements EventHandler<DeviceSnapshotRequestEvent> {

    @Resource
    private AutoImageProcessor autoImageProcessor;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(DeviceSnapshotRequestEvent.class, this);
        log.info("快照事件处理器已注册");
    }

    @Override
    public void handle(DeviceSnapshotRequestEvent event) {
        // 检查是否启用自动快照
        if (!sysConfigService.isDeviceConfigEnabled(DeviceConfigItem.AUTO_SNAPSHOT)) {
            return;
        }
        try {
            String deviceId = event.getDeviceId();
            Integer progress = event.getProgress();

            log.info("处理设备快照请求: deviceId={}, progress={}, type={}",
                    deviceId, progress, event.getSnapshotType());
            // 执行快照保存
            autoImageProcessor.execute(deviceId, progress);
        } catch (Exception e) {
            log.error("处理快照事件失败: deviceId={}, error={}",
                    event.getDeviceId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 70; // 较低优先级
    }

    @Override
    public boolean isAsync() {
        return true; // 异步处理
    }
}