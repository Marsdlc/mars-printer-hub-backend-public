package cn.mars.marsprinterhub.manager;

import cn.mars.marsprinterhub.config.CosClientConfig;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.utils.ObjectStorageContentTypeUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

@Slf4j
public class CosManager implements ObjectStorageManager {

    private final CosClientConfig cosClientConfig;
    private final COSClient cosClient;

    public CosManager(CosClientConfig cosClientConfig, COSClient cosClient) {
        this.cosClientConfig = cosClientConfig;
        this.cosClient = cosClient;
    }

    @Override
    public boolean putObject(String key, File file) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(ObjectStorageContentTypeUtils.detectContentType(key));
            putObjectRequest.setMetadata(metadata);
            cosClient.putObject(putObjectRequest);
            return true;
        } catch (Exception e) {
            log.error("上传文件失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public boolean putObject(String key, InputStream inputStream, long length) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(length);
            metadata.setContentType(ObjectStorageContentTypeUtils.detectContentType(key));

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosClientConfig.getBucket(),
                    key,
                    inputStream,
                    metadata
            );
            cosClient.putObject(putObjectRequest);
            return true;
        } catch (Exception e) {
            log.error("上传对象失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public boolean putObject(String key, byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(ObjectStorageContentTypeUtils.detectContentType(key));

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosClientConfig.getBucket(),
                    key,
                    inputStream,
                    metadata
            );
            cosClient.putObject(putObjectRequest);
            return true;
        } catch (Exception e) {
            log.error("上传对象失败，key={}", key, e);
            return false;
        }
    }

    @Override
    public String getUrl(String key) {
        return cosClientConfig.getHost() + "/" + key;
    }

    @Override
    public String getStorageType() {
        return "COS";
    }

    @Override
    public boolean isAvailable() {
        try {
            return cosClient != null && cosClientConfig != null && cosClientConfig.getBucket() != null;
        } catch (Exception e) {
            log.error("检查COS可用性失败", e);
            return false;
        }
    }

    // ======== 向后兼容方法 ========

    /**
     * 获取COS对象（原始方法，保持向后兼容）
     */
    public COSObject getCOSObject(String key) {
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
            return cosClient.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("获取COS对象失败，key={}", key, e);
            return null;
        }
    }

    /**
     * 上传对象并返回COS结果（原始方法，保持向后兼容）
     */
    public PutObjectResult putObjectWithResult(String key, byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(ObjectStorageContentTypeUtils.detectContentType(key));

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosClientConfig.getBucket(),
                    key,
                    inputStream,
                    metadata
            );
            return cosClient.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传对象失败，key=" + key);
        }
    }

    /**
     * 删除对象（向后兼容）
     */
    public boolean deleteObject(String key) {
        try {
            cosClient.deleteObject(cosClientConfig.getBucket(), key);
            return true;
        } catch (Exception e) {
            log.error("删除对象失败，key={}", key, e);
            return false;
        }
    }
}
