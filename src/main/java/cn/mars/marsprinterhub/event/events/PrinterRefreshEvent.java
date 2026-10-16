package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 刷新打印机
 */
@Getter
public class PrinterRefreshEvent extends Event {

    /**
     * 打印机id
     */
    private final Long printerId;

    public PrinterRefreshEvent(Long printerId) {
        super("PRINT_REFRESH", "");
        this.printerId = printerId;
    }
}