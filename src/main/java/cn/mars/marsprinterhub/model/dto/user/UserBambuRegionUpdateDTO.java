package cn.mars.marsprinterhub.model.dto.user;

import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 当前用户拓竹区域更新 DTO
 */
@Data
public class UserBambuRegionUpdateDTO {

    @NotNull(message = "拓竹区域不能为空")
    private BambuRegionEnum bambuRegion;
}
