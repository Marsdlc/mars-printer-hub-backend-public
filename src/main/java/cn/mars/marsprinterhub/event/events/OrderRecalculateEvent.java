package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 订单重算事件
 * 当已绑定订单的耗材使用记录发生变化时发布此事件
 */
@Getter
public class OrderRecalculateEvent extends Event {

    /**
     * 订单 ID
     */
    private final Long orderId;

    public OrderRecalculateEvent(Long orderId) {
        super("ORDER_RECALCULATE", "AutoSpoolProcessor");
        this.orderId = orderId;
    }
}
