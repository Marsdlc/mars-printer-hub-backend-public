package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.model.dto.NotificationConfigDTO;
import cn.mars.marsprinterhub.notification.NotificationFactory;
import cn.mars.marsprinterhub.notification.NotificationResult;
import cn.mars.marsprinterhub.service.AccessControlService;
import cn.mars.marsprinterhub.service.NotificationConfigService;
import cn.mars.marsprinterhub.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 通知配置控制器
 *
 * @author mars
 */
@RestController
@RequestMapping("/notification/config")
@Tag(name = "NotificationConfig", description = "通知配置管理")
public class NotificationConfigController {

    @Resource
    private NotificationConfigService notificationConfigService;

    @Resource
    private NotificationFactory notificationFactory;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private AccessControlService accessControlService;

    @Operation(summary = "获取所有通知配置")
    @GetMapping("/list")
    public BaseResponse<List<NotificationConfigDTO>> listConfigs() {
        accessControlService.checkAdminIntranet();
        return success(notificationConfigService.getAllConfigs());
    }

    @Operation(summary = "更新指定渠道配置")
    @PutMapping("/{channel}")
    public BaseResponse<Boolean> updateConfig(@PathVariable String channel, @RequestBody Object config) {
        accessControlService.checkAdminIntranet();
        notificationConfigService.updateConfig(channel, config);
        notificationFactory.refresh();
        return success(true);
    }

    @Operation(summary = "设置渠道启用状态")
    @PatchMapping("/{channel}/status")
    public BaseResponse<Boolean> setEnabled(@PathVariable String channel, @RequestParam Boolean enabled) {
        accessControlService.checkAdminIntranet();
        if (enabled == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "enabled parameter is required");
        }
        notificationConfigService.setEnabled(channel, enabled);
        notificationFactory.refresh();
        return success(true);
    }

    @Operation(summary = "更新渠道排序")
    @PutMapping("/sort")
    public BaseResponse<Boolean> updateSort(@RequestBody List<String> sortedChannels) {
        accessControlService.checkAdminIntranet();
        notificationConfigService.updateSort(sortedChannels);
        notificationFactory.refresh();
        return success(true);
    }

    @Operation(summary = "测试配置")
    @PostMapping("/{channel}/test")
    public BaseResponse<NotificationResult> testConfig(@PathVariable String channel, @RequestBody Object config) {
        accessControlService.checkAdminIntranet();
        NotificationResult result = notificationConfigService.testConfig(channel, config);
        return success(result);
    }

    // ==================== 模板开关配置 ====================

    @Operation(summary = "获取所有通知模板配置")
    @GetMapping("/templates")
    public BaseResponse<List<SysConfigService.ConfigItemVO>> listTemplateConfigs() {
        accessControlService.checkAdminIntranet();
        return success(sysConfigService.getAllTemplateConfigs());
    }

    @Operation(summary = "更新通知模板开关设置")
    @PutMapping("/templates")
    public BaseResponse<Boolean> updateTemplateSettings(@RequestBody Map<String, Boolean> settings) {
        accessControlService.checkAdminIntranet();
        sysConfigService.updateTemplateSettings(settings);
        return success(true);
    }

}
