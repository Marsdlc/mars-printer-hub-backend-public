package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SpoolHomeSummaryVO {

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
     * 创建时间
     */
    private Date createTime;
}
