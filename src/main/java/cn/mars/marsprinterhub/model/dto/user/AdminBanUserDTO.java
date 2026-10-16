package cn.mars.marsprinterhub.model.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员封禁用户 DTO
 */
@Data
public class AdminBanUserDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Size(max = 255, message = "封禁原因长度不能超过255")
    private String reason;
}
