package cn.mars.marsprinterhub.event.handlers;

import cn.hutool.core.collection.CollUtil;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.UserBambuRegionChangedEvent;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户拓竹区域变更事件处理器
 */
@Slf4j
@Component
public class UserBambuRegionChangedEventHandler implements EventHandler<UserBambuRegionChangedEvent> {

    @Resource
    private EventBus eventBus;

    @Resource
    private BambuCloudService bambuCloudService;

    @Resource
    private RedisUtils redisUtils;

    @PostConstruct
    public void init() {
        eventBus.register(UserBambuRegionChangedEvent.class, this);
        log.info("用户拓竹区域变更事件处理器已注册");
    }

    @Override
    public void handle(UserBambuRegionChangedEvent event) {
        Long userId = event.getUserId();
        if (userId == null) {
            return;
        }

        try {
            bambuCloudService.cleanupCurrentUserBambuBinding("用户切换拓竹区域");
            redisUtils.delete("user:" + userId);
        } catch (Exception e) {
            log.error("处理用户拓竹区域变更事件失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
