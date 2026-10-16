package cn.mars.marsprinterhub.model.dto.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderBindRecordsDTO {

    /**
     * 订单 id
     */
    private Long orderId;

    /**
     * 绑定的记录 ids
     */
    private List<Long> recordIds;
}
