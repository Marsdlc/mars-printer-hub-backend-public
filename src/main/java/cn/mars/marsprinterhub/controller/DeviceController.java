package cn.mars.marsprinterhub.controller;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.common.ResultUtils;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.dto.ams.AmsBindRequest;
import cn.mars.marsprinterhub.model.dto.ams.AmsEntityUpdateDTO;
import cn.mars.marsprinterhub.model.entity.Ams;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.vo.DeviceCompleteVO;
import cn.mars.marsprinterhub.model.vo.DeviceSimpleVO;
import cn.mars.marsprinterhub.model.vo.DeviceStreamVO;
import cn.mars.marsprinterhub.model.vo.HomeAssistantEntityStateVO;
import cn.mars.marsprinterhub.service.AccessControlService;
import cn.mars.marsprinterhub.service.AmsService;
import cn.mars.marsprinterhub.service.DeviceService;
import cn.mars.marsprinterhub.service.SysConfigService;
import cn.mars.marsprinterhub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 设备接口
 *
 * @author mars
 */
@RestController
@RequestMapping("/device")
@Slf4j
@Tag(name = "Device", description = "设备相关接口")
public class DeviceController {

    @Resource
    private DeviceService deviceService;

    @Resource
    private UserService userService;

    @Resource
    private AmsService amsService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private AccessControlService accessControlService;

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Operation(summary = "获取所有打印机")
    @GetMapping("/list")
    public BaseResponse<List<DeviceSimpleVO>> listPrinters() {
        List<Printer> deviceList = deviceService.getDeviceList();
        List<DeviceSimpleVO> deviceSimpleVOList = deviceList.stream()
                .map(deviceService::convertToVO)
                .toList();
        return success(deviceSimpleVOList);
    }

    /**
     * 获取打印机完整信息
     * 包含打印机基本信息、状态信息和AMS信息
     *
     * @param deviceId 设备 ID
     * @return 打印机完整信息
     */
    @Operation(summary = "获取打印机完整信息", description = "一次性获取打印机基本信息、状态信息和AMS信息")
    @GetMapping("/{deviceId}/complete")
    public BaseResponse<DeviceCompleteVO> getCompleteInfo(
            @Parameter(description = "设备 ID", required = true)
            @PathVariable Long deviceId) {

        // 参数校验
        ThrowUtils.throwIf(deviceId == null, ErrorCode.PARAMS_ERROR, "设备 ID 不能为空");

        // 获取完整信息
        DeviceCompleteVO completeInfo = deviceService.getDeviceCompleteInfo(deviceId);

        return success(completeInfo);
    }

    /**
     * 绑定槽位和耗材
     * 如果槽位已绑定其他耗材，会自动覆盖原有绑定
     *
     * @param request 绑定请求
     * @return 操作结果
     */
    @Operation(summary = "绑定槽位和耗材")
    @PostMapping("/bind")
    public BaseResponse<Boolean> bindSlotSpool(@Valid @RequestBody AmsBindRequest request) {
        boolean result = deviceService.bindSlotSpool(request);
        return success(result);
    }

    @Operation(summary = "更新 AMS 温湿度实体ID")
    @PutMapping("/ams/entity")
    public BaseResponse<Boolean> updateAmsEntities(@Valid @RequestBody AmsEntityUpdateDTO request) {
        Long currentUserId = userService.getCurrentUserId();
        Ams ams = amsService.getById(request.getAmsId());
        ThrowUtils.throwIf(ams == null, ErrorCode.NOT_FOUND_ERROR, "AMS 不存在");
        ThrowUtils.throwIf(!currentUserId.equals(ams.getUserId()), ErrorCode.NO_AUTH_ERROR);
        return success(deviceService.updateAmsEntities(request));
    }

    /**
     * 切换主打印腔灯
     */
    @PostMapping("/{deviceId}/light/chamber")
    @Operation(summary = "切换主打印腔灯", description = "通过 MQTT 命令切换设备的主打印腔灯")
    public BaseResponse<String> switchChamberLight(
            @Parameter(description = "设备 ID") @PathVariable String deviceId) {
        ThrowUtils.throwIf(!isIntranet, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署可用");
        deviceService.switchChamberLight(deviceId);
        return ResultUtils.success("主打印腔灯切换命令发送成功");
    }

    /**
     * 切换主打印腔灯2
     */
    @PostMapping("/{deviceId}/light/chamber2")
    @Operation(summary = "切换主打印腔灯2", description = "通过MQTT命令切换设备的主打印腔灯2")
    public BaseResponse<String> switchChamberLight2(
            @Parameter(description = "设备 ID") @PathVariable String deviceId) {
        ThrowUtils.throwIf(!isIntranet, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署可用");
        deviceService.switchChamberLight2(deviceId);
        return ResultUtils.success("主打印腔灯切换命令发送成功");
    }

    // ==================== 流媒体相关接口 ====================

    @GetMapping("/stream/content")
    @Operation(summary = "获取设备流内容", description = "统一流内容入口，当前用于 MJPEG 输出")
    public ResponseEntity<StreamingResponseBody> getStreamContent(
            @Parameter(description = "设备 ID", required = true) @RequestParam String deviceId) {
        ThrowUtils.throwIf(StrUtil.isBlank(deviceId), ErrorCode.PARAMS_ERROR, "设备 ID 不能为空");
        ThrowUtils.throwIf(!isIntranet, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署可用");
        return deviceService.getStreamContent(deviceId);
    }

    @GetMapping("/stream")
    @Operation(summary = "获取设备统一流信息", description = "按设备能力返回 HLS 或 MJPEG 播放地址")
    public BaseResponse<DeviceStreamVO> getStreamInfo(@RequestParam String deviceId) {
        ThrowUtils.throwIf(StrUtil.isBlank(deviceId), ErrorCode.PARAMS_ERROR, "设备 ID 不能为空");
        ThrowUtils.throwIf(!isIntranet, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署可用");
        return ResultUtils.success(deviceService.getStreamInfo(deviceId));
    }

    @PostMapping("/stream/disconnect/{deviceId}")
    @Operation(summary = "断开设备统一流", description = "支持 auto/hls/mjpeg/all，默认 auto")
    public BaseResponse<Boolean> disconnectStream(
            @Parameter(description = "设备 ID", required = true) @PathVariable String deviceId,
            @Parameter(description = "断流类型：auto/hls/mjpeg/all")
            @RequestParam(required = false, defaultValue = "auto") String type) {
        ThrowUtils.throwIf(StrUtil.isBlank(deviceId), ErrorCode.PARAMS_ERROR, "设备 ID 不能为空");
        ThrowUtils.throwIf(!isIntranet, ErrorCode.SYSTEM_ERROR, "该功能仅内网部署可用");
        return ResultUtils.success(deviceService.disconnectStream(deviceId, type));
    }

    // ==================== 设备配置 ====================

    @Operation(summary = "获取所有设备配置")
    @GetMapping("/device")
    public BaseResponse<List<SysConfigService.ConfigItemVO>> listDeviceConfigs() {
        return success(sysConfigService.getAllDeviceConfigs());
    }

    @Operation(summary = "更新设备配置")
    @PutMapping("/device")
    public BaseResponse<Boolean> updateDeviceSettings(@RequestBody Map<String, Boolean> settings) {
        accessControlService.checkAdmin();
        sysConfigService.updateDeviceSettings(settings);
        return success(true);
    }

    // ==================== 远程配置 URL ====================

    @Operation(summary = "获取远程配置 URL", description = "返回 colorUrl, filamentsUrl, spoolUrl")
    @GetMapping("/remote-config")
    public BaseResponse<Map<String, String>> getRemoteConfigUrls() {
        return success(sysConfigService.getRemoteConfigUrls());
    }

    @Operation(summary = "更新远程配置 URL")
    @PutMapping("/remote-config")
    public BaseResponse<Boolean> updateRemoteConfigUrls(@RequestBody Map<String, String> urls) {
        accessControlService.checkAdmin();
        sysConfigService.updateRemoteConfigUrls(urls);
        return success(true);
    }

    @Operation(summary = "调试获取 Home Assistant 实体状态", description = "根据实体 ID 调试查询 Home Assistant 状态，用于配置实体 ID 时测试连通性")
    @GetMapping("/home-assistant/entity")
    public BaseResponse<HomeAssistantEntityStateVO> getHomeAssistantEntityState(
            @Parameter(description = "Home Assistant 实体 ID", required = true)
            @RequestParam String entityId) {
        ThrowUtils.throwIf(StrUtil.isBlank(entityId), ErrorCode.PARAMS_ERROR, "实体 ID 不能为空");
        return success(deviceService.getHomeAssistantEntityState(entityId));
    }

    @Operation(summary = "根据打印机获取功率信息", description = "根据打印机 ID 查询已绑定的功耗实体状态")
    @GetMapping("/power/printer")
    public BaseResponse<HomeAssistantEntityStateVO> getPrinterPower(
            @Parameter(description = "打印机 ID", required = true)
            @RequestParam Long printerId) {
        ThrowUtils.throwIf(printerId == null, ErrorCode.PARAMS_ERROR, "打印机 ID 不能为空");
        return success(deviceService.getPrinterPower(printerId));
    }

}
