package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.user.UserApiKeyCreateDTO;
import cn.mars.marsprinterhub.model.dto.user.UserApiKeyUpdateDTO;
import cn.mars.marsprinterhub.model.entity.UserApiKey;
import cn.mars.marsprinterhub.model.vo.UserApiKeyCreateVO;
import cn.mars.marsprinterhub.model.vo.UserApiKeyVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserApiKeyService extends IService<UserApiKey> {

    UserApiKeyVO getCurrentUserApiKey();

    UserApiKeyCreateVO createCurrentUserApiKey(UserApiKeyCreateDTO createDTO);

    void updateCurrentUserApiKey(UserApiKeyUpdateDTO updateDTO);

    void enableCurrentUserApiKey();

    void disableCurrentUserApiKey();

    UserApiKeyCreateVO resetCurrentUserApiKey();

    UserApiKey validateApiKey(String apiKey);

    void recordApiKeyUsage(Long apiKeyId, String ipAddress);
}
