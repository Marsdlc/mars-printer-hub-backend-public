package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.entity.VipActivationCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * VIP 激活码服务
 */
public interface VipActivationCodeService extends IService<VipActivationCode> {

    String LIFETIME_ACTIVATION_CODE = "mars";

    /**
     * 激活 VIP
     *
     * @param code 激活码
     * @return 普通激活码的天数，或内置激活码距 9999 年到期的天数
     */
    int activateVip(String code);

    /**
     * 生成激活码
     *
     * @param days 有效天数
     * @return 激活码
     */
    String generateCode(int days);
}
