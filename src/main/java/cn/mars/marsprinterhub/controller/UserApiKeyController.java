package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.model.dto.user.UserApiKeyCreateDTO;
import cn.mars.marsprinterhub.model.dto.user.UserApiKeyUpdateDTO;
import cn.mars.marsprinterhub.model.vo.UserApiKeyCreateVO;
import cn.mars.marsprinterhub.model.vo.UserApiKeyVO;
import cn.mars.marsprinterhub.service.UserApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

@RestController
@RequestMapping("/user/api-key")
@Tag(name = "UserApiKey", description = "用户 API Key 接口")
public class UserApiKeyController {

    @Resource
    private UserApiKeyService userApiKeyService;

    @Operation(summary = "获取当前用户 API Key")
    @GetMapping("/current")
    public BaseResponse<UserApiKeyVO> getCurrentUserApiKey() {
        return success(userApiKeyService.getCurrentUserApiKey());
    }

    @Operation(summary = "创建当前用户 API Key")
    @PostMapping("/current")
    public BaseResponse<UserApiKeyCreateVO> createCurrentUserApiKey(@RequestBody @Valid UserApiKeyCreateDTO createDTO) {
        return success(userApiKeyService.createCurrentUserApiKey(createDTO));
    }

    @Operation(summary = "更新当前用户 API Key")
    @PutMapping("/current")
    public BaseResponse<Boolean> updateCurrentUserApiKey(@RequestBody @Valid UserApiKeyUpdateDTO updateDTO) {
        userApiKeyService.updateCurrentUserApiKey(updateDTO);
        return success(true);
    }

    @Operation(summary = "启用当前用户 API Key")
    @PutMapping("/current/enable")
    public BaseResponse<Boolean> enableCurrentUserApiKey() {
        userApiKeyService.enableCurrentUserApiKey();
        return success(true);
    }

    @Operation(summary = "禁用当前用户 API Key")
    @PutMapping("/current/disable")
    public BaseResponse<Boolean> disableCurrentUserApiKey() {
        userApiKeyService.disableCurrentUserApiKey();
        return success(true);
    }

    @Operation(summary = "重置当前用户 API Key")
    @PutMapping("/current/reset")
    public BaseResponse<UserApiKeyCreateVO> resetCurrentUserApiKey() {
        return success(userApiKeyService.resetCurrentUserApiKey());
    }
}
