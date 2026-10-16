package cn.mars.marsprinterhub.bambu.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bambu打印机MQTT连接信息
 * 封装连接所需的所有参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BambuMqttConnectionInfo {
    
    /**
     * 打印机设备序列号
     */
    private String deviceId;
    
    /**
     * 打印机IP地址（本地连接使用）
     */
    private String printerIp;
    
    /**
     * 访问码（本地连接的密码）
     */
    private String accessCode;
    
    /**
     * 用户名（云端连接使用）
     */
    private String username;
    
    /**
     * 密码（云端连接使用）
     */
    private String password;
    
    /**
     * 连接类型：LOCAL(本地) 或 CLOUD(云端)
     */
    private ConnectionType connectionType;
    
    /**
     * MQTT主机地址
     */
    private String mqttHost;
    
    /**
     * MQTT端口
     */
    private int mqttPort;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 是否启用SSL
     */
    private boolean sslEnabled;
    
    /**
     * 连接超时时间（秒）
     */
    private int connectionTimeout;
    
    /**
     * 保持连接间隔（秒）
     */
    private int keepAliveInterval;
    
    /**
     * 是否自动重连
     */
    private boolean autoReconnect;
    
    /**
     * 是否清理会话
     */
    private boolean cleanSession;
    
    /**
     * 连接类型枚举
     */
    public enum ConnectionType {
        /**
         * 本地连接
         */
        LOCAL,
        
        /**
         * 云端连接
         */
        CLOUD
    }
    
    /**
     * 创建本地连接信息
     */
    public static BambuMqttConnectionInfo createLocalConnection(String deviceId, String printerIp, String accessCode) {
        return BambuMqttConnectionInfo.builder()
                .deviceId(deviceId)
                .printerIp(printerIp)
                .accessCode(accessCode)
                .connectionType(ConnectionType.LOCAL)
                .mqttHost(printerIp)
                .mqttPort(8883)
                .username("bblp")
                .password(accessCode)
                .sslEnabled(true)
                .connectionTimeout(30)
                .keepAliveInterval(60)
                .autoReconnect(true)
                .cleanSession(true)
                .build();
    }
    
    /**
     * 创建云端连接信息
     */
    public static BambuMqttConnectionInfo createCloudConnection(String deviceId, String username, String password, String mqttHost) {
        return BambuMqttConnectionInfo.builder()
                .deviceId(deviceId)
                .username(username)
                .password(password)
                .connectionType(ConnectionType.CLOUD)
                .mqttHost(mqttHost)
                .mqttPort(8883)
                .sslEnabled(true)
                .connectionTimeout(30)
                .keepAliveInterval(60)
                .autoReconnect(true)
                .cleanSession(true)
                .build();
    }
    
    /**
     * 获取订阅主题
     */
    public String getSubscribeTopic() {
        return "device/" + deviceId + "/report";
    }
    
    /**
     * 获取发布主题
     */
    public String getPublishTopic() {
        return "device/" + deviceId + "/request";
    }
    
    /**
     * 检查连接信息是否有效
     */
    public boolean isValid() {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return false;
        }
        
        if (connectionType == ConnectionType.LOCAL) {
            return printerIp != null && !printerIp.trim().isEmpty() 
                    && accessCode != null && !accessCode.trim().isEmpty();
        } else {
            return username != null && !username.trim().isEmpty() 
                    && password != null && !password.trim().isEmpty();
        }
    }
    
    /**
     * 获取连接描述
     */
    public String getConnectionDescription() {
        if (connectionType == ConnectionType.LOCAL) {
            return String.format("本地连接 - %s:%d", printerIp, mqttPort);
        } else {
            return String.format("云端连接 - %s:%d", mqttHost, mqttPort);
        }
    }
}
