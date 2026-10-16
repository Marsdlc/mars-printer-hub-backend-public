package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.openapi.OpenApiRequestAttributes;
import cn.mars.marsprinterhub.model.vo.OpenDeviceInfoVO;
import cn.mars.marsprinterhub.service.OpenDeviceService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 开放接口入口，后续用于承载基于 API Key 的查询接口。
 */
@RestController
@RequestMapping("/open")
@Tag(name = "Open", description = "开放接口")
public class OpenController {

    @Resource
    private OpenDeviceService openDeviceService;

    @Operation(summary = "开放接口健康检查")
    @GetMapping("/ping")
    public BaseResponse<String> ping() {
        return success("pong");
    }

    @Operation(summary = "获取当前 Open API 调用身份")
    @GetMapping("/whoami")
    public BaseResponse<Map<String, Object>> whoami(HttpServletRequest request) {
        Object userId = request.getAttribute(OpenApiRequestAttributes.OPEN_API_USER_ID);
        Object apiKeyId = request.getAttribute(OpenApiRequestAttributes.OPEN_API_KEY_ID);
        return success(Map.of(
                "userId", userId,
                "apiKeyId", apiKeyId
        ));
    }

    @Operation(summary = "获取开放接口设备信息")
    @GetMapping("/device/{deviceId}")
    @RateLimiting(name = "open-api")
    public BaseResponse<OpenDeviceInfoVO> getDeviceInfo(
            @PathVariable String deviceId,
            HttpServletRequest request) {
        Long userId = getOpenApiUserId(request);
        return success(openDeviceService.getDeviceInfo(userId, deviceId));
    }

    private Long getOpenApiUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(OpenApiRequestAttributes.OPEN_API_USER_ID);
        ThrowUtils.throwIf(!(userId instanceof Long), ErrorCode.NOT_LOGIN_ERROR, "Open API 调用身份缺失");
        return (Long) userId;
    }
}
