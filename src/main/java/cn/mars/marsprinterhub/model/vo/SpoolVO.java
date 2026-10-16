package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 耗材视图对象
 */
@Data
public class SpoolVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
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
    private BigDecimal netWeight;

    /**
     * 料盘重量(克)
     */
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
     * 备注
     */
    private String remark;

    /**
     * 外观/质感
     */
    private Integer appearance;

    /**
     * 剩余重量(克) - 计算字段
     */
    private BigDecimal remainingWeight;

    /**
     * 剩余百分比
     */
    private BigDecimal remainingPercentage;

    /**
     * 首次使用时间
     */
    private Date firstUsedTime;

    /**
     * 最后使用时间
     */
    private Date lastUsedTime;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
