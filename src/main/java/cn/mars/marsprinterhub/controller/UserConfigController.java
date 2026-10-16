package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.model.dto.user.UserConfigDTO;
import cn.mars.marsprinterhub.service.UserConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

@RestController
@RequestMapping("/user/config")
@Tag(name = "UserConfig", description = "用户配置接口")
public class UserConfigController {

    @Resource
    private UserConfigService userConfigService;

    @Operation(summary = "获取当前用户配置")
    @GetMapping("/current")
    public BaseResponse<UserConfigDTO> getCurrentUserConfig() {
        return success(userConfigService.getCurrentUserConfig());
    }

    @Operation(summary = "保存当前用户配置")
    @PutMapping("/current")
    public BaseResponse<Boolean> saveCurrentUserConfig(@RequestBody @Valid UserConfigDTO config) {
        userConfigService.saveCurrentUserConfig(config);
        return success(true);
    }
}
