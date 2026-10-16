package cn.mars.marsprinterhub.model.dto.spool;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 耗材使用DTO
 */
@Data
@Schema(description = "耗材使用DTO")
public class SpoolUseDTO {

    @Schema(description = "耗材ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "耗材ID不能为空")
    private Long spoolId;

    @Schema(description = "使用重量(克)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "使用重量不能为空")
    @Positive(message = "使用重量必须大于0")
    private BigDecimal usedWeight;
}