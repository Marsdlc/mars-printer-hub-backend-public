package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.license.LicenseActivateReq;
import cn.mars.marsprinterhub.license.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

@Tag(name = "license", description = "旧版授权接口兼容，系统无需激活")
@RestController
@RequestMapping("/license")
public class LicenseController {

    @Resource
    private LicenseService licenseService;

    @Operation(summary = "获取当前机器码")
    @GetMapping("/machine-code")
    public BaseResponse<String> getMachineCode() {
        // 直接返回字符串数据
        return success(licenseService.getMachineCode());
    }

    @Operation(summary = "获取激活状态（始终已启用）")
    @GetMapping("/status")
    public BaseResponse<Boolean> getStatus() {
        return success(licenseService.isActivated());
    }

    @Operation(summary = "兼容旧版激活请求（无需激活）")
    @PostMapping("/activate")
    public BaseResponse<Void> activate(@RequestBody(required = false) LicenseActivateReq req) {
        return success(null);
    }
}
