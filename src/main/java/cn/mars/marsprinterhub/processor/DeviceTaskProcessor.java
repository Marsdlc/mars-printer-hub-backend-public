package cn.mars.marsprinterhub.processor;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.mars.marsprinterhub.manager.ObjectStorageManager;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_TASK_PREFIX;
import static cn.mars.marsprinterhub.utils.DateUtils.formatTimestamp;

@Component
@Slf4j
public class DeviceTaskProcessor {

    @Autowired(required = false)
    private ObjectStorageManager objectStorageManager;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 保存任务封面URL到Redis
     * 注意：此方法现在通过事件驱动调用，不再直接调用BambuCloudService
     *
     * @param deviceId 设备ID
     * @param coverUrl 封面URL（来自BambuCloudService获取的任务信息）
     */
    public void saveTaskCoverUrl(String deviceId, String coverUrl) {
        if (objectStorageManager == null || !objectStorageManager.isAvailable()) {
            log.warn("对象存储未配置，跳过封面上传");
            return;
        }

        if (coverUrl == null || coverUrl.trim().isEmpty()) {
            log.warn("封面URL为空，跳过上传，设备: {}", deviceId);
            return;
        }

        try {
            String redisTaskKey = DEVICE_TASK_PREFIX + deviceId;

            // 根据拓竹返回的临时URL，存储到自己的对象存储中
            String timestamp = formatTimestamp(LocalDateTime.now());
            String ossKey = timestamp + "_taskCover" + ".png";
            String uploadedUrl = uploadFromUrl(coverUrl, ossKey);
            if (uploadedUrl != null && !uploadedUrl.isEmpty()) {
                redisUtils.set(redisTaskKey, uploadedUrl);
            } else {
                log.error("任务封面上传失败，设备: {}", deviceId);
            }

        } catch (Exception e) {
            log.error("保存任务封面失败，设备: {}, error: {}", deviceId, e.getMessage(), e);
        }
    }

    private String uploadFromUrl(String fileUrl, String cosKey) {
        try (HttpResponse response = HttpRequest.get(fileUrl).execute();
             InputStream in = response.bodyStream()) {
            boolean success = objectStorageManager.putObject(cosKey, in, response.contentLength());
            if (success) {
                return objectStorageManager.getUrl(cosKey);
            } else {
                log.error("上传文件失败: 对象存储操作失败");
                return "";
            }
        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            return "";
        }
    }
}
