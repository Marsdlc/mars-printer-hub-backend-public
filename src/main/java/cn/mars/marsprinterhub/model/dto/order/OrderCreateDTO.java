package cn.mars.marsprinterhub.model.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateDTO {

    private Long id;

    /**
     * 订单名称
     */
    @NotNull(message = "订单名称不能为空")
    private String orderName;

    /**
     * 客户名称
     */
    @NotNull(message = "客户名称不能为空")
    private String customerName;

    /**
     * 客户来源
     */
    private String customerSource;

    /**
     * 收入
     */
    @NotNull(message = "收入不能为空")
    @Min(value = 0, message = "收入不能小于0")
    private BigDecimal totalIncome;

    /**
     * 电力成本
     */
    @Min(value = 0, message = "电力成本不能小于0")
    private BigDecimal electricityCost;

    /**
     * 物流成本
     */
    @Min(value = 0, message = "物流成本不能小于0")
    private BigDecimal logisticsCost;

    /**
     * 设计成本
     */
    @Min(value = 0, message = "设计成本不能小于0")
    private BigDecimal designCost;

    /**
     * 人工成本
     */
    @Min(value = 0, message = "人工成本不能小于0")
    private BigDecimal laborCost;

    /**
     * 其他成本
     */
    @Min(value = 0, message = "其他成本不能小于0")
    private BigDecimal otherCost;

    /**
     * 备注
     */
    private String remark;

}
