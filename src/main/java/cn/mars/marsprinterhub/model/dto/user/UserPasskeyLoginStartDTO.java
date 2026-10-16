package cn.mars.marsprinterhub.model.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasskeyLoginStartDTO {

    @Size(max = 50, message = "用户名长度不能超过 50")
    private String username;
}
