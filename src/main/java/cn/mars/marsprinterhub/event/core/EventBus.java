package cn.mars.marsprinterhub.event.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * 事件总线
 * 负责事件的发布、订阅和分发
 */
@Slf4j
@Component
public class EventBus {

    /**
     * 事件处理器注册表
     * Key: 事件类型, Value: 处理器列表
     */
    private final Map<Class<? extends Event>, List<EventHandler<Event>>> handlers = new ConcurrentHashMap<>();

    /**
     * 异步执行器
     */
    private final Executor asyncExecutor;

    public EventBus(Executor taskExecutor) {
        this.asyncExecutor = taskExecutor;
    }

    /**
     * 注册事件处理器
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void register(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((EventHandler<Event>) handler);

        // 按优先级排序
        handlers.get(eventType).sort(Comparator.comparingInt(EventHandler::getPriority));
    }

    /**
     * 发布事件
     */
    public <T extends Event> void publish(T event) {
        if (event == null) {
            log.warn("尝试发布空事件");
            return;
        }

        Class<? extends Event> eventType = event.getClass();
        List<EventHandler<Event>> eventHandlers = handlers.get(eventType);

        if (eventHandlers == null || eventHandlers.isEmpty()) {
            log.error("没有找到事件处理器: eventType={}, eventId={}", eventType.getSimpleName(), event.getEventId());
            return;
        }

        for (EventHandler<Event> handler : eventHandlers) {
            processEvent(event, handler);
        }
    }

    /**
     * 处理单个事件
     */
    private void processEvent(Event event, EventHandler<Event> handler) {
        try {
            if (handler.isAsync()) {
                // 异步处理
                asyncExecutor.execute(() -> handleEvent(event, handler));
            } else {
                // 同步处理
                handleEvent(event, handler);
            }
        } catch (Exception e) {
            log.error("处理事件失败: eventId={}, handler={}, error={}",
                    event.getEventId(), handler.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * 执行事件处理
     */
    private void handleEvent(Event event, EventHandler<Event> handler) {
        try {
            handler.handle(event);
        } catch (Exception e) {
            log.error("处理事件失败: eventId={}, handler={}, error={}",
                    event.getEventId(), handler.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * 获取已注册的事件类型
     */
    public Set<Class<? extends Event>> getRegisteredEventTypes() {
        return new HashSet<>(handlers.keySet());
    }

    /**
     * 获取指定事件类型的处理器数量
     */
    public int getHandlerCount(Class<? extends Event> eventType) {
        List<EventHandler<Event>> eventHandlers = handlers.get(eventType);
        return eventHandlers != null ? eventHandlers.size() : 0;
    }


}