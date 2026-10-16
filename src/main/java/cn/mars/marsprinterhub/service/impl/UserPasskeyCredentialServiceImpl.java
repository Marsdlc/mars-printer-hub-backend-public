package cn.mars.marsprinterhub.service.impl;

import cn.mars.marsprinterhub.mapper.UserPasskeyCredentialMapper;
import cn.mars.marsprinterhub.model.entity.UserPasskeyCredential;
import cn.mars.marsprinterhub.service.UserPasskeyCredentialService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class UserPasskeyCredentialServiceImpl extends ServiceImpl<UserPasskeyCredentialMapper, UserPasskeyCredential>
        implements UserPasskeyCredentialService {

    @Override
    public String buildUserHandle(Long userId) {
        return new ByteArray(String.valueOf(userId).getBytes(StandardCharsets.UTF_8)).getBase64Url();
    }

    @Override
    public Long parseUserHandle(ByteArray userHandle) {
        try {
            return Long.valueOf(new String(userHandle.getBytes(), StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return null;
        }
    }
}
