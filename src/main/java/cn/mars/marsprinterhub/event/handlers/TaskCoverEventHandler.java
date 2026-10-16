package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.TaskCoverEvent;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskDTO;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.processor.DeviceTaskProcessor;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;

/**
 * 任务封面事件处理器
 * 处理打印任务开始事件，保存任务封面
 */
@Slf4j
@Component
public class TaskCoverEventHandler implements EventHandler<TaskCoverEvent> {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private DeviceTaskProcessor deviceTaskProcessor;

    @Resource
    private BambuCloudService bambuCloudService;

    @Resource
    private UserService userService;

    @Resource
    private PrinterService printerService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(TaskCoverEvent.class, this);
        log.info("任务封面事件处理器已注册");
    }

    @Override
    public void handle(TaskCoverEvent event) {
        String deviceId = event.getDeviceId();

        try {
            // 外网环境才做 VIP 校验
            if (!isIntranet) {
                User vipUser = userService.getVipUserByDeviceId(deviceId);
                if (vipUser == null) {
                    return;
                }
            }

            // 获取任务封面 URL并保存
            String coverUrl = getTaskCoverUrl(deviceId);
            if (coverUrl != null && !coverUrl.isEmpty()) {
                deviceTaskProcessor.saveTaskCoverUrl(deviceId, coverUrl);
            } else {
                log.error("无法获取任务封面URL: deviceId={}", deviceId);
            }

        } catch (Exception e) {
            log.error("处理任务封面事件失败: deviceId={}, error={}",
                    deviceId, e.getMessage(), e);
        }
    }

    /**
     * 获取任务封面URL
     * 从原有的DeviceTaskHandle.saveLastTaskCoverUrl方法中提取的逻辑
     */
    private String getTaskCoverUrl(String deviceId) {
        try {
            String redisUserKey = DEVICE_USER_PREFIX + deviceId;

            Long userId = redisUtils.getLong(redisUserKey);
            if (userId == null) {
                printerService.createCache();
                userId = redisUtils.getLong(redisUserKey);
            }

            if (userId == null) {
                log.warn("无法获取用户ID: deviceId={}", deviceId);
                return null;
            }

            // 根据设备id获取拓竹token
            User user = userService.getById(userId);
            if (user == null || user.getBambuToken() == null ||
                    user.getBambuTokenExpireTime() == null ||
                    user.getBambuTokenExpireTime().before(new Date())) {
                log.warn("用户token无效或已过期: deviceId={}, userId={}", deviceId, userId);
                return null;
            }

            // 获取打印任务信息
            BambuTaskDTO latestTask = bambuCloudService.getLatestTaskForPrinter(user, deviceId, user.getBambuToken());
            if (latestTask == null) {
                log.warn("无法获取最新任务信息: deviceId={}", deviceId);
                return null;
            }

            return latestTask.getCover();

        } catch (Exception e) {
            log.error("获取任务封面URL失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }

    @Override
    public boolean isAsync() {
        return true; // 异步处理，不阻塞主流程
    }
}
