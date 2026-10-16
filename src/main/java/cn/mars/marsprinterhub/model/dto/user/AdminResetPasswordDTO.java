package cn.mars.marsprinterhub.model.dto.user;

import cn.mars.marsprinterhub.validation.FieldMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置密码 DTO
 */
@Data
@FieldMatch(first = "newPassword", second = "confirmPassword", message = "两次输入的密码不一致")
public class AdminResetPasswordDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 16, message = "新密码长度必须在8-16位之间")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
