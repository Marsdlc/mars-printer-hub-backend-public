package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 解绑打印机
 */
@Getter
public class PrinterUnbindEvent extends Event {

    /**
     * 打印机id
     */
    private final Long printerId;

    public PrinterUnbindEvent(Long printerId) {
        super("PRINT_UNBIND", "");
        this.printerId = printerId;
    }
}