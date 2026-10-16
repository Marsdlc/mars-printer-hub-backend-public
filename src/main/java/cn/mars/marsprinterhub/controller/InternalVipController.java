package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.service.VipActivationCodeService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 内部接口（不需要登录，但需要请求头验证）
 */
@Hidden
@RestController
@RequestMapping("/internal/vip")
public class InternalVipController {

    @Value("${vip.internal-key}")
    private String internalKey;

    @Resource
    private VipActivationCodeService vipActivationCodeService;

    /**
     * VIP 类型枚举
     */
    @Getter
    public enum VipType {
        MONTH(30),
        HALF_YEAR(180),
        YEAR(365);

        private final int days;

        VipType(int days) {
            this.days = days;
        }

    }

    /**
     * 生成激活码
     *
     * @param key  请求头验证 Key
     * @param type VIP 类型
     * @return 激活码
     */
    @PostMapping("/generate")
    public BaseResponse<String> generateCode(
            @RequestHeader("X-Internal-Key") String key,
            @RequestParam VipType type) {
        // 验证请求头
        if (internalKey == null || internalKey.isBlank() || !internalKey.equals(key)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无效的请求");
        }

        String code = vipActivationCodeService.generateCode(type.getDays());
        return success(code);
    }
}
