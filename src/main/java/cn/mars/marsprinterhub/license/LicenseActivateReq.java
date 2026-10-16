package cn.mars.marsprinterhub.license;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LicenseActivateReq {
    
    @Schema(description = "加密的 License 字符串", requiredMode = Schema.RequiredMode.REQUIRED)
    private String license;
}