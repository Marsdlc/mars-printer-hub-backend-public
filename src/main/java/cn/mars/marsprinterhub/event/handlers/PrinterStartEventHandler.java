package cn.mars.marsprinterhub.event.handlers;

import cn.hutool.core.collection.CollUtil;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.PrinterStartEvent;
import cn.mars.marsprinterhub.model.dto.bambu.AmsDetailMappingDTO;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskDTO;
import cn.mars.marsprinterhub.model.entity.AmsSlots;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.Spool;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.DeviceConfigItem;
import cn.mars.marsprinterhub.model.enums.LedNodeEnum;
import cn.mars.marsprinterhub.model.enums.PrinterModelEnum;
import cn.mars.marsprinterhub.notification.template.PrinterNotificationTemplate;
import cn.mars.marsprinterhub.processor.AutoSpoolProcessor;
import cn.mars.marsprinterhub.service.*;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_TASK_AMS_PREFIX;

/**
 * 任务封面事件处理器
 * 处理打印任务开始事件，保存任务封面
 */
@Slf4j
@Component
public class PrinterStartEventHandler implements EventHandler<PrinterStartEvent> {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private UserService userService;

    @Resource
    private SpoolService spoolService;

    @Resource
    private PrinterService printerService;

    @Resource
    private AmsService amsService;

    @Resource
    private BambuCloudService bambuCloudService;

    @Resource
    private BambuMqttService bambuMqttService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private EventBus eventBus;

    @Resource
    private PrinterNotificationTemplate notificationTemplate;

    @Resource
    private AutoSpoolProcessor autoSpoolProcessor;

    @PostConstruct
    public void init() {
        eventBus.register(PrinterStartEvent.class, this);
        log.info("打印开始事件处理器已注册");
    }

    @Override
    public void handle(PrinterStartEvent event) {
        try {
            String deviceId = event.getDeviceId();
            log.info("处理打印开始事件: deviceId={}", deviceId);

            // 启动任务记录
            autoSpoolProcessor.startTask(deviceId);

            // 删除该设备在 redis 中记录的 ams 使用情况
            redisUtils.delete(DEVICE_TASK_AMS_PREFIX + deviceId);

            if (!isIntranet) {
                return;
            }

            // 获取打印机信息
            Printer printer = printerService.getByDeviceId(deviceId);
            if (printer == null) {
                log.warn("未找到设备ID为 {} 的打印机", deviceId);
                return;
            }
            String model = printer.getModel();
            PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(model);
            // p1s处理关灯
            if (printerModelEnum == PrinterModelEnum.P1S &&
                    sysConfigService.isDeviceConfigEnabled(DeviceConfigItem.P1S_AUTO_OFF_LIGHT)) {
                bambuMqttService.turnOffChamberLight(deviceId, LedNodeEnum.CHAMBER_LIGHT.getValue());
            }

            // 获取设备所属的用户
            Long userId = printer.getUserId();
            User user = userService.getById(userId);

            // 获取设备最新打印任务
            BambuTaskDTO task = bambuCloudService.getLatestTaskForPrinter(user, deviceId, user.getBambuToken());
            List<AmsDetailMappingDTO> amsDetailMapping = task.getAmsDetailMapping();

            // 根据任务中的耗材信息来和槽位绑定的耗材进行匹配判断是否需要发送通知
            if (CollUtil.isEmpty(amsDetailMapping)) return;
            for (AmsDetailMappingDTO detailMappingDTO : amsDetailMapping) {
                checkFilamentBindingAndNotify(detailMappingDTO, printer);
            }
        } catch (Exception e) {
            log.error("处理打印开始事件失败: deviceId={}, error={}",
                    event.getDeviceId(), e.getMessage(), e);
        }
    }

    private void checkFilamentBindingAndNotify(AmsDetailMappingDTO detailMappingDTO, Printer printer) {
        // 1. 前置检查：detailMappingDTO 必须有 ams、filamentType、weight
        if (detailMappingDTO.getAms() == null
                || detailMappingDTO.getFilamentType() == null
                || detailMappingDTO.getWeight() == null) {
            return;
        }

        // 2. 查找 AMS 槽位
        AmsSlots amsSlots = amsService.getAmsSlotsByPrinterIdAndTrayNow(printer.getId(), printer.getModel(), detailMappingDTO.getAms());
        if (amsSlots == null) return;

        Long spoolId = amsSlots.getSpoolId();

        // 3. 判断是否绑定耗材
        if (spoolId == null) {
            notificationTemplate.sendFilamentNotBoundWarning(printer.getName(), detailMappingDTO.getAms());
            return;
        }

        // 4. 获取绑定耗材信息
        Spool spool = spoolService.getSpoolByIdNotLogin(spoolId);
        if (spool == null) {
            notificationTemplate.sendFilamentNotFoundWarning(printer.getName(), spoolId);
            return;
        }

        // 5. 校验耗材类型
        if (!detailMappingDTO.getFilamentType().equals(spool.getMaterialType())) {
            // 发送耗材类型不匹配通知
            notificationTemplate.sendFilamentTypeMismatchWarning(
                    printer.getName(),
                    detailMappingDTO.getAms(),
                    detailMappingDTO.getFilamentType(),
                    spool.getMaterialType(),
                    spool.getName()
            );
        }

        // 6. 校验耗材重量是否足够
        if (detailMappingDTO.getWeight() > spool.getNetWeight().subtract(spool.getUsedWeight()).doubleValue()) {
            log.info("耗材不足: ams={}, requiredWeight={}, availableWeight={}",
                    detailMappingDTO.getAms(),
                    detailMappingDTO.getWeight(),
                    spool.getNetWeight()
            );

            // 发送耗材不足通知
            notificationTemplate.sendFilamentWeightInsufficientWarning(
                    printer.getName(),
                    detailMappingDTO.getAms(),
                    spool.getName(),
                    detailMappingDTO.getWeight(),
                    spool.getNetWeight().subtract(spool.getUsedWeight()).doubleValue()
            );
        }
    }


    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }

    @Override
    public boolean isAsync() {
        return true; // 异步处理，不阻塞主流程
    }
}
