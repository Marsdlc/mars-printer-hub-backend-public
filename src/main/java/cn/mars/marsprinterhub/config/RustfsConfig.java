package cn.mars.marsprinterhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RustFS 配置属性类
 * 通过 application.yml 配置 endpoint、accessKey、secretKey 等
 */
@Component
@ConfigurationProperties(prefix = "rustfs.client")
@Data
public class RustfsConfig {

    /**
     * 是否启用 RustFS 客户端
     */
    private boolean enabled;

    /**
     * RustFS 访问地址
     */
    private String endpoint;

    /**
     * 区域，可随便写，RustFS 不校验
     */
    private String region = "us-east-1";

    /**
     * Access Key
     */
    private String accessKey;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * 默认桶名称，可选
     */
    private String defaultBucket;

    /**
     * 桶域名，可选
     */
    private String domain;

}