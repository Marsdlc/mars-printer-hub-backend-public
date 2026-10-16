package cn.mars.marsprinterhub.model.dto.user;

import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册 DTO
 */
@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 16, message = "用户名长度必须在4-16位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 16, message = "密码长度必须在8-16位之间")
    private String password;
    
    /**
     * 昵称（可选）
     */
    private String nickname;

    /**
     * 拓竹区域（可选，默认 China）
     */
    private BambuRegionEnum bambuRegion;
}
