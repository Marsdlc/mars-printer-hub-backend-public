package cn.mars.marsprinterhub.model.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasskeyRegisterStartDTO {

    @Size(max = 100, message = "Passkey 名称过长")
    private String nickname;
}
