package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.config.PasskeyProperties;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyLoginFinishDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyLoginStartDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyRegisterFinishDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyRegisterStartDTO;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.entity.UserPasskeyCredential;
import cn.mars.marsprinterhub.model.vo.PasskeyAssertionStartVO;
import cn.mars.marsprinterhub.model.vo.PasskeyRegisterResultVO;
import cn.mars.marsprinterhub.model.vo.PasskeyRegistrationStartVO;
import cn.mars.marsprinterhub.model.vo.UserPasskeyCredentialVO;
import cn.mars.marsprinterhub.service.PasskeyCredentialRepository;
import cn.mars.marsprinterhub.service.PasskeyService;
import cn.mars.marsprinterhub.service.SysConfigService;
import cn.mars.marsprinterhub.service.UserPasskeyCredentialService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RegistrationExtensionInputs;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PasskeyServiceImpl implements PasskeyService {

    private static final String PASSKEY_REGISTER_REQUEST_PREFIX = "passkey:register:";
    private static final String PASSKEY_ASSERTION_REQUEST_PREFIX = "passkey:assert:";

    @Resource
    private UserService userService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private PasskeyCredentialRepository passkeyCredentialRepository;

    @Resource
    private UserPasskeyCredentialService userPasskeyCredentialService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private PasskeyProperties passkeyProperties;

    @Override
    public PasskeyRegistrationStartVO startRegistration(UserPasskeyRegisterStartDTO dto) {
        User currentUser = userService.getCurrentUser();
        String nickname = dto == null ? null : dto.getNickname();

        PublicKeyCredentialCreationOptions options = relyingParty().startRegistration(
                StartRegistrationOptions.builder()
                        .user(buildUserIdentity(currentUser))
                        .authenticatorSelection(com.yubico.webauthn.data.AuthenticatorSelectionCriteria.builder()
                                .residentKey(ResidentKeyRequirement.PREFERRED)
                                .build())
                        .extensions(RegistrationExtensionInputs.builder().credProps().build())
                        .timeout(passkeyProperties.getTimeoutMilliseconds())
                        .build()
        );

        String requestId = UUID.randomUUID().toString();
        PendingPasskeyRegistration pending = new PendingPasskeyRegistration();
        pending.setUserId(currentUser.getId());
        pending.setNickname(StrUtil.blankToDefault(nickname, defaultNickname()));
        pending.setOptionsJson(toOptionsJson(options));
        redisUtils.set(PASSKEY_REGISTER_REQUEST_PREFIX + requestId, writeJson(pending), passkeyProperties.getChallengeTtlSeconds());

        return new PasskeyRegistrationStartVO(requestId, parseJson(toCredentialsCreateJson(options)));
    }

    @Override
    public PasskeyRegisterResultVO finishRegistration(String requestId, UserPasskeyRegisterFinishDTO dto) {
        PendingPasskeyRegistration pending = getPendingRegistration(requestId);
        Long currentUserId = userService.getCurrentUserId();
        if (!currentUserId.equals(pending.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该 Passkey 注册请求");
        }

        PublicKeyCredentialCreationOptions request = readCreationOptions(pending.getOptionsJson());
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response = parseRegistrationResponse(dto.getCredential());
        RegistrationResult result;
        try {
            result = relyingParty().finishRegistration(FinishRegistrationOptions.builder()
                    .request(request)
                    .response(response)
                    .build());
        } catch (RegistrationFailedException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passkey 注册校验失败: " + e.getMessage());
        }

        String credentialId = result.getKeyId().getId().getBase64Url();
        boolean exists = userPasskeyCredentialService.lambdaQuery()
                .eq(UserPasskeyCredential::getCredentialId, credentialId)
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该 Passkey 已绑定");
        }

        UserPasskeyCredential credential = new UserPasskeyCredential();
        credential.setUserId(currentUserId);
        credential.setCredentialId(credentialId);
        credential.setPublicKeyCose(result.getPublicKeyCose().getBase64Url());
        credential.setUserHandle(userPasskeyCredentialService.buildUserHandle(currentUserId));
        credential.setSignatureCount(result.getSignatureCount());
        credential.setAaguid(result.getAaguid().getHex());
        credential.setTransports(joinTransports(response.getResponse().getTransports().stream().toList()));
        credential.setAttachment(resolveAttachment(response));
        credential.setNickname(StrUtil.blankToDefault(dto.getNickname(), pending.getNickname()));
        userPasskeyCredentialService.save(credential);

        redisUtils.delete(PASSKEY_REGISTER_REQUEST_PREFIX + requestId);
        return new PasskeyRegisterResultVO(credential.getId(), credential.getNickname());
    }

    @Override
    public PasskeyAssertionStartVO startAssertion(UserPasskeyLoginStartDTO dto) {
        String username = dto == null ? null : StrUtil.blankToDefault(StrUtil.trim(dto.getUsername()), null);

        if (StrUtil.isNotBlank(username) && userService.findByUsername(username).isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        AssertionRequest request = relyingParty().startAssertion(
                StartAssertionOptions.builder()
                        .username(username)
                        .userVerification(UserVerificationRequirement.PREFERRED)
                        .timeout(passkeyProperties.getTimeoutMilliseconds())
                        .build()
        );

        String requestId = UUID.randomUUID().toString();
        PendingPasskeyAssertion pending = new PendingPasskeyAssertion();
        pending.setUsername(username);
        pending.setRequestJson(toAssertionRequestJson(request));
        redisUtils.set(PASSKEY_ASSERTION_REQUEST_PREFIX + requestId, writeJson(pending), passkeyProperties.getChallengeTtlSeconds());

        return new PasskeyAssertionStartVO(requestId, parseJson(toCredentialsGetJson(request)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String finishAssertion(String requestId, UserPasskeyLoginFinishDTO dto) {
        PendingPasskeyAssertion pending = getPendingAssertion(requestId);
        AssertionRequest request = readAssertionRequest(pending.getRequestJson());
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response =
                parseAssertionResponse(dto.getCredential());

        AssertionResult result;
        try {
            result = relyingParty().finishAssertion(FinishAssertionOptions.builder()
                    .request(request)
                    .response(response)
                    .build());
        } catch (AssertionFailedException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, mapAssertionFailureMessage(e));
        }

        if (!result.isSuccess()) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Passkey 登录失败");
        }

        User user = userService.findByUsername(result.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在"));

        UserPasskeyCredential credential = userPasskeyCredentialService.lambdaQuery()
                .eq(UserPasskeyCredential::getCredentialId, result.getCredentialId().getBase64Url())
                .one();
        if (credential == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Passkey 凭证不存在");
        }

        credential.setSignatureCount(result.getSignatureCount());
        credential.setLastUsedTime(new Date());
        boolean updated = userPasskeyCredentialService.updateById(credential);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新 Passkey 使用记录失败");
        }

        redisUtils.delete(PASSKEY_ASSERTION_REQUEST_PREFIX + requestId);
        return userService.loginByUserId(user.getId());
    }

    @Override
    public List<UserPasskeyCredentialVO> listCurrentUserCredentials() {
        Long userId = userService.getCurrentUserId();
        return userPasskeyCredentialService.lambdaQuery()
                .eq(UserPasskeyCredential::getUserId, userId)
                .orderByDesc(UserPasskeyCredential::getCreateTime)
                .list()
                .stream()
                .map(this::toCredentialVO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCredential(Long credentialId) {
        Long userId = userService.getCurrentUserId();
        UserPasskeyCredential credential = userPasskeyCredentialService.getById(credentialId);
        if (credential == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Passkey 不存在");
        }
        if (!userId.equals(credential.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该 Passkey");
        }
        boolean removed = userPasskeyCredentialService.removeById(credentialId);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除 Passkey 失败");
        }
    }

    private UserIdentity buildUserIdentity(User user) {
        return UserIdentity.builder()
                .name(user.getUsername())
                .displayName(StrUtil.blankToDefault(user.getNickname(), user.getUsername()))
                .id(new ByteArray(String.valueOf(user.getId()).getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    private RelyingParty relyingParty() {
        return RelyingParty.builder()
                .identity(com.yubico.webauthn.data.RelyingPartyIdentity.builder()
                        .id(sysConfigService.getEffectivePasskeyRpId())
                        .name(passkeyProperties.getRpName())
                        .build())
                .credentialRepository(passkeyCredentialRepository)
                .origins(sysConfigService.getEffectivePasskeyOrigins())
                .build();
    }

    private PendingPasskeyRegistration getPendingRegistration(String requestId) {
        Object cached = redisUtils.get(PASSKEY_REGISTER_REQUEST_PREFIX + requestId);
        if (!(cached instanceof String json)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Passkey 注册请求不存在或已过期");
        }
        try {
            return objectMapper.readValue(json, PendingPasskeyRegistration.class);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取 Passkey 注册请求失败");
        }
    }

    private PendingPasskeyAssertion getPendingAssertion(String requestId) {
        Object cached = redisUtils.get(PASSKEY_ASSERTION_REQUEST_PREFIX + requestId);
        if (!(cached instanceof String json)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Passkey 登录请求不存在或已过期");
        }
        try {
            return objectMapper.readValue(json, PendingPasskeyAssertion.class);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取 Passkey 登录请求失败");
        }
    }

    private PublicKeyCredentialCreationOptions readCreationOptions(String json) {
        try {
            return PublicKeyCredentialCreationOptions.fromJson(json);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析 Passkey 注册请求失败");
        }
    }

    private PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> parseRegistrationResponse(String json) {
        try {
            return PublicKeyCredential.parseRegistrationResponseJson(json);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passkey 注册响应格式错误");
        }
    }

    private AssertionRequest readAssertionRequest(String json) {
        try {
            return AssertionRequest.fromJson(json);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析 Passkey 登录请求失败");
        }
    }

    private PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> parseAssertionResponse(String json) {
        try {
            return PublicKeyCredential.parseAssertionResponseJson(json);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passkey 登录响应格式错误");
        }
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成 Passkey 注册参数失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缓存 Passkey 注册请求失败");
        }
    }

    private String toCredentialsCreateJson(PublicKeyCredentialCreationOptions options) {
        try {
            return options.toCredentialsCreateJson();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成 Passkey 注册参数失败");
        }
    }

    private String toCredentialsGetJson(AssertionRequest request) {
        try {
            return request.toCredentialsGetJson();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成 Passkey 登录参数失败");
        }
    }

    private String toOptionsJson(PublicKeyCredentialCreationOptions options) {
        try {
            return options.toJson();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成 Passkey 注册参数失败");
        }
    }

    private String toAssertionRequestJson(AssertionRequest request) {
        try {
            return request.toJson();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成 Passkey 登录参数失败");
        }
    }

    private String mapAssertionFailureMessage(AssertionFailedException exception) {
        String message = StrUtil.blankToDefault(exception.getMessage(), "Passkey 登录校验失败");

        if (message.contains("Unknown credential")) {
            return "当前设备返回的 Passkey 未在本账号中绑定，请确认使用正确的设备或重新绑定";
        }
        if (message.contains("Invalid assertion")) {
            return "Passkey 校验未通过，请重试";
        }
        if (message.contains("Challenge")) {
            return "Passkey 登录请求已过期，请重新尝试";
        }
        if (message.contains("User verification")) {
            return "未完成设备身份验证，请重试";
        }

        return "Passkey 登录校验失败，请重试";
    }

    private String defaultNickname() {
        return "我的设备 " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    private String joinTransports(List<com.yubico.webauthn.data.AuthenticatorTransport> transports) {
        if (transports == null || transports.isEmpty()) {
            return null;
        }
        return transports.stream().map(com.yubico.webauthn.data.AuthenticatorTransport::getId).collect(Collectors.joining(","));
    }

    private String resolveAttachment(PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response) {
        AuthenticatorAttachment attachment = response.getAuthenticatorAttachment().orElse(null);
        return attachment == null ? null : attachment.name();
    }

    private UserPasskeyCredentialVO toCredentialVO(UserPasskeyCredential credential) {
        UserPasskeyCredentialVO vo = BeanUtil.copyProperties(credential, UserPasskeyCredentialVO.class);
        String id = credential.getCredentialId();
        if (StrUtil.isNotBlank(id) && id.length() > 12) {
            vo.setCredentialId(id.substring(0, 6) + "..." + id.substring(id.length() - 6));
        }
        return vo;
    }

    @Data
    public static class PendingPasskeyRegistration {
        private Long userId;
        private String nickname;
        private String optionsJson;

        public PendingPasskeyRegistration() {
        }
    }

    @Data
    public static class PendingPasskeyAssertion {
        private String username;
        private String requestJson;

        public PendingPasskeyAssertion() {
        }
    }
}
