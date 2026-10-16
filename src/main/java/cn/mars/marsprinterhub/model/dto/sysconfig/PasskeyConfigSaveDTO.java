package cn.mars.marsprinterhub.model.dto.sysconfig;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PasskeyConfigSaveDTO {

    @NotBlank(message = "RP ID 不能为空")
    private String rpId;

    @NotEmpty(message = "Allowed Origins 不能为空")
    private List<String> origins;
}
