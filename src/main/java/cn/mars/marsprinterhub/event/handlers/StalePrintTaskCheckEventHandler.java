package cn.mars.marsprinterhub.event.handlers;

import cn.hutool.core.collection.CollUtil;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.StalePrintTaskCheckEvent;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskDTO;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.SpoolUsedRecord;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.TaskStatusEnum;
import cn.mars.marsprinterhub.processor.AutoSpoolProcessor;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.SpoolUsedRecordService;
import cn.mars.marsprinterhub.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 旧打印任务兜底检查事件处理器
 */
@Slf4j
@Component
public class StalePrintTaskCheckEventHandler implements EventHandler<StalePrintTaskCheckEvent> {

    @Resource
    private EventBus eventBus;

    @Resource
    private PrinterService printerService;

    @Resource
    private UserService userService;

    @Resource
    private SpoolUsedRecordService spoolUsedRecordService;

    @Resource
    private BambuCloudService bambuCloudService;

    @Resource
    private AutoSpoolProcessor autoSpoolProcessor;

    @PostConstruct
    public void init() {
        eventBus.register(StalePrintTaskCheckEvent.class, this);
        log.info("旧打印任务兜底检查事件处理器已注册");
    }

    @Override
    public void handle(StalePrintTaskCheckEvent event) {
        String deviceId = event.getDeviceId();
        try {
            Printer printer = printerService.getByDeviceId(deviceId);
            if (printer == null) {
                return;
            }
            User user = userService.getById(printer.getUserId());
            if (user == null || user.getBambuToken() == null) {
                return;
            }

            // 旧任务兜底是低优先级补偿逻辑，只处理该设备上仍残留为 PRINTING 的旧记录。
            List<SpoolUsedRecord> runningRecords = spoolUsedRecordService.lambdaQuery()
                    .eq(SpoolUsedRecord::getDeviceId, deviceId)
                    .eq(SpoolUsedRecord::getStatus, TaskStatusEnum.PRINTING.getValue())
                    .isNotNull(SpoolUsedRecord::getTaskId)
                    .list();
            if (CollUtil.isEmpty(runningRecords)) {
                return;
            }

            Map<Long, List<SpoolUsedRecord>> recordsByTaskId = runningRecords.stream()
                    .collect(Collectors.groupingBy(SpoolUsedRecord::getTaskId));
            for (Map.Entry<Long, List<SpoolUsedRecord>> entry : recordsByTaskId.entrySet()) {
                Long staleTaskId = entry.getKey();
                if (staleTaskId == null) {
                    continue;
                }

                BambuTaskDTO task = bambuCloudService.getTaskById(user, staleTaskId, user.getBambuToken());
                if (task == null) {
                    log.warn("兜底收尾旧任务失败，未获取到任务详情: deviceId={}, taskId={}", deviceId, staleTaskId);
                    continue;
                }

                Integer taskStatus = task.getStatus();
                if (Objects.equals(taskStatus, TaskStatusEnum.PRINTING.getValue())) {
                    log.info("旧任务仍处于打印中，跳过兜底收尾: deviceId={}, taskId={}", deviceId, staleTaskId);
                    continue;
                }

                // 旧任务保证能拿到 AMS 明细，直接复用现有任务收尾逻辑；
                // 即使这里失败，也只影响补偿本身，不影响已经开始的新任务。
                int fallbackPercent = Objects.equals(taskStatus, TaskStatusEnum.SUCCESS.getValue()) ? 100 : 0;
                log.info("开始异步兜底收尾旧任务: deviceId={}, taskId={}, status={}", deviceId, staleTaskId, taskStatus);
                autoSpoolProcessor.execute(deviceId, fallbackPercent, task);
            }
        } catch (Exception e) {
            log.error("处理旧打印任务兜底检查事件失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
