package cn.mars.marsprinterhub.model.dto.bambu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BambuLoginDTO {
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;
}
