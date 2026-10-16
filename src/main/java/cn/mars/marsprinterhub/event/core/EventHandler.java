package cn.mars.marsprinterhub.event.core;

/**
 * 事件处理器接口
 * 所有事件处理器都应实现此接口
 */
@FunctionalInterface
public interface EventHandler<T extends Event> {

    /**
     * 处理事件
     *
     * @param event 事件对象
     */
    void handle(T event);

    /**
     * 获取处理器支持的事件类型
     * 默认通过泛型推断
     */
    default Class<? extends Event> getSupportedEventType() {
        return Event.class;
    }

    /**
     * 获取处理器优先级
     * 数值越小优先级越高
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否异步处理
     */
    default boolean isAsync() {
        return true;
    }
}