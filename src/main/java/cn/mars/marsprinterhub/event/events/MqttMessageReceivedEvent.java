package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttMessage;
import cn.mars.marsprinterhub.event.core.Event;
import lombok.Getter;

/**
 * MQTT消息接收事件
 * 当收到来自设备的MQTT消息时发布此事件
 */
@Getter
public class MqttMessageReceivedEvent extends Event {

    /**
     * 设备ID
     */
    private final String deviceId;

    /**
     * 解析后的MQTT消息
     */
    private final BambuMqttMessage message;

    public MqttMessageReceivedEvent(String deviceId, BambuMqttMessage message) {
        super("MQTT_MESSAGE_RECEIVED", "BambuMqttClientManager");
        this.deviceId = deviceId;
        this.message = message;
    }
}