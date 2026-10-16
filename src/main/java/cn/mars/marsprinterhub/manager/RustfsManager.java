package cn.mars.marsprinterhub.manager;

import cn.mars.marsprinterhub.config.RustfsConfig;
import cn.mars.marsprinterhub.utils.ObjectStorageContentTypeUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 * RustFS 管理类，用于操作对象存储（兼容 S3 API）
 */
@Slf4j
public class RustfsManager implements ObjectStorageManager {

    private final RustfsConfig rustfsConfig; // 配置属性
    private S3Client s3Client;                  // S3 客户端

    public RustfsManager(RustfsConfig rustfsConfig) {
        this.rustfsConfig = rustfsConfig;
    }

    /**
     * 初始化 S3 客户端
     * 并自动创建默认桶（如果指定了 defaultBucket）
     */
    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(rustfsConfig.getEndpoint()))
                .region(Region.of(rustfsConfig.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(rustfsConfig.getAccessKey(), rustfsConfig.getSecretKey())
                        )
                )
                .forcePathStyle(true) // 关键配置！RustFS 需启用 Path-Style
                .build();
    }

    @Override
    public boolean putObject(String key, InputStream inputStream, long length) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(rustfsConfig.getDefaultBucket())
                    .key(key)
                    .contentLength(length)
                    .contentType(ObjectStorageContentTypeUtils.detectContentType(key))
                    .build();
            s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, length));
            return true;
        } catch (Exception e) {
            log.error("上传对象失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public boolean putObject(String key, byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return putObject(key, inputStream, bytes.length);
        } catch (Exception e) {
            log.error("上传对象失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public boolean putObject(String key, File file) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(rustfsConfig.getDefaultBucket())
                    .key(key)
                    .contentType(ObjectStorageContentTypeUtils.detectContentType(key))
                    .build(), file.toPath());
            return true;
        } catch (Exception e) {
            log.error("上传文件失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public String getUrl(String key) {
        return "/" + rustfsConfig.getDefaultBucket() + "/" + key;
    }

    @Override
    public String getStorageType() {
        return "RUSTFS";
    }

    @Override
    public boolean isAvailable() {
        try {
            return s3Client != null && rustfsConfig != null
                    && rustfsConfig.getEndpoint() != null
                    && rustfsConfig.getDefaultBucket() != null;
        } catch (Exception e) {
            log.error("检查RustFS可用性失败", e);
            return false;
        }
    }

    // ======== 向后兼容方法 ========

    /**
     * 删除对象（向后兼容）
     */
    public boolean deleteObject(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(rustfsConfig.getDefaultBucket())
                    .key(key)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("删除对象失败，key={}", key, e);
            return false;
        }
    }

    /**
     * 上传对象并返回 S3 结果（向后兼容）
     */
    public PutObjectResponse putObjectWithResult(String key, InputStream inputStream, long length) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(rustfsConfig.getDefaultBucket())
                    .key(key)
                    .contentLength(length)
                    .contentType(ObjectStorageContentTypeUtils.detectContentType(key))
                    .build();
            return s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, length));
        } catch (Exception e) {
            throw new RuntimeException("上传对象失败，key=" + key, e);
        }
    }
}
