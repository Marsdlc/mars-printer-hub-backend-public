package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单信息表
 *
 * @TableName order_info
 */
@TableName(value = "order_info", autoResultMap = true)
@Data
public class OrderInfo {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 订单名称
     */
    private String orderName;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 客户来源
     */
    private String customerSource;

    /**
     * 订单状态 1-进行中 2-已完成 3-已取消 4-库存中
     */
    private Integer status;

    /**
     * 订单开始时间
     */
    private Date startTime;

    /**
     * 订单结束时间
     */
    private Date endTime;

    /**
     * 耗材成本
     */
    private BigDecimal materialCost;

    /**
     * 电力成本
     */
    private BigDecimal electricityCost;

    /**
     * 物流成本
     */
    private BigDecimal logisticsCost;

    /**
     * 物流单号
     */
    private String trackingNumber;

    /**
     * 设计成本
     */
    private BigDecimal designCost;

    /**
     * 人工成本
     */
    private BigDecimal laborCost;

    /**
     * 其他成本
     */
    private BigDecimal otherCost;

    /**
     * 收入
     */
    private BigDecimal totalIncome;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 备注
     */
    private String remark;

    /**
     * 订单相关图片 URL 列表
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> images;

    /**
     * 订单关联模型 URL 列表
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> models;

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
