package cn.mars.marsprinterhub.model.dto.order;

import lombok.Data;

@Data
public class OrderStatusUpdateDTO {

    /**
     * 订单状态
     */
    private Integer status;
}
