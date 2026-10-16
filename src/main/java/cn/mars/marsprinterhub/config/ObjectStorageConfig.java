package cn.mars.marsprinterhub.config;

import cn.mars.marsprinterhub.manager.CosManager;
import cn.mars.marsprinterhub.manager.ObjectStorageManager;
import cn.mars.marsprinterhub.manager.R2Manager;
import cn.mars.marsprinterhub.manager.RustfsManager;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 对象存储配置类
 * 根据配置自动装配相应的存储管理器
 */
@Configuration
@Slf4j
public class ObjectStorageConfig {

    /**
     * 腾讯云 COS 客户端配置
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "cos.client", name = "enabled", havingValue = "true")
    public COSClient cosClient(CosClientConfig cosClientConfig) {
        log.info("初始化腾讯云 COS 客户端");
        COSCredentials cred = new BasicCOSCredentials(cosClientConfig.getSecretId(), cosClientConfig.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosClientConfig.getRegion()));
        return new COSClient(cred, clientConfig);
    }

    /**
     * 腾讯云 COS 管理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "cos.client", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "cosManager")
    public CosManager cosManager(CosClientConfig cosClientConfig, COSClient cosClient) {
        log.info("注册腾讯云 COS 存储管理器");
        return new CosManager(cosClientConfig, cosClient);
    }

    /**
     * RustFS 管理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rustfs.client", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "rustfsManager")
    public RustfsManager rustfsManager(RustfsConfig rustfsConfig) {
        log.info("注册 RustFS 存储管理器");
        return new RustfsManager(rustfsConfig);
    }

    /**
     * Cloudflare R2 管理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "r2.client", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "r2Manager")
    public R2Manager r2Manager(R2Config r2Config) {
        log.info("注册 Cloudflare R2 存储管理器");
        return new R2Manager(r2Config);
    }

    /**
     * 默认对象存储管理器
     * 优先级：COS > R2 > RustFS
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectStorageManager.class)
    public ObjectStorageManager objectStorageManager(
            // 使用 required = false 避免在没有配置时启动失败
            @org.springframework.beans.factory.annotation.Autowired(required = false) CosManager cosManager,
            @org.springframework.beans.factory.annotation.Autowired(required = false) R2Manager r2Manager,
            @org.springframework.beans.factory.annotation.Autowired(required = false) RustfsManager rustfsManager) {

        // 优先返回可用的 COS 管理器
        if (cosManager != null && cosManager.isAvailable()) {
            log.info("使用腾讯云 COS 作为默认对象存储管理器");
            return cosManager;
        }

        // 其次返回可用的 R2 管理器
        if (r2Manager != null && r2Manager.isAvailable()) {
            log.info("使用 Cloudflare R2 作为默认对象存储管理器");
            return r2Manager;
        }

        // 再次返回可用的 RustFS 管理器
        if (rustfsManager != null && rustfsManager.isAvailable()) {
            log.info("使用 RustFS 作为默认对象存储管理器");
            return rustfsManager;
        }

        // 如果都不可用，返回一个空实现
        log.warn("没有可用的对象存储管理器，返回空实现");
        return new NoOpObjectStorageManager();
    }

    /**
     * 空实现的对象存储管理器
     * 当没有配置任何对象存储时使用
     */
    private static class NoOpObjectStorageManager implements ObjectStorageManager {

        @Override
        public boolean putObject(String key, java.io.File file) {
            log.warn("对象存储未配置，putObject 操作被忽略: key={}", key);
            return false;
        }

        @Override
        public boolean putObject(String key, java.io.InputStream inputStream, long length) {
            log.warn("对象存储未配置，putObject 操作被忽略: key={}", key);
            return false;
        }

        @Override
        public boolean putObject(String key, byte[] bytes) {
            log.warn("对象存储未配置，putObject 操作被忽略: key={}", key);
            return false;
        }

        @Override
        public String getUrl(String key) {
            log.warn("对象存储未配置，getUrl 操作被忽略: key={}", key);
            return null;
        }

        @Override
        public String getStorageType() {
            return "NONE";
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }
}