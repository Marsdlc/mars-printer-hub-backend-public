package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.common.ResultUtils;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 拓竹MQTT控制器
 * 提供MQTT连接管理和打印控制的REST API
 */
@Slf4j
@RestController
@RequestMapping("/bambu/mqtt")
@Tag(name = "BambuMqtt", description = "拓竹打印机MQTT连接和控制API")
public class BambuMqttController {

    @Resource
    private BambuMqttService bambuMqttService;

    @Resource
    private UserService userService;

    @Resource
    private PrinterService printerService;

    /**
     * 使用云服务信息连接打印机
     */
    @PostMapping("/connect/{deviceId}")
    @Operation(summary = "连接指定设备", description = "使用云服务信息连接指定的打印机设备")
    public BaseResponse<String> connect(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        printerService.getCurrentUserPrinterByDeviceId(deviceId);
        User currentUser = userService.getCurrentUser();
        boolean connected = bambuMqttService.doConnect(deviceId, currentUser);
        ThrowUtils.throwIf(!connected, ErrorCode.CONNECTION_FAILED);
        return ResultUtils.success("连接成功");
    }

    /**
     * 强制刷新
     */
    @PostMapping("/refresh/{deviceId}")
    @Operation(summary = "强制刷新", description = "发送refresh命令强制刷新设备状态")
    public BaseResponse<String> refresh(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        printerService.getCurrentUserPrinterByDeviceId(deviceId);
        bambuMqttService.refresh(deviceId);
        return ResultUtils.success("刷新成功");
    }
}
