package cn.mars.marsprinterhub.model.dto.user;

import cn.mars.marsprinterhub.validation.FieldMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户修改密码 DTO
 */
@Data
@FieldMatch(first = "newPassword", second = "confirmPassword", message = "两次输入的密码不一致")
public class UserChangePasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 16, message = "新密码长度必须在8-16位之间")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
