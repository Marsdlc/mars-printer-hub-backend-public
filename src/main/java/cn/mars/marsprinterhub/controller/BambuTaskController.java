package cn.mars.marsprinterhub.controller;

import cn.hutool.core.collection.CollUtil;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.common.ResultUtils;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.dto.bambu.AmsDetailMappingDTO;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskDTO;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskListResponseDTO;
import cn.mars.marsprinterhub.model.entity.SpoolUsedRecord;
import cn.mars.marsprinterhub.model.enums.TaskStatusEnum;
import cn.mars.marsprinterhub.processor.AutoSpoolProcessor;
import cn.mars.marsprinterhub.processor.DeviceTaskProcessor;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.SpoolUsedRecordService;
import cn.mars.marsprinterhub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 拓竹云任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/bambu/tasks")
@Tag(name = "BambuTask", description = "拓竹云任务相关接口")
public class BambuTaskController {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private UserService userService;

    @Resource
    private BambuCloudService bambuCloudService;

    @Resource
    private AutoSpoolProcessor autoSpoolProcessor;

    @Resource
    private DeviceTaskProcessor deviceTaskProcessor;

    @Resource
    private SpoolUsedRecordService spoolUsedRecordService;

    @Resource
    private PrinterService printerService;

    /**
     * 获取任务列表
     */
    @GetMapping
    @Operation(summary = "获取任务列表", description = "获取当前用户的拓竹云任务列表")
    public BaseResponse<BambuTaskListResponseDTO> getTaskList(
            @Parameter(description = "偏移量，从第几条开始") @RequestParam(defaultValue = "0") Integer offset,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(description = "开始时间戳（秒），0表示不限制") @RequestParam(defaultValue = "0") Long startTime,
            @Parameter(description = "结束时间戳（秒），0表示不限制") @RequestParam(defaultValue = "0") Long endTime,
            @Parameter(description = "状态列表") @RequestParam(required = false) String statusList,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String query) {
        boolean currentUserVip = userService.isCurrentUserVip();
        ThrowUtils.throwIf(!isIntranet && !currentUserVip, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署或会员可用");
        BambuTaskListResponseDTO taskList = bambuCloudService.getTaskList(offset, limit, startTime, endTime, statusList, query);
        return ResultUtils.success(taskList);
    }

    /**
     * 获取指定设备的最新任务
     */
    @GetMapping("/latest/{deviceId}")
    @Operation(summary = "获取设备最新任务", description = "获取指定设备的最新打印任务")
    public BaseResponse<BambuTaskDTO> getLatest(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        boolean currentUserVip = userService.isCurrentUserVip();
        ThrowUtils.throwIf(!isIntranet && !currentUserVip, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署或会员可用");
        BambuTaskDTO latestTask = bambuCloudService.getLatestTaskForPrinter(deviceId, null);
        return ResultUtils.success(latestTask);
    }

    /**
     * 手动处理任务
     */
    @PostMapping("/handle/{deviceId}")
    @Operation(summary = "手动处理任务", description = "手动记录指定设备的打印任务")
    public BaseResponse<Boolean> handleTask(
            @Parameter(description = "设备 ID", required = true)
            @PathVariable String deviceId,
            @Parameter(description = "任务信息", required = true)
            @RequestBody BambuTaskDTO taskDTO) {
        boolean currentUserVip = userService.isCurrentUserVip();
        ThrowUtils.throwIf(!isIntranet && !currentUserVip, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署或会员可用");
        printerService.getCurrentUserPrinterByDeviceId(deviceId);

        // 判断任务是否已经处理过
        Long taskId = taskDTO == null ? null : taskDTO.getId();
        ThrowUtils.throwIf(taskId == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        BambuTaskDTO verifiedTask = bambuCloudService.getTaskById(taskId, null);
        ThrowUtils.throwIf(verifiedTask == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        ThrowUtils.throwIf(!Objects.equals(deviceId, verifiedTask.getDeviceId()),
                ErrorCode.NO_AUTH_ERROR, "任务不属于该设备");

        log.info("手动处理任务，设备ID: {}, 任务ID: {}", deviceId, taskId);
        Long currentUserId = userService.getCurrentUserId();
        List<AmsDetailMappingDTO> amsDetailMapping = verifiedTask.getAmsDetailMapping();
        if (CollUtil.isNotEmpty(amsDetailMapping)) {
            amsDetailMapping.forEach(amsDetail -> {
                Integer ams = amsDetail.getAms();
                if (ams == null) return;
                SpoolUsedRecord record = spoolUsedRecordService.getByTaskIdAndAmsRef(taskId, ams);
                if (record != null) {
                    ThrowUtils.throwIf(!Objects.equals(currentUserId, record.getUserId()),
                            ErrorCode.NO_AUTH_ERROR, "无权操作该任务记录");
                    Integer status = record.getStatus();
                    if (status != null && (status.equals(TaskStatusEnum.SUCCESS.getValue()) || status.equals(TaskStatusEnum.CANCEL.getValue()))) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务已经处理过，请勿重复操作");
                    }
                }
            });
        }
        String cover = verifiedTask.getCover();
        deviceTaskProcessor.saveTaskCoverUrl(deviceId, cover);
        autoSpoolProcessor.execute(deviceId, 100, verifiedTask);
        return ResultUtils.success(true);
    }
}
