package cn.mars.marsprinterhub.event.core;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 事件基类
 * 所有业务事件都应继承此类
 */
@Data
public abstract class Event {
    
    /**
     * 事件唯一标识
     */
    private final String eventId;
    
    /**
     * 事件类型
     */
    private final String eventType;
    
    /**
     * 事件发生时间
     */
    private final LocalDateTime timestamp;
    
    /**
     * 事件来源
     */
    private final String source;
    
    /**
     * 事件版本
     */
    private final String version;
    
    protected Event(String eventType, String source) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.source = source;
        this.version = "1.0";
    }
    
    protected Event(String eventType, String source, String version) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.source = source;
        this.version = version;
    }
}