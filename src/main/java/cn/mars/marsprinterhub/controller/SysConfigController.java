package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.model.dto.sysconfig.AliyunMarketAppCodeSaveDTO;
import cn.mars.marsprinterhub.model.dto.sysconfig.HomeAssistantConfigSaveDTO;
import cn.mars.marsprinterhub.model.dto.sysconfig.OpenRegistrationConfigSaveDTO;
import cn.mars.marsprinterhub.model.dto.sysconfig.PasskeyConfigSaveDTO;
import cn.mars.marsprinterhub.model.dto.sysconfig.PublicAccessUrlSaveDTO;
import cn.mars.marsprinterhub.service.AccessControlService;
import cn.mars.marsprinterhub.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

@RestController
@RequestMapping("/sys/config")
@Tag(name = "SysConfig", description = "系统配置接口")
public class SysConfigController {

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private AccessControlService accessControlService;

    @Operation(summary = "获取阿里云市场 AppCode 配置状态")
    @GetMapping("/aliyun-market-app-code")
    public BaseResponse<SysConfigService.AliyunMarketAppCodeVO> getAliyunMarketAppCodeInfo() {
        accessControlService.checkAdmin();
        return success(sysConfigService.getAliyunMarketAppCodeInfo());
    }

    @Operation(summary = "保存阿里云市场 AppCode")
    @PutMapping("/aliyun-market-app-code")
    public BaseResponse<Boolean> saveAliyunMarketAppCode(@Valid @RequestBody AliyunMarketAppCodeSaveDTO dto) {
        accessControlService.checkAdmin();
        sysConfigService.saveAliyunMarketAppCode(dto.getAppCode());
        return success(true);
    }

    @Operation(summary = "获取 Home Assistant 配置状态")
    @GetMapping("/home-assistant")
    public BaseResponse<SysConfigService.HomeAssistantConfigVO> getHomeAssistantConfigInfo() {
        accessControlService.checkAdmin();
        return success(sysConfigService.getHomeAssistantConfigInfo());
    }

    @Operation(summary = "保存 Home Assistant 配置")
    @PutMapping("/home-assistant")
    public BaseResponse<Boolean> saveHomeAssistantConfig(@Valid @RequestBody HomeAssistantConfigSaveDTO dto) {
        accessControlService.checkAdmin();
        sysConfigService.saveHomeAssistantConfig(dto.getBaseUrl(), dto.getApiKey(), dto.getScaleEntityId());
        return success(true);
    }

    @Operation(summary = "获取公网访问 URL")
    @GetMapping("/public-access-url")
    public BaseResponse<SysConfigService.PublicAccessUrlVO> getPublicAccessUrlInfo() {
        accessControlService.checkAdmin();
        return success(sysConfigService.getPublicAccessUrlInfo());
    }

    @Operation(summary = "保存公网访问 URL")
    @PutMapping("/public-access-url")
    public BaseResponse<Boolean> savePublicAccessUrl(@Valid @RequestBody PublicAccessUrlSaveDTO dto) {
        accessControlService.checkAdmin();
        sysConfigService.savePublicAccessUrl(dto.getPublicAccessUrl());
        return success(true);
    }

    @Operation(summary = "获取 Passkey 配置")
    @GetMapping("/passkey")
    public BaseResponse<SysConfigService.PasskeyConfigVO> getPasskeyConfigInfo() {
        accessControlService.checkAdmin();
        return success(sysConfigService.getPasskeyConfigInfo());
    }

    @Operation(summary = "保存 Passkey 配置")
    @PutMapping("/passkey")
    public BaseResponse<Boolean> savePasskeyConfig(@Valid @RequestBody PasskeyConfigSaveDTO dto) {
        accessControlService.checkAdmin();
        sysConfigService.savePasskeyConfig(dto.getRpId(), dto.getOrigins());
        return success(true);
    }

    @Operation(summary = "获取开放自由注册配置")
    @GetMapping("/open-registration")
    public BaseResponse<SysConfigService.OpenRegistrationConfigVO> getOpenRegistrationConfigInfo() {
        accessControlService.checkAdmin();
        return success(sysConfigService.getOpenRegistrationConfigInfo());
    }

    @Operation(summary = "保存开放自由注册配置")
    @PutMapping("/open-registration")
    public BaseResponse<Boolean> saveOpenRegistrationConfig(@Valid @RequestBody OpenRegistrationConfigSaveDTO dto) {
        accessControlService.checkAdmin();
        sysConfigService.saveOpenRegistrationConfig(dto.getEnabled());
        return success(true);
    }

}
