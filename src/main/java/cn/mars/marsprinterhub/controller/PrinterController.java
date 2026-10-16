package cn.mars.marsprinterhub.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.PrinterRefreshEvent;
import cn.mars.marsprinterhub.event.events.PrinterUnbindEvent;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.dto.printer.*;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.vo.PrinterOverviewVO;
import cn.mars.marsprinterhub.model.vo.PrinterVO;
import cn.mars.marsprinterhub.service.DeviceService;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 打印机接口
 *
 * @author mars
 */
@RestController
@RequestMapping("/printer")
@Slf4j
@Tag(name = "Printer", description = "打印机相关接口")
public class PrinterController {

    private static final int VIP_MAX_MQTT_CONNECTIONS = 10;

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private UserService userService;

    @Resource
    private PrinterService printerService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private BambuMqttService bambuMqttService;

    @Resource
    private EventPublisher eventPublisher;

    @PostMapping("/sync")
    @Operation(summary = "从拓竹 API 同步所有打印机")
    public BaseResponse<Boolean> syncPrinters() {
        User currentUser = userService.getCurrentUser();
        boolean result = deviceService.syncPrintersFromBambu();
        String message = buildSyncPrintersMessage(currentUser);
        return success(result, message);
    }

    private String buildSyncPrintersMessage(User currentUser) {
        Long userId = currentUser.getId();
        List<Printer> printers = printerService.getListByUserId(userId);
        Set<String> connectedDeviceIds = bambuMqttService.getUserConnectedDevices(userId);
        List<String> connectedPrinterNames = printers.stream()
                .filter(printer -> connectedDeviceIds.contains(printer.getDeviceId()))
                .map(this::getPrinterDisplayName)
                .toList();

        if (isIntranet) {
            return connectedPrinterNames.isEmpty()
                    ? "未同步成功"
                    : "同步成功：" + String.join("、", connectedPrinterNames);
        }

        if (!currentUser.isVip()) {
            if (printers.size() <= 1) {
                return connectedPrinterNames.isEmpty() ? "未同步成功" : "同步成功";
            }
            return connectedPrinterNames.isEmpty()
                    ? "未同步成功。普通用户最多同时同步1台设备，可开通VIP获取更多设备同时同步"
                    : "同步成功：" + String.join("、", connectedPrinterNames)
                    + "。普通用户最多同时同步1台设备，可开通VIP获取更多设备同时同步";
        }

        if (printers.size() <= VIP_MAX_MQTT_CONNECTIONS) {
            return connectedPrinterNames.isEmpty() ? "未同步成功" : "同步成功";
        }
        return connectedPrinterNames.isEmpty()
                ? "未同步成功。设备数量已超过VIP同时同步上限，可联系作者选择内网部署"
                : "同步成功：" + String.join("、", connectedPrinterNames)
                + "。设备数量已超过VIP同时同步上限，可联系作者选择内网部署";
    }

    private String getPrinterDisplayName(Printer printer) {
        if (StrUtil.isNotBlank(printer.getName())) {
            return printer.getName();
        }
        return printer.getDeviceId();
    }

    @GetMapping("/overview")
    @Operation(summary = "获取打印机概览列表（带实时状态）")
    public BaseResponse<List<PrinterOverviewVO>> getOverview() {
        return success(printerService.getOverviewList());
    }

    @PostMapping("/unbind")
    @Operation(summary = "解绑打印机")
    public BaseResponse<Boolean> unbindPrinters(@Valid @RequestBody PrinterUnbindDTO printerUnbindDTO) {
        getOwnedPrinter(printerUnbindDTO.getPrinterId());
        eventPublisher.publish(new PrinterUnbindEvent(printerUnbindDTO.getPrinterId()));
        return success(true);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新打印机")
    public BaseResponse<Boolean> refreshPrinters(@Valid @RequestBody PrinterRefreshDTO printerRefreshDTO) {
        getOwnedPrinter(printerRefreshDTO.getPrinterId());
        eventPublisher.publish(new PrinterRefreshEvent(printerRefreshDTO.getPrinterId()));
        return success(true);
    }

    @PostMapping("/sort")
    @Operation(summary = "批量更新打印机排序")
    public BaseResponse<Boolean> updatePrinterSort(@RequestBody List<PrinterSortUpdateDTO> printerSortList) {
        printerService.updateSort(printerSortList);
        return success(true);
    }

    @PutMapping("/location")
    @Operation(summary = "更新打印机位置")
    public BaseResponse<Boolean> updatePrinterLocation(@Valid @RequestBody PrinterLocationUpdateDTO locationUpdateDTO) {
        getOwnedPrinter(locationUpdateDTO.getPrinterId());
        printerService.updateLocation(locationUpdateDTO.getPrinterId(), locationUpdateDTO.getLocation());
        return success(true);
    }

    @PutMapping("/power-entity")
    @Operation(summary = "更新打印机功耗实体 ID")
    public BaseResponse<Boolean> updatePrinterPowerEntity(@Valid @RequestBody PrinterPowerEntityUpdateDTO powerEntityUpdateDTO) {
        getOwnedPrinter(powerEntityUpdateDTO.getPrinterId());
        printerService.updatePowerEntityId(powerEntityUpdateDTO.getPrinterId(), powerEntityUpdateDTO.getPowerEntityId());
        return success(true);
    }

    @PutMapping("/chamber-temp-entity")
    @Operation(summary = "更新打印机仓温实体 ID")
    public BaseResponse<Boolean> updatePrinterChamberTempEntity(@Valid @RequestBody PrinterChamberTempEntityUpdateDTO chamberTempEntityUpdateDTO) {
        getOwnedPrinter(chamberTempEntityUpdateDTO.getPrinterId());
        printerService.updateChamberTempEntityId(chamberTempEntityUpdateDTO.getPrinterId(), chamberTempEntityUpdateDTO.getChamberTempEntityId());
        return success(true);
    }

    @PutMapping("/custom-camera-rtsp-url")
    @Operation(summary = "更新打印机自定义摄像头 RTSP 路径")
    public BaseResponse<Boolean> updatePrinterCustomCameraRtspUrl(@Valid @RequestBody PrinterCustomCameraRtspUrlUpdateDTO customCameraRtspUrlUpdateDTO) {
        getOwnedPrinter(customCameraRtspUrlUpdateDTO.getPrinterId());
        printerService.updateCustomCameraRtspUrl(customCameraRtspUrlUpdateDTO.getPrinterId(), customCameraRtspUrlUpdateDTO.getCustomCameraRtspUrl());
        return success(true);
    }

    @Operation(summary = "获取打印机详细信息")
    @GetMapping("/{id}")
    public BaseResponse<PrinterVO> getPrinter(
            @Parameter(description = "打印机 ID", required = true)
            @PathVariable Long id
    ) {
        Printer printer = getOwnedPrinter(id);
        return success(printerService.convertToVO(printer));
    }

    /**
     * 获取当前用户拥有的打印机，不存在或无权限时抛出异常
     */
    private Printer getOwnedPrinter(Long printerId) {
        Printer printer = printerService.getById(printerId);
        ThrowUtils.throwIf(printer == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!ObjUtil.equals(userService.getCurrentUserId(), printer.getUserId()),
                ErrorCode.NO_AUTH_ERROR);
        return printer;
    }

    @Operation(summary = "分页获取打印机列表")
    @GetMapping("/list/page")
    public BaseResponse<Page<PrinterVO>> listPagePrinters(
            @Valid @ParameterObject PrinterQueryDTO queryDTO) {

        long current = queryDTO.getCurrent();
        long pageSize = queryDTO.getSize();

        IPage<Printer> printerPage = printerService.getPage(current, pageSize, queryDTO);

        // 构建返回结果
        Page<PrinterVO> printerVOPage = new Page<>(current, pageSize, printerPage.getTotal());
        List<PrinterVO> printerVOList = printerPage.getRecords().stream()
                .map(printerService::convertToVO)
                .toList();
        printerVOPage.setRecords(printerVOList);
        return success(printerVOPage);
    }
}
