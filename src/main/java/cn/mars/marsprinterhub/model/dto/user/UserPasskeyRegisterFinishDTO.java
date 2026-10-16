package cn.mars.marsprinterhub.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasskeyRegisterFinishDTO {

    @NotBlank(message = "credential 不能为空")
    private String credential;

    @Size(max = 100, message = "Passkey 名称过长")
    private String nickname;
}
