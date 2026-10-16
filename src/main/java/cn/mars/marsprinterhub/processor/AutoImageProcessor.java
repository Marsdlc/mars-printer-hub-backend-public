package cn.mars.marsprinterhub.processor;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.bambu.service.BambuImageService;
import cn.mars.marsprinterhub.manager.ObjectStorageManager;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.enums.DeviceConfigItem;
import cn.mars.marsprinterhub.model.enums.LedNodeEnum;
import cn.mars.marsprinterhub.model.enums.PrinterModelEnum;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.SysConfigService;
import cn.mars.marsprinterhub.utils.ImageConvertUtils;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_IMAGES_PREFIX;
import static cn.mars.marsprinterhub.utils.DateUtils.formatTimestamp;

@Service
@Slf4j
public class AutoImageProcessor {

    private static final List<PrinterModelEnum> PRINTER_MODEL = List.of(
            PrinterModelEnum.P2S,
            PrinterModelEnum.X1C,
            PrinterModelEnum.X2D,
            PrinterModelEnum.H2D,
            PrinterModelEnum.H2C,
            PrinterModelEnum.H2S
    );
    @Resource
    private RedisUtils redisUtils;
    @Autowired(required = false)
    private ObjectStorageManager objectStorageManager;
    @Resource
    private BambuMqttService bambuMqttService;
    @Resource
    private BambuImageService bambuImageService;
    @Resource
    private PrinterService printerService;
    @Resource
    private SysConfigService sysConfigService;

    public void execute(String deviceId, Integer mcPercent) {
        try {
            log.info("开始处理自动图片处理任务");

            // 上传图片
            if (objectStorageManager == null || !objectStorageManager.isAvailable()) {
                log.error("对象存储未配置或不可用，无法上传图片");
                return;
            }

            String key = DEVICE_IMAGES_PREFIX + deviceId;
            String field = String.valueOf(mcPercent);

            // 如果已经有该进度点，就直接返回
            if (redisUtils.hExists(key, field)) {
                return;
            }

            String timestamp = formatTimestamp(LocalDateTime.now());
            String ossKey = timestamp + "_" + mcPercent + ".webp";

            // 获取打印机信息
            Printer printer = printerService.getByDeviceId(deviceId);
            if (printer == null) {
                log.error("打印机信息不存在");
                return;
            }
            if (StrUtil.hasBlank(printer.getIpAddress(), printer.getAccessCode())) {
                log.error("打印机信息不完整");
                return;
            }

            boolean uploadResult;

            // 根据打印机型号来获取截图
            String model = printer.getModel();
            PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(model);
            if (PRINTER_MODEL.contains(printerModelEnum)) {
                uploadResult = uploadSnapshotByH2D(printer.getIpAddress(), printer.getAccessCode(), ossKey);
            } else {
                uploadResult = uploadSnapshot(deviceId, printer.getIpAddress(), printer.getAccessCode(), ossKey);
            }

            if (!uploadResult) {
                log.error("图片上传失败");
                return;
            }
            String url = objectStorageManager.getUrl(ossKey);

            // 存入 redis
            redisUtils.hSet(key, field, url);
        } catch (Exception e) {
            // 不影响流程，只记录错误日志
            log.error("自动图片处理任务出错", e);
        }
    }

    private boolean uploadSnapshot(String deviceId, String ipAddress, String accessCode, String ossKey) {
        boolean deviceConfigEnabled = sysConfigService.isDeviceConfigEnabled(DeviceConfigItem.P1S_AUTO_OFF_LIGHT);
        if (deviceConfigEnabled) {
            // 先开灯
            bambuMqttService.turnOnChamberLight(deviceId, LedNodeEnum.CHAMBER_LIGHT.getValue());
        }
        // 延迟 5s
        bambuMqttService.waitStabilization(5000);
        // 获取图片
        byte[] image = bambuImageService.captureImage(ipAddress, accessCode);
        if (image == null) {
            log.error("获取图片失败");
            return false;
        }
        if (deviceConfigEnabled) {
            // 关灯
            bambuMqttService.turnOffChamberLight(deviceId, LedNodeEnum.CHAMBER_LIGHT.getValue());
        }
        File tempImageFile = null;
        File webpFile = null;
        try {
            tempImageFile = ImageConvertUtils.writeBytesToTempFile(image, ".jpg");
            webpFile = ImageConvertUtils.convertToWebp(tempImageFile);
            if (webpFile == null) {
                log.error("jpeg 转 webp 失败");
                return false;
            }
            return objectStorageManager.putObject(ossKey, webpFile);
        } finally {
            ImageConvertUtils.deleteQuietly(tempImageFile);
            ImageConvertUtils.deleteQuietly(webpFile);
        }
    }

    private boolean uploadSnapshotByH2D(String ipAddress, String accessCode, String ossKey) {
        File snapshotFile = null;
        File webpFile = null;
        try {
            snapshotFile = bambuImageService.captureImageByH2D(ipAddress, accessCode);
            if (snapshotFile == null) {
                log.error("抓取截图失败, IP: {}", ipAddress);
                return false;
            }
            webpFile = ImageConvertUtils.convertToWebp(snapshotFile);
            if (webpFile == null) {
                log.error("png 转 webp 失败, IP: {}", ipAddress);
                return false;
            }
            return objectStorageManager.putObject(ossKey, webpFile);
        } finally {
            // 确保临时文件删除
            ImageConvertUtils.deleteQuietly(snapshotFile);
            ImageConvertUtils.deleteQuietly(webpFile);
        }
    }
}
