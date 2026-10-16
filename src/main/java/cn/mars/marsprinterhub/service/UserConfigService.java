package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.user.UserConfigDTO;
import cn.mars.marsprinterhub.model.entity.UserConfig;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserConfigService extends IService<UserConfig> {

    /**
     * 获取当前用户配置
     *
     * @return 用户配置
     */
    UserConfigDTO getCurrentUserConfig();

    /**
     * 根据用户ID获取配置
     *
     * @param userId 用户ID
     * @return 用户配置
     */
    UserConfigDTO getUserConfigByUserId(Long userId);

    /**
     * 保存当前用户配置
     *
     * @param config 用户配置
     */
    void saveCurrentUserConfig(UserConfigDTO config);
}
