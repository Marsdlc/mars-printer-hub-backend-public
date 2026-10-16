package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * 设备连接事件
 * 当设备连接状态发生变化时发布此事件
 */
@Getter
public class DeviceConnectionEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    /**
     * 连接状态
     */
    private final ConnectionStatus status;

    /**
     * 连接类型
     */
    private final ConnectionType connectionType;

    /**
     * 错误信息（如果有）
     */
    private final String errorMessage;

    public DeviceConnectionEvent(String deviceId, ConnectionStatus status, ConnectionType connectionType) {
        this(deviceId, status, connectionType, null);
    }

    public DeviceConnectionEvent(String deviceId, ConnectionStatus status, ConnectionType connectionType, String errorMessage) {
        super("DEVICE_CONNECTION_CHANGED", "BambuMqttService");
        this.deviceId = deviceId;
        this.status = status;
        this.connectionType = connectionType;
        this.errorMessage = errorMessage;
    }

    /**
     * 连接状态枚举
     */
    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        CONNECTION_FAILED,
        CONNECTION_LOST
    }

    /**
     * 连接类型枚举
     */
    public enum ConnectionType {
        LOCAL,      // 局域网连接
        CLOUD,      // 云端连接
        UNKNOWN     // 未知类型
    }
}