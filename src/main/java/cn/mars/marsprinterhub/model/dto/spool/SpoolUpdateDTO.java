package cn.mars.marsprinterhub.model.dto.spool;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 更新耗材请求DTO
 */
@Data
public class SpoolUpdateDTO {

    /**
     * 主键ID
     */
    @NotNull(message = "耗材ID不能为空")
    private Long id;

    /**
     * 耗材名称
     */
    private String name;

    /**
     * 材料类型
     */
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
    @Positive(message = "净重必须大于0")
    private BigDecimal netWeight;

    /**
     * 料盘重量(克)
     */
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
     * 首次使用时间
     */
    private Date firstUsedTime;

    /**
     * 最后使用时间
     */
    private Date lastUsedTime;

    /**
     * 外观/质感
     */
    private Integer appearance;

    /**
     * 备注
     */
    private String remark;
}