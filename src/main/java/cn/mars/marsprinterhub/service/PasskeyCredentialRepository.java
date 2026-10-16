package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.entity.UserPasskeyCredential;
import cn.mars.marsprinterhub.service.impl.UserServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.exception.Base64UrlException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PasskeyCredentialRepository implements CredentialRepository {

    @Resource
    private UserPasskeyCredentialService userPasskeyCredentialService;

    @Resource
    private UserService userService;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userService.findByUsername(username)
                .map(user -> userPasskeyCredentialService.lambdaQuery()
                        .eq(UserPasskeyCredential::getUserId, user.getId())
                        .list())
                .orElse(List.of())
                .stream()
                .map(credential -> PublicKeyCredentialDescriptor.builder()
                        .id(fromBase64Url(credential.getCredentialId()))
                        .build())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userService.findByUsername(username)
                .map(user -> fromBase64Url(userPasskeyCredentialService.buildUserHandle(user.getId())));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        Long userId = userPasskeyCredentialService.parseUserHandle(userHandle);
        return userId == null ? Optional.empty() : Optional.ofNullable(userService.getById(userId)).map(user -> user.getUsername());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        Long userId = userPasskeyCredentialService.parseUserHandle(userHandle);
        if (userId == null) {
            return Optional.empty();
        }
        return userPasskeyCredentialService.lambdaQuery()
                .eq(UserPasskeyCredential::getUserId, userId)
                .eq(UserPasskeyCredential::getCredentialId, credentialId.getBase64Url())
                .oneOpt()
                .map(this::toRegisteredCredential);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return userPasskeyCredentialService.lambdaQuery()
                .eq(UserPasskeyCredential::getCredentialId, credentialId.getBase64Url())
                .list()
                .stream()
                .map(this::toRegisteredCredential)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private RegisteredCredential toRegisteredCredential(UserPasskeyCredential credential) {
        return RegisteredCredential.builder()
                .credentialId(fromBase64Url(credential.getCredentialId()))
                .userHandle(fromBase64Url(credential.getUserHandle()))
                .publicKeyCose(fromBase64Url(credential.getPublicKeyCose()))
                .signatureCount(credential.getSignatureCount())
                .build();
    }

    private ByteArray fromBase64Url(String value) {
        try {
            return ByteArray.fromBase64Url(value);
        } catch (Base64UrlException e) {
            throw new IllegalStateException("Invalid stored passkey credential data", e);
        }
    }
}
