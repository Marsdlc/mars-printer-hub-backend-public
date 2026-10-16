package cn.mars.marsprinterhub.model.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderOtherCostUpdateDTO {

    private Long id;

    /**
     * 电力成本
     */
    private BigDecimal electricityCost;

    /**
     * 物流成本
     */
    private BigDecimal logisticsCost;

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
}