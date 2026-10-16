package cn.mars.marsprinterhub.controller;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.common.ResultUtils;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.dto.bambu.BambuLoginDTO;
import cn.mars.marsprinterhub.model.dto.bambu.BambuLoginWithCodeDTO;
import cn.mars.marsprinterhub.model.dto.bambu.BambuSmsSendDTO;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 拓竹云控制器
 * 提供拓竹云账户和设备管理的REST API
 */
@Slf4j
@RestController
@RequestMapping("/bambu")
@Tag(name = "BambuCloud", description = "拓竹云设备管理相关接口")
public class BambuCloudController {

    @Resource
    private BambuCloudService bambuCloudService;

    /**
     * 拓竹云登录
     */
    @PostMapping("/login")
    @Operation(
            summary = "拓竹云登录",
            operationId = "bambuCloudLogin"
    )
    @RateLimiting(name = "cloud-login")
    public BaseResponse<Map<String, Object>> login(@RequestBody BambuLoginDTO loginRequest) {
        boolean success = bambuCloudService.login(loginRequest.getEmail(), loginRequest.getPassword());
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "登录失败");

        String username = bambuCloudService.getCurrentUsername();
        String userId = bambuCloudService.getCurrentUserId();
        return ResultUtils.success(Map.of(
                "success", true,
                "message", "登录成功",
                "username", username != null ? username : "",
                "userId", userId != null ? userId : ""
        ));
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/send-sms-code")
    @Operation(summary = "发送验证码", description = "向指定账号发送验证码")
    @RateLimiting(name = "sms-code")
    public BaseResponse<Map<String, Object>> sendSmsCode(@RequestBody BambuSmsSendDTO smsRequest) {
        ThrowUtils.throwIf(StrUtil.isBlank(smsRequest.getAccount()), ErrorCode.PARAMS_ERROR, "账号不能为空");
        if (bambuCloudService.isCurrentUserChinaRegion()) {
            bambuCloudService.sendSmsVerificationCode(smsRequest.getAccount());
        } else {
            bambuCloudService.sendEmailVerificationCode(smsRequest.getAccount());
        }

        return ResultUtils.success(Map.of(
                "success", true,
                "message", "验证码发送成功"
        ));
    }

    /**
     * 使用验证码登录拓竹云
     */
    @PostMapping("/login-with-code")
    @Operation(summary = "拓竹云验证码登录", description = "使用验证码完成拓竹云登录")
    @RateLimiting(name = "cloud-login")
    public BaseResponse<Map<String, Object>> loginWithCode(@RequestBody BambuLoginWithCodeDTO loginRequest) {

        boolean success = bambuCloudService.loginWithVerificationCode(loginRequest.getAccount(), loginRequest.getCode());
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "验证码登录失败");
        return ResultUtils.success(Map.of(
                "success", true,
                "message", "登录成功",
                "username", bambuCloudService.getCurrentUsername()
        ));
    }

    /**
     * 获取登录状态
     */
    @GetMapping("/auth/status")
    @Operation(summary = "获取登录状态", description = "获取当前用户的登录状态")
    public BaseResponse<Map<String, Object>> getAuthStatus() {
        boolean isLoggedIn = bambuCloudService.isLoggedIn();
        String username = bambuCloudService.getCurrentUsername();
        String userId = bambuCloudService.getCurrentUserId();

        return ResultUtils.success(Map.of(
                "loggedIn", isLoggedIn,
                "username", username != null ? username : "",
                "userId", userId != null ? userId : ""
        ));
    }

    @PostMapping("/unbind")
    @Operation(summary = "解绑拓竹云账户", description = "解绑当前用户绑定的拓竹云账户")
    public BaseResponse<Map<String, Object>> unbind() {
        boolean success = bambuCloudService.unbind();
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "解绑失败");
        return ResultUtils.success(Map.of(
                "success", true,
                "message", "解绑成功"
        ));
    }

    @GetMapping("/uid")
    @Operation(summary = "获取当前用户ID", description = "获取当前用户ID")
    public BaseResponse<Map<String, Object>> getUid() {
        String uid = bambuCloudService.getCurrentBambuUserUid();
        return ResultUtils.success(Map.of(
                "uid", uid != null ? uid : ""
        ));
    }
}
