package cn.mars.marsprinterhub.bambu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 拓竹云服务配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bambu.cloud")
public class BambuCloudConfig {
    
    /**
     * 是否启用拓竹云服务
     */
    private boolean enabled = true;
    
    /**
     * API基础URL（默认为中国区域）
     */
    private String baseUrl = "https://api.bambulab.cn";

    /**
     * 请求超时时间（毫秒）
     */
    private int requestTimeout = 10000;
    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;
    
    /**
     * 最大重试次数
     */
    private int maxRetries = 3;
    
    /**
     * 重试间隔（毫秒）
     */
    private int retryInterval = 1000;
    
    /**
     * 是否启用SSL
     */
    private boolean sslEnabled = true;
    
    /**
     * 用户代理字符串
     */
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    
    /**
     * 缓存配置
     */
    private Cache cache = new Cache();
    
    @Data
    public static class Cache {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;
        
        /**
         * 设备列表缓存时间（秒）
         */
        private int deviceListTtl = 300;
        
        /**
         * 认证token缓存时间（秒）
         */
        private int authTokenTtl = 3600;
    }
}