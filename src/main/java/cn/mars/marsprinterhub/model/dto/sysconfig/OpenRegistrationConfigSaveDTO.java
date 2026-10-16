package cn.mars.marsprinterhub.model.dto.sysconfig;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpenRegistrationConfigSaveDTO {

    @NotNull(message = "开放自由注册配置不能为空")
    private Boolean enabled;
}
