package cn.mars.marsprinterhub.model.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderUpdateDTO {

    private Long id;

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
     * 其他成本
     */
    private BigDecimal otherCost;

    /**
     * 收入
     */
    private BigDecimal totalIncome;

    /**
     * 备注
     */
    private String remark;

}
