package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.bambu.dto.BambuDeviceDTO;
import cn.mars.marsprinterhub.bambu.model.AmsStatus;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.common.ResultUtils;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCache;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/bambu/devices")
@Tag(name = "BambuDevice", description = "拓竹设备管理相关接口")
public class BambuDeviceController {

    @Resource
    private BambuCloudService bambuCloudService;
    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;
    @Resource
    private PrinterService printerService;

    /**
     * 获取设备列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取设备列表", description = "获取当前用户的所有拓竹打印机设备")
    public BaseResponse<List<BambuDeviceDTO>> getDevices() {
        List<BambuDeviceDTO> devices = bambuCloudService.getUserDevices();
        return ResultUtils.success(devices);
    }

    /**
     * 获取在线设备列表
     */
    @GetMapping("/online")
    @Operation(summary = "获取在线设备", description = "获取当前在线的拓竹打印机设备")
    public BaseResponse<List<BambuDeviceDTO>> getOnlineDevices() {
        List<BambuDeviceDTO> devices = bambuCloudService.getOnlineDevices();
        return ResultUtils.success(devices);
    }

    /**
     * 获取正在打印的设备列表
     */
    @GetMapping("/printing")
    @Operation(summary = "获取打印中设备", description = "获取正在打印的拓竹打印机设备")
    public BaseResponse<List<BambuDeviceDTO>> getPrintingDevices() {
        List<BambuDeviceDTO> devices = bambuCloudService.getPrintingDevices();
        return ResultUtils.success(devices);
    }

    /**
     * 根据设备ID获取设备信息
     */
    @GetMapping("/{deviceId}")
    @Operation(summary = "获取设备详情", description = "根据设备ID获取特定设备的详细信息")
    public BaseResponse<BambuDeviceDTO> getDevice(
            @Parameter(description = "设备ID", required = true) @PathVariable String deviceId) {

        BambuDeviceDTO device = bambuCloudService.getDeviceById(deviceId);
        ThrowUtils.throwIf(device == null, ErrorCode.SYSTEM_ERROR, "设备不存在");
        return ResultUtils.success(device);
    }

    /**
     * 获取设备访问码
     */
    @GetMapping("/{deviceId}/access-code")
    @Operation(summary = "获取设备访问码", description = "根据设备ID获取设备的访问码，用于本地MQTT连接")
    public BaseResponse<Map<String, String>> getDeviceAccessCode(
            @Parameter(description = "设备ID", required = true) @PathVariable String deviceId) {

        String accessCode = bambuCloudService.getDeviceAccessCode(deviceId);
        ThrowUtils.throwIf(accessCode == null, ErrorCode.SYSTEM_ERROR, "设备不存在或访问码为空");
        return ResultUtils.success(Map.of(
                "deviceId", deviceId,
                "accessCode", accessCode,
                "message", "获取访问码成功"
        ));
    }

    /**
     * 获取设备当前状态
     */
    @GetMapping("/{deviceId}/status")
    @Operation(summary = "获取设备当前状态", description = "获取指定设备的当前状态信息")
    public BaseResponse<DeviceStatusCache> getDeviceStatus(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        printerService.getCurrentUserPrinterByDeviceId(deviceId);
        DeviceStatusCache status = deviceStatusCacheService.getDeviceStatus(deviceId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备状态不存在或设备离线");
        }
        return ResultUtils.success(status);
    }

    /**
     * 获取设备最新状态
     */
    @GetMapping("/{deviceId}/status/latest")
    @Operation(summary = "获取设备最新状态", description = "获取指定设备的最新状态信息")
    public BaseResponse<DeviceStatusCache> getLatestDeviceStatus(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        printerService.getCurrentUserPrinterByDeviceId(deviceId);
        DeviceStatusCache status = deviceStatusCacheService.getLastStatus(deviceId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备最新状态不存在");
        }
        return ResultUtils.success(status);
    }

    /**
     * 获取打印进度信息
     */
    @GetMapping("/{deviceId}/print/progress")
    @Operation(summary = "获取打印进度", description = "获取指定设备的打印进度信息")
    public BaseResponse<Map<String, Object>> getPrintProgress(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        printerService.getCurrentUserPrinterByDeviceId(deviceId);
        DeviceStatusCache status = deviceStatusCacheService.getDeviceStatus(deviceId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备状态不存在");
        }

        Map<String, Object> progressInfo = new HashMap<>();
        progressInfo.put("deviceId", deviceId);
        progressInfo.put("progress", status.getPrintProgress());
        progressInfo.put("remainingTime", status.getRemainingTime());
        progressInfo.put("currentLayer", status.getCurrentLayer());
        progressInfo.put("totalLayers", status.getTotalLayers());
        progressInfo.put("printStatus", status.getStatusSummary());
        progressInfo.put("timestamp", status.getTimestamp());

        return ResultUtils.success(progressInfo);
    }

    /**
     * 获取打印状态详情
     */
    @GetMapping("/{deviceId}/print/status")
    @Operation(summary = "获取打印状态详情", description = "获取指定设备的详细打印状态")
    public BaseResponse<Map<String, Object>> getPrintStatus(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        printerService.getCurrentUserPrinterByDeviceId(deviceId);
        DeviceStatusCache status = deviceStatusCacheService.getDeviceStatus(deviceId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备状态不存在");
        }

        Map<String, Object> printInfo = new HashMap<>();
        printInfo.put("deviceId", deviceId);
        printInfo.put("isPrintStarted", status.isPrintStarted());
        printInfo.put("isPrintCompleted", status.isPrintFinished());
        printInfo.put("isPrintPaused", status.isPrintPaused());
        printInfo.put("isPrintFailed", status.isPrintFailed());
        printInfo.put("printProgress", status.getPrintProgress());
        printInfo.put("remainingTime", status.getRemainingTime());
        printInfo.put("currentLayer", status.getCurrentLayer());
        printInfo.put("totalLayers", status.getTotalLayers());
        printInfo.put("statusSummary", status.getStatusSummary());
        printInfo.put("timestamp", status.getTimestamp());

        return ResultUtils.success(printInfo);
    }

    /**
     * 获取AMS状态
     */
    @GetMapping("/{deviceId}/ams/status")
    @Operation(summary = "获取AMS状态", description = "获取指定设备的AMS状态信息")
    public BaseResponse<AmsStatus> getAmsStatus(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        printerService.getCurrentUserPrinterByDeviceId(deviceId);
        DeviceStatusCache status = deviceStatusCacheService.getDeviceStatus(deviceId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备状态不存在");
        }

        AmsStatus amsStatus = status.getAmsStatus();
        if (amsStatus == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "AMS状态信息不可用");
        }
        return ResultUtils.success(amsStatus);
    }

    /**
     * 检查设备是否在线
     */
    @GetMapping("/{deviceId}/online")
    @Operation(summary = "检查设备在线状态", description = "检查指定设备是否在线")
    public BaseResponse<Map<String, Object>> isDeviceOnline(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        List<BambuDeviceDTO> onlineDevices = bambuCloudService.getOnlineDevices();

        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("isOnline", onlineDevices.stream().anyMatch(device -> device.getDeviceId().equals(deviceId)));
        result.put("timestamp", LocalDateTime.now());

        return ResultUtils.success(result);
    }

    /**
     * 清除设备缓存
     */
    @DeleteMapping("/{deviceId}/cache")
    @Operation(summary = "清除设备缓存", description = "清除指定设备的缓存数据")
    public BaseResponse<String> clearDeviceCache(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        printerService.getCurrentUserPrinterByDeviceId(deviceId);
        deviceStatusCacheService.deleteDeviceStatus(deviceId);
        return ResultUtils.success("设备缓存清除成功");
    }
}
