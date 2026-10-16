package cn.mars.marsprinterhub.manager;

import cn.mars.marsprinterhub.config.R2Config;
import cn.mars.marsprinterhub.utils.ObjectStorageContentTypeUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 * Cloudflare R2 管理类
 * R2 兼容 S3 API，使用 AWS S3 SDK 进行操作
 */
@Slf4j
public class R2Manager implements ObjectStorageManager {

    private final R2Config r2Config;
    private S3Client s3Client;

    public R2Manager(R2Config r2Config) {
        this.r2Config = r2Config;
    }

    /**
     * 初始化 S3 客户端连接 R2
     */
    @PostConstruct
    public void init() {
        String endpoint = r2Config.getEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            // 根据 accountId 自动构建端点
            endpoint = "https://" + r2Config.getAccountId() + ".r2.cloudflarestorage.com";
        }

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto")) // R2 使用 "auto" 区域
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(r2Config.getAccessKey(), r2Config.getSecretKey())
                        )
                )
                .forcePathStyle(true) // R2 需要 Path-Style
                .build();

        log.info("R2 客户端初始化完成，endpoint={}", endpoint);
    }

    @Override
    public boolean putObject(String key, File file) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(r2Config.getBucket())
                    .key(key)
                    .contentType(ObjectStorageContentTypeUtils.detectContentType(key))
                    .build(), file.toPath());
            return true;
        } catch (Exception e) {
            log.error("R2 上传文件失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public boolean putObject(String key, InputStream inputStream, long length) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(r2Config.getBucket())
                    .key(key)
                    .contentLength(length)
                    .contentType(ObjectStorageContentTypeUtils.detectContentType(key))
                    .build();
            s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, length));
            return true;
        } catch (Exception e) {
            log.error("R2 上传对象失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public boolean putObject(String key, byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return putObject(key, inputStream, bytes.length);
        } catch (Exception e) {
            log.error("R2 上传对象失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public String getUrl(String key) {
        String publicUrl = r2Config.getPublicUrl();
        if (publicUrl != null && !publicUrl.isEmpty()) {
            // 移除末尾斜杠
            if (publicUrl.endsWith("/")) {
                publicUrl = publicUrl.substring(0, publicUrl.length() - 1);
            }
            return publicUrl + "/" + key;
        }
        // 如果没有配置公开 URL，返回相对路径
        return "/" + r2Config.getBucket() + "/" + key;
    }

    @Override
    public String getStorageType() {
        return "R2";
    }

    @Override
    public boolean isAvailable() {
        try {
            return s3Client != null && r2Config != null
                    && r2Config.getBucket() != null
                    && (r2Config.getEndpoint() != null || r2Config.getAccountId() != null);
        } catch (Exception e) {
            log.error("检查 R2 可用性失败", e);
            return false;
        }
    }

    // ======== 扩展方法 ========

    /**
     * 删除对象
     */
    public boolean deleteObject(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(r2Config.getBucket())
                    .key(key)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("R2 删除对象失败，key={}", key, e);
            return false;
        }
    }

    /**
     * 上传对象并返回 S3 响应
     */
    public PutObjectResponse putObjectWithResult(String key, InputStream inputStream, long length) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(r2Config.getBucket())
                    .key(key)
                    .contentLength(length)
                    .contentType(ObjectStorageContentTypeUtils.detectContentType(key))
                    .build();
            return s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, length));
        } catch (Exception e) {
            throw new RuntimeException("R2 上传对象失败，key=" + key, e);
        }
    }
}
