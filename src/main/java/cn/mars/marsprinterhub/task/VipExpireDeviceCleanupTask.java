package cn.mars.marsprinterhub.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.PrinterStatusEnum;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCache;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static cn.mars.marsprinterhub.constant.RedisConstants.USER_MQTT_AUTO_DEVICES_PREFIX;

/**
 * VIP 过期用户设备连接清理定时任务
 * 每天凌晨1点执行，将昨天VIP过期的用户的设备连接数缩减为一台
 * 优先保留正在打印中的设备
 *
 * @author mars
 */
@Slf4j
@Component
public class VipExpireDeviceCleanupTask {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private UserService userService;

    @Resource
    private PrinterService printerService;

    @Resource
    private BambuMqttService bambuMqttService;

    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void execute() {
        // 内网部署不执行
        if (isIntranet) {
            log.debug("当前为内网部署，跳过VIP过期设备清理任务");
            return;
        }

        log.info("开始执行 VIP 过期设备清理任务");

        try {
            // 计算昨天的时间范围：00:00:00 ~ 23:59:59
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Date startOfYesterday = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endOfYesterday = Date.from(yesterday.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

            // 查询昨天 VIP 过期的用户
            List<User> expiredUsers = userService.list(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getVipExpireTime, startOfYesterday)
                            .le(User::getVipExpireTime, endOfYesterday)
            );

            if (CollUtil.isEmpty(expiredUsers)) {
                log.info("没有昨天VIP过期的用户，任务结束");
                return;
            }

            log.info("找到 {} 个昨天VIP过期的用户", expiredUsers.size());

            for (User user : expiredUsers) {
                try {
                    processExpiredUser(user);
                } catch (Exception e) {
                    log.error("处理用户 {} 的设备清理时发生异常: {}", user.getUsername(), e.getMessage(), e);
                }
            }

            log.info("VIP 过期设备清理任务执行完毕");

        } catch (Exception e) {
            log.error("VIP 过期设备清理任务执行异常", e);
        }
    }

    /**
     * 处理单个过期 VIP 用户的设备连接
     */
    private void processExpiredUser(User user) {
        List<Printer> printers = printerService.getListByUserId(user.getId());

        if (CollUtil.isEmpty(printers) || printers.size() <= 1) {
            log.debug("用户 {} 的设备数量不超过1台，无需清理", user.getUsername());
            return;
        }

        log.info("用户 {} VIP已过期，当前连接 {} 台设备，开始清理", user.getUsername(), printers.size());

        // 过滤出有有效 deviceId的打印机
        List<Printer> validPrinters = printers.stream()
                .filter(p -> StrUtil.isNotBlank(p.getDeviceId()))
                .toList();

        if (validPrinters.isEmpty()) {
            return;
        }

        // 筛选出正在打印中的设备
        List<Printer> printingDevices = new ArrayList<>();
        for (Printer printer : validPrinters) {
            DeviceStatusCache statusCache = deviceStatusCacheService.getDeviceStatus(printer.getDeviceId());
            if (statusCache != null && PrinterStatusEnum.RUNNING.getValue().equals(statusCache.getPrintStatus())) {
                printingDevices.add(printer);
            }
        }

        // 选择保留的设备
        Printer keepPrinter;
        if (printingDevices.size() == 1) {
            // 恰好有1台在打印，保留该台
            keepPrinter = printingDevices.getFirst();
        } else {
            // 多台在打印或无打印设备，随机选一台
            List<Printer> candidates = printingDevices.isEmpty() ? validPrinters : printingDevices;
            keepPrinter = candidates.get(new Random().nextInt(candidates.size()));
        }

        log.info("用户 {} 保留设备: {} ({})", user.getUsername(), keepPrinter.getName(), keepPrinter.getDeviceId());

        // 断开其他设备的连接
        for (Printer printer : validPrinters) {
            if (!printer.getDeviceId().equals(keepPrinter.getDeviceId())) {
                try {
                    bambuMqttService.disconnectPrinter(printer.getDeviceId());
                    log.info("已断开用户 {} 的设备: {} ({})", user.getUsername(), printer.getName(), printer.getDeviceId());
                } catch (Exception e) {
                    log.error("断开设备 {} 连接失败: {}", printer.getDeviceId(), e.getMessage());
                }
            }
        }

        // 设置自动连接设备列表
        String key = USER_MQTT_AUTO_DEVICES_PREFIX + user.getId();
        redisUtils.zSet(key, keepPrinter.getDeviceId());
        redisUtils.zRemoveRange(key, 0, -2);
        log.info("已设置用户 {} 的自动连接设备为: {}", user.getUsername(), keepPrinter.getDeviceId());
    }
}
