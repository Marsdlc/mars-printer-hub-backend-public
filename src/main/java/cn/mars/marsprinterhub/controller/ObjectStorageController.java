package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.manager.ObjectStorageManager;
import cn.mars.marsprinterhub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 对象存储控制器
 * 提供对象存储上传相关接口
 */
@RestController
@RequestMapping("/storage")
@Tag(name = "ObjectStorage", description = "对象存储上传相关接口")
@Slf4j
public class ObjectStorageController {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private UserService userService;

    @Resource
    private ObjectStorageManager objectStorageManager;

    /**
     * 上传文件到对象存储
     *
     * @param file 文件
     * @return 上传文件的访问 URL
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传文件到对象存储，返回文件访问URL")
    public BaseResponse<String> uploadFile(
            @Parameter(description = "文件", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {
        boolean currentUserVip = userService.isCurrentUserVip();
        ThrowUtils.throwIf(!isIntranet && !currentUserVip, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署或会员可用");
        // 参数校验
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "请选择要上传的文件");
        // 检查对象存储服务是否可用
        ThrowUtils.throwIf(!objectStorageManager.isAvailable(), ErrorCode.SYSTEM_ERROR, "对象存储服务不可用");

        // 生成存储key：文件名+时间戳，如果读取不到文件名就只使用时间戳
        String originalFilename = file.getOriginalFilename();
        String key;
        if (originalFilename != null && !originalFilename.trim().isEmpty()) {
            // 去掉文件扩展名，添加时间戳，再加回扩展名
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0) {
                String nameWithoutExtension = originalFilename.substring(0, lastDotIndex);
                String extension = originalFilename.substring(lastDotIndex);
                key = nameWithoutExtension + "_" + System.currentTimeMillis() + extension;
            } else {
                key = originalFilename + "_" + System.currentTimeMillis();
            }
        } else {
            key = String.valueOf(System.currentTimeMillis());
        }

        // 执行上传
        boolean success = objectStorageManager.putObject(key, file.getInputStream(), file.getSize());
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "文件上传失败");

        // 获取访问URL
        String url = objectStorageManager.getUrl(key);

        log.info("文件上传成功: key={}, storageType={}", key, objectStorageManager.getStorageType());
        return success(url);
    }
}