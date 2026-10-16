package cn.mars.marsprinterhub.model.dto.spool;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建耗材请求DTO
 */
@Data
public class SpoolCreateDTO {

    /**
     * 耗材名称
     */
    @NotBlank(message = "耗材名称不能为空")
    private String name;

    /**
     * 材料类型
     */
    @NotBlank(message = "材料类型不能为空")
    private String materialType;

    /**
     * 颜色
     */
    private String color;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 净重(克)
     */
    @NotNull(message = "净重不能为空")
    @Positive(message = "净重必须大于0")
    private BigDecimal netWeight;

    /**
     * 料盘重量(克)
     */
    @NotNull(message = "料盘重量不能为空")
    @Positive(message = "料盘重量必须大于0")
    private BigDecimal spoolWeight;

    /**
     * 已使用重量(克)
     */
    private BigDecimal usedWeight;

    /**
     * 价格(元)
     */
    private BigDecimal price;

    /**
     * 外观/质感
     */
    private Integer appearance;

    /**
     * 备注
     */
    private String remark;

    /**
     * 数量
     */
    @NotNull(message = "数量不能为空")
    @Max(value = 10, message = "数量不能大于10")
    @Min(value = 1, message = "数量不能小于1")
    private Integer num = 1;
}