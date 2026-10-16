package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.entity.UserPasskeyCredential;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yubico.webauthn.data.ByteArray;

public interface UserPasskeyCredentialService extends IService<UserPasskeyCredential> {

    String buildUserHandle(Long userId);

    Long parseUserHandle(ByteArray userHandle);
}
