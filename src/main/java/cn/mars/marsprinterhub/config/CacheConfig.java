package cn.mars.marsprinterhub.config;

import cn.hutool.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 缓存配置类
 */
@Configuration
public class CacheConfig {

    /**
     * 设备状态热缓存配置
     * 用于存储高频访问的设备状态数据
     */
    @Bean
    public Cache<String, JSONObject> deviceStatusCache() {
        return Caffeine.newBuilder()
                // 最大缓存200个设备状态
                .maximumSize(200)
                // 写入后10分钟过期
                .expireAfterWrite(Duration.ofMinutes(10))
                // 启用统计功能，用于监控缓存性能
                .recordStats()
                .build();
    }

    /**
     * 设备列表缓存配置
     * 用于缓存用户的设备列表
     */
    @Bean
    public Cache<String, Object> deviceListCache() {
        return Caffeine.newBuilder()
                // 最大缓存100个用户的设备列表
                .maximumSize(100)
                // 写入后5分钟过期
                .expireAfterWrite(Duration.ofMinutes(5))
                // 启用统计功能
                .recordStats()
                .build();
    }
}