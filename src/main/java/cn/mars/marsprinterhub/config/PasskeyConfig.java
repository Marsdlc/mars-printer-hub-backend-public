package cn.mars.marsprinterhub.config;

import cn.mars.marsprinterhub.service.PasskeyCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

@Configuration
@EnableConfigurationProperties(PasskeyProperties.class)
public class PasskeyConfig {

    @Bean
    public RelyingParty relyingParty(PasskeyProperties properties, PasskeyCredentialRepository credentialRepository) {
        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(properties.getRpId())
                        .name(properties.getRpName())
                        .build())
                .credentialRepository(credentialRepository)
                .origins(new HashSet<>(properties.getOrigins()))
                .build();
    }
}
