package cn.mars.marsprinterhub.bambu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bambu MQTT配置类
 * 用于配置MQTT连接相关参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bambu.mqtt")
public class BambuMqttConfig {
    
    /**
     * 云端MQTT主机地址（中国大陆）
     */
    private String cloudHost = "cn.mqtt.bambulab.com";
    
    /**
     * MQTT端口（TLS加密）
     */
    private int port = 8883;
    
    /**
     * 连接超时时间（秒）
     */
    private int connectionTimeout = 30;
    
    /**
     * 保持连接间隔（秒）
     */
    private int keepAliveInterval = 60;
    
    /**
     * 自动重连
     */
    private boolean automaticReconnect = true;
    
    /**
     * 清理会话
     */
    private boolean cleanSession = true;
    
    /**
     * 最大重连尝试次数
     */
    private int maxReconnectAttempts = 5;
    
    /**
     * 重连间隔（毫秒）
     */
    private long reconnectInterval = 5000;
    
    /**
     * 本地连接优先
     */
    private boolean preferLocalConnection = true;
    
    /**
     * 本地连接超时时间（秒）
     */
    private int localConnectionTimeout = 10;
    
    /**
     * 消息质量等级
     */
    private int qos = 0;

    /**
     * 是否初始化所有MQTT连接
     */
    private boolean initAll = false;
    
    /**
     * 客户端ID前缀
     */
    private String clientIdPrefix = "mars-printer-hub";
    
    /**
     * 是否启用SSL
     */
    private boolean sslEnabled = true;
    
    /**
     * 订阅主题模式
     */
    private String subscribeTopicPattern = "device/%s/report";
    
    /**
     * 发布主题模式
     */
    private String publishTopicPattern = "device/%s/request";
}