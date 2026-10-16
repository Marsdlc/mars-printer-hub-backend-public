package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 耗材料盘表
 *
 * @TableName spools
 */
@TableName(value = "spools")
@Data
public class Spool {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
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
     * 外观/质感
     */
    private Integer appearance;

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
     * 是否删除(0:未删除,1:已删除)
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Date updateTime;
}