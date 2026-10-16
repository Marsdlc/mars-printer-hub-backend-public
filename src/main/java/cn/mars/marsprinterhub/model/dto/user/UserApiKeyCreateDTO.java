package cn.mars.marsprinterhub.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserApiKeyCreateDTO {

    @NotBlank(message = "名称不能为空")
    private String name;
}
