package cn.mars.marsprinterhub.task;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.DeviceConfigItem;
import cn.mars.marsprinterhub.notification.template.PrinterNotificationTemplate;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.SysConfigService;
import cn.mars.marsprinterhub.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * MQTT 设备定时重连任务
 * 定期检查并重连已断开的 MQTT 设备
 *
 * @author mars
 */
@Slf4j
@Component
@EnableScheduling
public class MqttReconnectTask {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private SysConfigService sysConfigService;
    @Resource
    private BambuMqttService bambuMqttService;
    @Resource
    private PrinterService printerService;
    @Resource
    private UserService userService;
    @Resource
    private PrinterNotificationTemplate notificationTemplate;

    /**
     * 执行重连任务
     */
    @Scheduled(cron = "0 0,30 * * * ?")
    public void executeReconnectTask() {
        // 检查是否启用
        if (!sysConfigService.isDeviceConfigEnabled(DeviceConfigItem.MQTT_AUTO_RECONNECT)) {
            log.debug("MQTT 重连任务未启用，跳过执行");
            return;
        }

        try {
            // 获取所有有效用户，非内网部署时仅 VIP 用户参与自动重连
            Date now = new Date();
            Date oneMonthAgo = new Date(System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(30));
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.isNotNull(User::getBambuToken)
                    .ge(User::getLastLoginTime, oneMonthAgo)
                    .ge(User::getBambuTokenExpireTime, now);
            if (!isIntranet) {
                queryWrapper.ge(User::getVipExpireTime, now);
            }
            List<User> userList = userService.list(queryWrapper);

            // 用于全局统计这次定时任务的整体重连情况（移到循环外）
            int totalReconnectedCount = 0;
            int totalFailedCount = 0;

            for (User user : userList) {
                if (StrUtil.isBlank(user.getBambuToken()) ||
                        StrUtil.isBlank(user.getBambuUsername()) ||
                        user.getBambuTokenExpireTime() == null ||
                        user.getBambuTokenExpireTime().before(new Date())) {
                    log.warn("用户 {} 缺少拓竹信息或已过期，跳过连接", user.getUsername());
                    continue;
                }

                // 获取用户的所有打印机
                List<Printer> printers = printerService.getListByUserId(user.getId());
                if (printers == null || printers.isEmpty()) {
                    log.debug("用户 {} 没有找到打印机设备，跳过重连", user.getUsername());
                    continue; // 修复：必须使用 continue，不能用 return
                }
                for (Printer printer : printers) {
                    String deviceId = printer.getDeviceId();
                    if (deviceId == null || deviceId.isBlank()) {
                        continue;
                    }

                    // 检查是否已连接
                    if (bambuMqttService.isConnected(deviceId)) {
                        log.debug("设备 {} 已连接，跳过", deviceId);
                        continue;
                    }

                    // 尝试重连
                    log.info("尝试重连设备: {}", deviceId);
                    try {
                        boolean connected = bambuMqttService.doAutoConnect(deviceId, user, printers);

                        // 验证连接是否成功
                        if (connected) {
                            totalReconnectedCount++;
                            log.info("设备 {} 重连成功", deviceId);
                        } else {
                            log.debug("设备 {} 未入选自动重连或重连后状态未建立", deviceId);
                        }
                    } catch (Exception e) {
                        totalFailedCount++;
                        log.error("设备 {} 重连失败: {}", deviceId, e.getMessage());
                    }
                }
            }
            if (totalReconnectedCount > 0 || totalFailedCount > 0) {
                log.info("MQTT 重连任务完成，成功: {}, 失败: {}", totalReconnectedCount, totalFailedCount);
            }

            // 循环结束后统一发送一次系统通知 (如果业务要求每个用户单独发，需要把 userId 传进 notificationTemplate 并放回循环内)
            if (totalReconnectedCount > 0 || totalFailedCount > 0) {
                notificationTemplate.sendMqttReconnectNotification(totalReconnectedCount, totalFailedCount);
            }

        } catch (Exception e) {
            log.error("MQTT 重连任务执行异常", e);
        }
    }
}
