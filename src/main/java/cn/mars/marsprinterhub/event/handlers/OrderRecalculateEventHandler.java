package cn.mars.marsprinterhub.event.handlers;

import cn.mars.marsprinterhub.event.core.EventBus;
import cn.mars.marsprinterhub.event.core.EventHandler;
import cn.mars.marsprinterhub.event.events.OrderRecalculateEvent;
import cn.mars.marsprinterhub.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单重算事件处理器
 * 处理耗材使用记录变更后的订单刷新
 */
@Slf4j
@Component
public class OrderRecalculateEventHandler implements EventHandler<OrderRecalculateEvent> {

    @Resource
    private EventBus eventBus;

    @Resource
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void init() {
        eventBus.register(OrderRecalculateEvent.class, this);
        log.info("订单重算事件处理器已注册");
    }

    @Override
    public void handle(OrderRecalculateEvent event) {
        Long orderId = event.getOrderId();
        if (orderId == null) {
            return;
        }
        try {
            log.info("处理订单重算事件: orderId={}", orderId);
            orderInfoService.calculateOrder(orderId);
        } catch (Exception e) {
            log.error("处理订单重算事件失败: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
