package cn.mars.marsprinterhub.model.dto.order;

import cn.mars.marsprinterhub.model.enums.OrderQuantityOperationEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderQuantityAdjustDTO {

    /**
     * 订单 ID
     */
    @NotNull(message = "订单 ID 不能为空")
    private Long orderId;

    /**
     * 操作类型：入库 / 出库
     */
    @NotNull(message = "操作类型不能为空")
    private OrderQuantityOperationEnum operation;

    /**
     * 调整数量
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer quantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;
}
