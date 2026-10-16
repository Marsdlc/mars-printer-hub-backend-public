package cn.mars.marsprinterhub.model.dto.sysconfig;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HomeAssistantConfigSaveDTO {

    @NotBlank(message = "Home Assistant 地址不能为空")
    private String baseUrl;

    private String apiKey;

    private String scaleEntityId;
}
