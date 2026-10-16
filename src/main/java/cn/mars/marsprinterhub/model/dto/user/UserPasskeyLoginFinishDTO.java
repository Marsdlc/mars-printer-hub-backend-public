package cn.mars.marsprinterhub.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserPasskeyLoginFinishDTO {

    @NotBlank(message = "credential 不能为空")
    private String credential;
}
