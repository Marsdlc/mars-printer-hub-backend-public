package cn.mars.marsprinterhub.model.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderSummaryDTO {

    /**
     * 订单收入
     */
    private BigDecimal totalIncome;

    /**
     * 订单成本
     */
    private BigDecimal totalCost;

    /**
     * 订单利润
     */
    private BigDecimal totalProfit;
}
