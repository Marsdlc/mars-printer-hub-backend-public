package cn.mars.marsprinterhub.bambu.mqtt;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.config.BambuRegionConfig;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.SseNotificationEvent;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import cn.mars.marsprinterhub.model.enums.MqttConnectionTypeEnum;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.utils.FilamentUtils;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_LAST_STATUS_PREFIX;
import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_STATUS_PREFIX;
import static cn.mars.marsprinterhub.constant.RedisConstants.USER_MQTT_AUTO_DEVICES_PREFIX;
import static cn.mars.marsprinterhub.exception.ErrorCode.AUTHENTICATION_FAILED;
import static cn.mars.marsprinterhub.exception.ErrorCode.SEND_MESSAGE_FAILED;

/**
 * 拓竹MQTT服务
 * 提供MQTT连接管理和打印控制的高级业务逻辑
 */
@Slf4j
@Service
public class BambuMqttService {

    private static final int VIP_MAX_MQTT_CONNECTIONS = 10;
    /**
     * 用户连接限制锁，避免同一用户并发连接时突破数量上限
     */
    private final Map<Long, ReentrantLock> userConnectionLimitLocks = new ConcurrentHashMap<>();
    @Value("${bambu.intranet}")
    private boolean isIntranet;
    @Resource
    private BambuMqttClientManager clientManager;
    @Resource
    private UserService userService;
    @Resource
    private PrinterService printerService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private FilamentUtils filamentUtils;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private EventPublisher eventPublisher;
    @Resource
    private BambuRegionConfig bambuRegionConfig;

    /**
     * 使用用户信息连接打印机
     *
     * @param deviceId 设备 ID
     * @param user     用户信息
     */
    public boolean doConnect(String deviceId, User user) {
        log.info("开始连接打印机，设备id : {}", deviceId);

        validateConnectUser(user);

        if (isIntranet) {
            return connectClient(deviceId, user);
        }

        ReentrantLock lock = getUserConnectionLimitLock(user.getId());
        lock.lock();
        try {
            // 手动连接：非内网先看当前用户已连接设备，未达到上限时不做任何顶替，直接连接。
            Set<String> connectedDeviceIds = clientManager.getUserConnectedDevices(user.getId());
            int limit = getMqttConnectionLimit(user);
            if (connectedDeviceIds.contains(deviceId) || connectedDeviceIds.size() < limit) {
                boolean connected = connectClient(deviceId, user);
                if (connected) {
                    saveAutoConnectDeviceIds(user.getId(), resolveManualConnectDeviceIds(deviceId, user));
                }
                return connected;
            }

            // 已达到上限时，当前设备插入默认连接列表最前方，再断开被挤出列表的已连接设备。
            List<String> selectedDeviceIds = resolveManualConnectDeviceIds(deviceId, user);
            disconnectUnselectedDevices(connectedDeviceIds, selectedDeviceIds);
            boolean connected = connectClient(deviceId, user);
            if (connected) {
                saveAutoConnectDeviceIds(user.getId(), selectedDeviceIds);
            }
            return connected;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 自动连接设备，按默认连接列表选择设备，不调整手动连接优先级。
     *
     * @param deviceId 设备 ID
     * @param user     用户信息
     * @param printers 本轮自动连接候选打印机
     */
    public boolean doAutoConnect(String deviceId, User user, List<Printer> printers) {
        log.info("开始自动连接打印机，设备id : {}", deviceId);

        validateConnectUser(user);

        if (isIntranet) {
            return connectClient(deviceId, user);
        }

        ReentrantLock lock = getUserConnectionLimitLock(user.getId());
        lock.lock();
        try {
            // 自动连接：根据默认连接列表计算本轮可连接设备；不在列表内的设备直接跳过。
            List<String> autoConnectDeviceIds = resolveAutoConnectDeviceIds(user, printers);
            if (!autoConnectDeviceIds.contains(deviceId)) {
                log.info("设备 {} 不在本轮自动连接设备列表中，跳过连接", deviceId);
                return false;
            }

            int limit = getMqttConnectionLimit(user);
            if (!clientManager.isConnected(deviceId) && clientManager.getUserClientCount(user.getId()) >= limit) {
                log.info("用户 {} MQTT 自动连接数已达上限 {}，跳过设备 {}", user.getId(), limit, deviceId);
                return false;
            }
            return connectClient(deviceId, user);
        } finally {
            lock.unlock();
        }
    }

    private void validateConnectUser(User user) {
        if (user == null || user.getId() == null || user.getBambuUserId() == null || user.getBambuToken() == null) {
            throw new BusinessException(AUTHENTICATION_FAILED);
        }
    }

    private boolean connectClient(String deviceId, User user) {
        if (isIntranet && connectByIntranet(deviceId, user)) {
            log.info("成功连接到局域网 mqtt，设备: {}", deviceId);
            return true;
        }

        return connectByCloud(deviceId, user);
    }

    private boolean connectByCloud(String deviceId, User user) {
        String username = "u_" + user.getBambuUserId();
        BambuMqttConnectionInfo cloudConnection = BambuMqttConnectionInfo.createCloudConnection(
                deviceId,
                username,
                user.getBambuToken(),
                bambuRegionConfig.getCloudMqttHost(resolveBambuRegion(user))
        );
        try {
            clientManager.createClient(user.getId(), cloudConnection);
            // 连接成功通知前端更新页面
            SseNotificationEvent event = new SseNotificationEvent(user.getId(), deviceId);
            eventPublisher.publish(event);
            log.info("成功连接到云端 mqtt， 设备: {}", deviceId);
            return true;
        } catch (Exception e) {
            log.error("连接云端 mqtt 失败，设备: {}，error: {}", deviceId, e.getMessage());
            return false;
        }
    }

    private BambuRegionEnum resolveBambuRegion(User user) {
        if (user == null) {
            return BambuRegionEnum.CHINA;
        }
        BambuRegionEnum regionEnum = BambuRegionEnum.fromValue(user.getBambuRegion());
        return regionEnum == null ? BambuRegionEnum.CHINA : regionEnum;
    }

    private List<String> resolveManualConnectDeviceIds(String deviceId, User user) {
        List<Printer> printers = printerService.getListByUserId(user.getId());
        return resolveConnectDeviceIds(user, printers, deviceId);
    }

    /**
     * 断开当前已连接但不在最终默认连接列表里的设备，用于手动连接达到上限后的顶替。
     */
    private void disconnectUnselectedDevices(Set<String> connectedDeviceIds, List<String> selectedDeviceIds) {
        for (String connectedDeviceId : connectedDeviceIds) {
            if (!selectedDeviceIds.contains(connectedDeviceId)) {
                disconnectPrinter(connectedDeviceId);
            }
        }
    }

    private ReentrantLock getUserConnectionLimitLock(Long userId) {
        return userConnectionLimitLocks.computeIfAbsent(userId, key -> new ReentrantLock());
    }

    /**
     * 自动连接列表：优先使用用户保存的默认连接列表，不足上限时从本轮候选打印机中补齐，并回写补齐后的列表。
     */
    private List<String> resolveAutoConnectDeviceIds(User user, List<Printer> printers) {
        List<String> deviceIds = resolveConnectDeviceIds(user, printers, null);
        saveAutoConnectDeviceIds(user.getId(), deviceIds);
        return deviceIds;
    }

    /**
     * 解析最终默认连接列表。
     *
     * @param user 用户信息，用于获取连接上限和已保存的默认连接列表
     * @param printers 候选打印机列表，用于在默认连接列表不足上限时补齐
     * @param firstDeviceId 手动连接设备 id，存在时插入列表最前方；自动连接传 null
     * @return 按连接优先级排序，并按连接上限截断后的设备 id 列表
     */
    private List<String> resolveConnectDeviceIds(User user, List<Printer> printers, String firstDeviceId) {
        Map<String, Printer> printerMap = buildPrinterMap(printers);
        int limit = getMqttConnectionLimit(user);
        if (printerMap.isEmpty() && StrUtil.isBlank(firstDeviceId)) {
            return Collections.emptyList();
        }

        List<String> selectedDeviceIds = new ArrayList<>();
        addSelectedDeviceId(selectedDeviceIds, firstDeviceId, limit);

        List<String> autoConnectDeviceIds = getAutoConnectDeviceIds(user.getId());
        for (String deviceId : autoConnectDeviceIds) {
            if (printerMap.containsKey(deviceId)) {
                addSelectedDeviceId(selectedDeviceIds, deviceId, limit);
            }
        }

        for (String deviceId : printerMap.keySet()) {
            addSelectedDeviceId(selectedDeviceIds, deviceId, limit);
        }

        return selectedDeviceIds;
    }

    private void addSelectedDeviceId(List<String> selectedDeviceIds, String deviceId, int limit) {
        if (selectedDeviceIds.size() >= limit || StrUtil.isBlank(deviceId) || selectedDeviceIds.contains(deviceId)) {
            return;
        }
        selectedDeviceIds.add(deviceId);
    }

    private int getMqttConnectionLimit(User user) {
        return user.isVip() ? VIP_MAX_MQTT_CONNECTIONS : 1;
    }

    private Map<String, Printer> buildPrinterMap(List<Printer> printers) {
        Map<String, Printer> printerMap = new LinkedHashMap<>();
        if (printers == null) {
            return printerMap;
        }
        for (Printer printer : printers) {
            if (printer != null && StrUtil.isNotBlank(printer.getDeviceId())) {
                printerMap.putIfAbsent(printer.getDeviceId(), printer);
            }
        }
        return printerMap;
    }

    private List<String> getAutoConnectDeviceIds(Long userId) {
        return redisUtils.zReverseRange(USER_MQTT_AUTO_DEVICES_PREFIX + userId, 0, -1)
                .stream()
                .map(String::valueOf)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private void saveAutoConnectDeviceIds(Long userId, List<String> deviceIds) {
        String key = USER_MQTT_AUTO_DEVICES_PREFIX + userId;
        long score = System.currentTimeMillis();
        for (String deviceId : deviceIds) {
            if (StrUtil.isBlank(deviceId)) {
                continue;
            }
            redisUtils.zSet(key, deviceId, score);
            score--;
        }
        // zReverseRange 读取高分在前；这里删除低分尾部，只保留本次计算出的默认连接列表。
        redisUtils.zRemoveRange(key, 0, -deviceIds.size() - 1L);
    }

    private boolean connectByIntranet(String deviceId, User user) {
        // 从数据库中获取打印机信息
        Printer printer = printerService.getByDeviceId(deviceId);

        // 优先尝试局域网连接
        if (printer != null && StrUtil.isNotBlank(printer.getIpAddress()) && StrUtil.isNotBlank(printer.getAccessCode())) {
            try {
                BambuMqttConnectionInfo localConnection = BambuMqttConnectionInfo.createLocalConnection(
                        deviceId,
                        printer.getIpAddress(),
                        printer.getAccessCode()
                );
                clientManager.createClient(user.getId(), localConnection);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 断开连接
     */
    public void disconnectPrinter(String deviceId) {
        try {
            log.info("断开设备连接: deviceId={}", deviceId);
            clientManager.disconnectClient(deviceId);
            log.info("设备连接断开成功: deviceId={}", deviceId);
        } catch (Exception e) {
            log.error("断开连接失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 断开所有连接
     */
    public void disconnectAllPrinters() {
        try {
            log.info("断开所有设备连接");
            Set<String> connectedDevices = getConnectedDevices();
            for (String deviceId : connectedDevices) {
                try {
                    disconnectPrinter(deviceId);
                } catch (Exception e) {
                    log.error("断开设备连接失败: deviceId={}, error={}", deviceId, e.getMessage());
                }
            }
            log.info("所有设备连接断开完成");
        } catch (Exception e) {
            log.error("断开所有连接失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected(String deviceId) {
        return clientManager.isConnected(deviceId);
    }

    /**
     * 获取连接状态
     */
    public MqttConnectionTypeEnum getConnectionStatus(String deviceId) {
        return clientManager.getConnectionType(deviceId);
    }

    /**
     * 获取已连接设备列表
     */
    public Set<String> getConnectedDevices() {
        return clientManager.getConnectedDevices();
    }

    /**
     * 获取指定用户当前已连接的 MQTT 客户端数量
     */
    public int getUserClientCount(Long userId) {
        return clientManager.getUserClientCount(userId);
    }

    /**
     * 获取指定用户当前已连接的设备 ID
     */
    public Set<String> getUserConnectedDevices(Long userId) {
        return clientManager.getUserConnectedDevices(userId);
    }

    /**
     * 应用关闭时优雅断开 MQTT
     */
    @PreDestroy
    public void onShutdown() {
        try {
            log.info("应用关闭：正在断开所有 MQTT 连接...");
            disconnectAllPrinters();
        } catch (Exception e) {
            log.warn("应用关闭时断开连接失败：{}", e.getMessage());
        }
    }

    /**
     * 强制刷新
     */
    public void refresh(String deviceId) {
        // 断开链接
        disconnectPrinter(deviceId);
        // 删除 redis 缓存
        redisUtils.delete(DEVICE_STATUS_PREFIX + deviceId);
        redisUtils.delete(DEVICE_LAST_STATUS_PREFIX + deviceId);
        // 重新建立连接
        doConnect(deviceId, userService.getCurrentUser());
        // 延迟2秒
        waitStabilization(2000);
        // 请求全量数据
        pushAllStatus(deviceId);
    }

    /**
     * 发送命令
     */
    public void sendCommand(String deviceId, String command) {
        try {
            clientManager.publishMessage(deviceId, command);
        } catch (Exception e) {
            log.error("发送命令失败: deviceId={}, command={}, error={}", deviceId, command, e.getMessage(), e);
            throw new BusinessException(SEND_MESSAGE_FAILED);
        }
    }

    /**
     * 推送所有状态信息
     * 发送pushall命令获取设备的完整状态信息
     */
    public void pushAllStatus(String deviceId) {
        try {
            String pushAllCommand = "{\"pushing\":{\"sequence_id\":\"0\",\"command\":\"pushall\"}}";
            sendCommand(deviceId, pushAllCommand);
        } catch (Exception e) {
            log.error("推送所有状态失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new BusinessException(SEND_MESSAGE_FAILED);
        }
    }

    /**
     * 发送 AMS 槽位耗材设置命令
     */
    public void amsFilamentSetting(String deviceId, Integer amsNo, Integer slotNo,
                                   String trayColor, String trayInfoIdx) {
        try {
            Map<String, Object> commandMap = createAmsFilamentCommand(
                    amsNo, slotNo, trayColor, trayInfoIdx
            );
            if (commandMap == null) {
                return;
            }
            // 使用 ObjectMapper 转 JSON
            String commandJson = objectMapper.writeValueAsString(commandMap);

            sendCommand(deviceId, commandJson);
        } catch (Exception e) {
            log.error("AMS 槽位耗材设置失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new BusinessException(SEND_MESSAGE_FAILED);
        }
    }

    // ===================== 灯光控制功能 =====================

    /**
     * 开启打印腔灯
     *
     * @param deviceId 设备 ID
     */
    public void turnOnChamberLight(String deviceId, String ledNode) {
        try {
            Map<String, Object> command = createLightCommand(ledNode, "on");
            String commandJson = objectMapper.writeValueAsString(command);
            sendCommand(deviceId, commandJson);
        } catch (Exception e) {
            log.error("开启打印腔灯失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new BusinessException(SEND_MESSAGE_FAILED);
        }
    }

    /**
     * 关闭打印腔灯
     *
     * @param deviceId 设备 ID
     */
    public void turnOffChamberLight(String deviceId, String ledNode) {
        try {
            Map<String, Object> command = createLightCommand(ledNode, "off");
            String commandJson = objectMapper.writeValueAsString(command);
            sendCommand(deviceId, commandJson);
        } catch (Exception e) {
            log.error("关闭打印腔灯失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new BusinessException(SEND_MESSAGE_FAILED);
        }
    }

    /**
     * 等待稳定
     *
     * @param delayMs 延迟时间（毫秒）
     */
    public void waitStabilization(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 创建灯光控制命令
     *
     * @param ledNode 灯光节点名称
     * @param ledMode 灯光模式（on/off）
     * @return 命令 Map
     */
    private Map<String, Object> createLightCommand(String ledNode, String ledMode) {
        Map<String, Object> system = new HashMap<>();
        system.put("sequence_id", "0");
        system.put("command", "ledctrl");
        system.put("led_node", ledNode);
        system.put("led_mode", ledMode);
        system.put("led_on_time", 500);
        system.put("led_off_time", 500);
        system.put("loop_times", 0);
        system.put("interval_time", 0);

        Map<String, Object> command = new HashMap<>();
        command.put("system", system);
        return command;
    }

    /**
     * 构建 AMS 槽位耗材设置命令
     */
    private Map<String, Object> createAmsFilamentCommand(
            Integer amsId,
            Integer slotId,
            String trayColor,
            String trayInfoIdx
    ) {
        FilamentUtils.FilamentInfo filamentInfo = filamentUtils.getFilamentInfo(trayInfoIdx);
        if (filamentInfo == null) {
            return null;
        }
        Map<String, Object> print = new HashMap<>();
        print.put("sequence_id", 0);
        print.put("command", "ams_filament_setting");
        print.put("ams_id", amsId);
        print.put("tray_id", slotId);
        print.put("tray_info_idx", trayInfoIdx);
        print.put("tray_color", trayColor.replace("#", "") + "FF");
        print.put("nozzle_temp_max", filamentInfo.getNozzle_temperature_range_high());
        print.put("nozzle_temp_min", filamentInfo.getNozzle_temperature_range_low());
        print.put("tray_type", filamentInfo.getFilament_type());
        Map<String, Object> command = new HashMap<>();
        command.put("print", print);
        return command;
    }
}
