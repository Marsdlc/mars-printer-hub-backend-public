package cn.mars.marsprinterhub.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasskeyRegisterResultVO {

    private Long credentialId;

    private String nickname;
}
