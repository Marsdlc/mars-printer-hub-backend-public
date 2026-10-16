package cn.mars.marsprinterhub.model.dto.order;

import lombok.Data;

@Data
public class OrderUnbindRecordDTO {

    /**
     * 订单 id
     */
    private Long orderId;

    /**
     * 绑定的记录 id
     */
    private Long recordId;
}
