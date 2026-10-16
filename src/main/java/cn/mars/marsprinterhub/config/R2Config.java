package cn.mars.marsprinterhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cloudflare R2 配置属性类
 * R2 兼容 S3 API，通过 application.yml 配置 endpoint、accessKey、secretKey 等
 */
@Component
@ConfigurationProperties(prefix = "r2.client")
@Data
public class R2Config {

    /**
     * 是否启用 R2 客户端
     */
    private boolean enabled;

    /**
     * Cloudflare 账户 ID
     */
    private String accountId;

    /**
     * R2 端点 URL
     * 格式：https://{accountId}.r2.cloudflarestorage.com
     */
    private String endpoint;

    /**
     * Access Key ID
     */
    private String accessKey;

    /**
     * Secret Access Key
     */
    private String secretKey;

    /**
     * 默认存储桶名称
     */
    private String bucket;

    /**
     * 公开访问 URL（用于生成访问链接）
     * 例如：https://your-domain.com 或 https://pub-xxx.r2.dev
     */
    private String publicUrl;
}
