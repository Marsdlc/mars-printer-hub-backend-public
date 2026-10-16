package cn.mars.marsprinterhub.model.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderTrackingNumberUpdateDTO {

    @NotNull(message = "订单 ID 不能为空")
    private Long id;

    /**
     * 物流单号
     */
    private String trackingNumber;
}
