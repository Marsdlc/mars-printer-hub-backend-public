package cn.mars.marsprinterhub.bambu.mqtt;

import cn.mars.marsprinterhub.bambu.config.BambuMqttConfig;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.MqttMessageReceivedEvent;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.enums.MqttConnectionTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static cn.mars.marsprinterhub.exception.ErrorCode.*;

/**
 * Bambu MQTT客户端管理器
 * 负责管理MQTT客户端的生命周期、连接池
 */
@Slf4j
@Component
public class BambuMqttClientManager {

    @Resource
    private BambuMqttConfig bambuMqttConfig;
    @Resource
    private EventPublisher eventPublisher;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 客户端连接池
     * Key: deviceId, Value: MqttClient
     */
    private final Map<String, MqttClient> clientPool = new ConcurrentHashMap<>();

    /**
     * 用户连接索引
     * Key: userId, Value: deviceId集合
     */
    private final Map<Long, Set<String>> userDeviceIndex = new ConcurrentHashMap<>();

    /**
     * MQTT连接反向索引
     * Key: deviceId, Value: 当前连接所属userId
     */
    private final Map<String, Long> deviceClientOwnerIndex = new ConcurrentHashMap<>();

    /**
     * 连接信息缓存
     * Key: deviceId, Value: BambuConnectionInfo
     */
    private final Map<String, BambuMqttConnectionInfo> connectionInfoCache = new ConcurrentHashMap<>();

    /**
     * 每台设备独立加锁，避免并发创建/断开导致 client 丢失引用后线程泄漏
     */
    private final Map<String, ReentrantLock> deviceLocks = new ConcurrentHashMap<>();

    /**
     * 创建MQTT客户端
     */
    public void createClient(BambuMqttConnectionInfo connectionInfo) {
        ThrowUtils.throwIf(!connectionInfo.isValid(), INVALID_CONNECTION_INFO);

        String deviceId = connectionInfo.getDeviceId();
        ReentrantLock lock = getDeviceLock(deviceId);
        lock.lock();
        try {
            doCreateClient(connectionInfo, deviceId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 创建MQTT客户端，并登记用户连接索引
     */
    public void createClient(Long userId, BambuMqttConnectionInfo connectionInfo) {
        ThrowUtils.throwIf(userId == null, INVALID_CONNECTION_INFO);
        ThrowUtils.throwIf(!connectionInfo.isValid(), INVALID_CONNECTION_INFO);

        String deviceId = connectionInfo.getDeviceId();
        ReentrantLock lock = getDeviceLock(deviceId);
        lock.lock();
        try {
            doCreateClient(connectionInfo, deviceId);
            bindClientOwner(userId, deviceId);
        } finally {
            lock.unlock();
        }
    }

    private void doCreateClient(BambuMqttConnectionInfo connectionInfo, String deviceId) {
        MqttClient client = null;

        // 如果已存在连接，先断开
        if (clientPool.containsKey(deviceId)) {
            doDisconnectClient(deviceId);
        }

        // 缓存连接信息（无论连接是否成功都要缓存，用于重连）
        connectionInfoCache.put(deviceId, connectionInfo);

        try {
            // 生成客户端ID
            String clientId = generateClientId(connectionInfo);

            // 创建MQTT客户端
            client = new MqttClient(
                    String.format("ssl://%s:%d", connectionInfo.getMqttHost(), connectionInfo.getMqttPort()),
                    clientId,
                    new MemoryPersistence()
            );

            // 设置连接选项
            MqttConnectOptions options = createConnectOptions(connectionInfo);

            // 设置回调
            client.setCallback(new BambuMqttCallback(deviceId, this, objectMapper));

            // 建立连接
            log.info("正在连接到Bambu打印机: {} - {}", deviceId, connectionInfo.getConnectionDescription());
            client.connect(options);

            // 订阅主题
            String subscribeTopic = connectionInfo.getSubscribeTopic();
            client.subscribe(subscribeTopic, bambuMqttConfig.getQos());
            log.info("已订阅主题: {} (设备: {})", subscribeTopic, deviceId);

            // 缓存客户端
            clientPool.put(deviceId, client);

            log.info("MQTT客户端连接成功: {} - {}", deviceId, connectionInfo.getConnectionDescription());
        } catch (MqttException e) {
            closeQuietly(client, deviceId);
            log.error("MQTT 客户端初始连接失败: {}", deviceId, e);
            throw new BusinessException(CONNECTION_FAILED);
        }
    }

    /**
     * 断开客户端连接
     */
    public void disconnectClient(String deviceId) {
        ReentrantLock lock = getDeviceLock(deviceId);
        lock.lock();
        try {
            doDisconnectClient(deviceId);
        } finally {
            lock.unlock();
        }
    }

    private void doDisconnectClient(String deviceId) {
        MqttClient client = clientPool.get(deviceId);
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
                log.info("MQTT客户端已断开: {}", deviceId);
            } catch (MqttException e) {
                log.error("断开MQTT客户端失败: {}", deviceId, e);
            }
        }

        // 清理缓存
        clientPool.remove(deviceId);
        connectionInfoCache.remove(deviceId);
        unbindClientOwner(deviceId);
    }

    private ReentrantLock getDeviceLock(String deviceId) {
        return deviceLocks.computeIfAbsent(deviceId, key -> new ReentrantLock());
    }

    private void closeQuietly(MqttClient client, String deviceId) {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException e) {
            log.warn("关闭MQTT客户端失败: {}", deviceId, e);
        }
    }

    /**
     * 检查客户端是否已连接
     */
    public boolean isConnected(String deviceId) {
        MqttClient client = clientPool.get(deviceId);
        return client != null && client.isConnected();
    }

    /**
     * 获取指定用户当前已连接的设备数
     */
    public int getUserClientCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return getUserConnectedDevices(userId).size();
    }

    /**
     * 获取指定用户当前已连接的设备ID快照
     */
    public Set<String> getUserConnectedDevices(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        Set<String> deviceIds = userDeviceIndex.get(userId);
        if (deviceIds == null) {
            return Set.of();
        }
        return deviceIds.stream()
                .filter(this::isConnected)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * 获取设备当前MQTT连接所属用户ID
     */
    public Long getDeviceUserId(String deviceId) {
        return deviceClientOwnerIndex.get(deviceId);
    }

    /**
     * 登记MQTT连接所属用户。连接被重新归属时，会先从旧用户索引中移除。
     */
    public void bindClientOwner(Long userId, String deviceId) {
        if (userId == null || deviceId == null || deviceId.isBlank()) {
            return;
        }

        Long oldUserId = deviceClientOwnerIndex.put(deviceId, userId);
        if (oldUserId != null && !oldUserId.equals(userId)) {
            Set<String> oldDeviceIds = userDeviceIndex.get(oldUserId);
            if (oldDeviceIds != null) {
                oldDeviceIds.remove(deviceId);
                if (oldDeviceIds.isEmpty()) {
                    userDeviceIndex.remove(oldUserId, oldDeviceIds);
                }
            }
        }
        userDeviceIndex.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(deviceId);
    }

    /**
     * 清理MQTT连接所属用户索引
     */
    public void unbindClientOwner(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        Long userId = deviceClientOwnerIndex.remove(deviceId);
        if (userId == null) {
            return;
        }
        Set<String> deviceIds = userDeviceIndex.get(userId);
        if (deviceIds != null) {
            deviceIds.remove(deviceId);
            if (deviceIds.isEmpty()) {
                userDeviceIndex.remove(userId, deviceIds);
            }
        }
    }

    /**
     * 获取客户端连接类型
     */
    public MqttConnectionTypeEnum getConnectionType(String deviceId) {
        MqttClient client = clientPool.get(deviceId);
        if (client == null || !client.isConnected()) {
            return MqttConnectionTypeEnum.NONE;
        }
        if (client.getServerURI().contains("mqtt")) {
            return MqttConnectionTypeEnum.CLOUD;
        } else {
            return MqttConnectionTypeEnum.LOCAL;
        }
    }

    /**
     * 发布消息
     */
    public void publishMessage(String deviceId, String message) throws MqttException {
        MqttClient client = clientPool.get(deviceId);
        if (client == null || !client.isConnected()) {
            throw new BusinessException(CLIENT_NOT_FOUND);
        }

        BambuMqttConnectionInfo connectionInfo = connectionInfoCache.get(deviceId);
        if (connectionInfo == null) {
            throw new BusinessException(INVALID_CONNECTION_INFO);
        }

        String publishTopic = connectionInfo.getPublishTopic();
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(bambuMqttConfig.getQos());

        client.publish(publishTopic, mqttMessage);
    }

    /**
     * 创建连接选项
     */
    private MqttConnectOptions createConnectOptions(BambuMqttConnectionInfo connectionInfo) {
        MqttConnectOptions options = new MqttConnectOptions();

        // 基本连接参数
        options.setUserName(connectionInfo.getUsername());
        options.setPassword(connectionInfo.getPassword().toCharArray());
        options.setConnectionTimeout(connectionInfo.getConnectionTimeout());
        options.setKeepAliveInterval(connectionInfo.getKeepAliveInterval());
        options.setAutomaticReconnect(false); // 我们自己处理重连
        options.setCleanSession(connectionInfo.isCleanSession());

        // SSL配置
        if (connectionInfo.isSslEnabled()) {
            try {
                options.setHttpsHostnameVerificationEnabled(false);
                // 创建SSLContext对象
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{
                        new X509TrustManager() {
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
                }, null);

                // 设置SSLContext为默认
                SSLContext.setDefault(sslContext);
                options.setSocketFactory(sslContext.getSocketFactory());
            } catch (Exception e) {
                log.error("SSL配置失败", e);
            }
        }

        return options;
    }

    /**
     * 生成客户端ID
     */
    private String generateClientId(BambuMqttConnectionInfo connectionInfo) {
        return String.format("%s_%s_%d",
                bambuMqttConfig.getClientIdPrefix(),
                connectionInfo.getDeviceId(),
                System.currentTimeMillis());
    }

    /**
     * 获取所有已连接的设备ID
     */
    public java.util.Set<String> getConnectedDevices() {
        return clientPool.entrySet().stream()
                .filter(entry -> entry.getValue().isConnected())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 创建回调对象
     */
    public MqttCallback create(String deviceId, BambuMqttClientManager manager) {
        return new BambuMqttCallback(deviceId, manager, objectMapper);
    }

    /**
     * 内部 record，实现 MqttCallback
     */
    private record BambuMqttCallback(
            String deviceId,
            BambuMqttClientManager manager,
            ObjectMapper objectMapper
    ) implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

            if (payload.equals("{}")) {
                return;
            }

            try {
                // 高频消息处理：复用注入的 ObjectMapper
                JsonNode root = objectMapper.readTree(payload);
                JsonNode printNode = root.get("print");
                BambuMqttMessage bambuMessage = objectMapper.treeToValue(printNode, BambuMqttMessage.class);
                if (bambuMessage != null) {
                    // 发布MQTT消息接收事件
                    MqttMessageReceivedEvent event = new MqttMessageReceivedEvent(
                            deviceId, bambuMessage);
                    manager.eventPublisher.publish(event);
                }
            } catch (Exception e) {
                log.error("解析或处理MQTT消息失败: deviceId={}, topic={}, payload={}", deviceId, topic, payload, e);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            try {
                log.info("消息发送完成: deviceId={}, messageId={}", deviceId, token.getMessageId());
            } catch (Exception e) {
                log.warn("消息发送完成回调异常: deviceId={}", deviceId, e);
            }
        }
    }
}
