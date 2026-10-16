package cn.mars.marsprinterhub.model.dto.bambu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BambuLoginWithCodeDTO {
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
