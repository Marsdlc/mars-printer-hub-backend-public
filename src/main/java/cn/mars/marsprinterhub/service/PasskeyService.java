package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.user.UserPasskeyRegisterFinishDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyLoginFinishDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyLoginStartDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyRegisterStartDTO;
import cn.mars.marsprinterhub.model.vo.PasskeyAssertionStartVO;
import cn.mars.marsprinterhub.model.vo.PasskeyRegisterResultVO;
import cn.mars.marsprinterhub.model.vo.PasskeyRegistrationStartVO;
import cn.mars.marsprinterhub.model.vo.UserPasskeyCredentialVO;

import java.util.List;

public interface PasskeyService {

    PasskeyRegistrationStartVO startRegistration(UserPasskeyRegisterStartDTO dto);

    PasskeyRegisterResultVO finishRegistration(String requestId, UserPasskeyRegisterFinishDTO dto);

    PasskeyAssertionStartVO startAssertion(UserPasskeyLoginStartDTO dto);

    String finishAssertion(String requestId, UserPasskeyLoginFinishDTO dto);

    List<UserPasskeyCredentialVO> listCurrentUserCredentials();

    void deleteCredential(Long credentialId);
}
