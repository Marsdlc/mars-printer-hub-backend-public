package cn.mars.marsprinterhub.bambu.mqtt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;

@Slf4j
@Component
@ConditionalOnProperty(name = "bambu.mqtt.init-all", havingValue = "true")
public class BambuMqttInitializer {

    @Resource
    private PrinterService printerService;
    @Resource
    private UserService userService;
    @Resource
    private BambuMqttService mqttService;
    @Resource
    private RedisUtils redisUtils;

    // 调度器：负责延迟触发任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    // 连接池：控制最大同时连接设备数
    private final ExecutorService connectPool = Executors.newFixedThreadPool(10);

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Date now = new Date();
        Date oneMonthAgo = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));

        // 获取所有有效用户
        List<User> userList = userService.list(
                new LambdaQueryWrapper<User>()
                        .isNotNull(User::getBambuToken)
                        .ge(User::getLastLoginTime, oneMonthAgo)
                        .ge(User::getBambuTokenExpireTime, now)
        );

        AtomicInteger totalDevices = new AtomicInteger();
        AtomicInteger connectedDevices = new AtomicInteger();

        for (User user : userList) {
            if (StrUtil.isBlank(user.getBambuToken()) ||
                    StrUtil.isBlank(user.getBambuUsername()) ||
                    user.getBambuTokenExpireTime() == null ||
                    user.getBambuTokenExpireTime().before(now)) {
                log.warn("用户 {} 缺少拓竹信息，跳过连接", user.getUsername());
                continue;
            }

            List<Printer> printers = printerService.getListByUserId(user.getId());
            if (CollUtil.isEmpty(printers)) {
                log.warn("用户 {} 没有打印机，跳过连接", user.getUsername());
                continue;
            }

            totalDevices.addAndGet(printers.size());

            for (Printer printer : printers) {
                String deviceId = printer.getDeviceId();
                if (StrUtil.isBlank(deviceId)) {
                    log.warn("打印机 {} 缺少设备ID，跳过连接", printer.getName());
                    continue;
                }

                // 缓存 Redis
                redisUtils.set(DEVICE_USER_PREFIX + deviceId, user.getId());

                // 随机延迟 100ms ~ 5000ms
                long delay = ThreadLocalRandom.current().nextLong(100, 5000);

                scheduler.schedule(() -> {
                    connectPool.submit(() -> {
                        try {
                            boolean connected = mqttService.doAutoConnect(deviceId, user, printers);
                            if (connected) {
                                int finished = connectedDevices.incrementAndGet();
                                log.info("设备 {} 连接成功 ({}/{})", deviceId, finished, totalDevices.get());
                            } else {
                                log.debug("设备 {} 未自动连接或状态未建立", deviceId);
                            }
                        } catch (Exception e) {
                            log.error("设备 {} 连接失败: {}", deviceId, e.getMessage());
                        }
                    });
                }, delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    @PreDestroy
    public void shutdownExecutors() {
        scheduler.shutdownNow();
        connectPool.shutdownNow();
    }
}
