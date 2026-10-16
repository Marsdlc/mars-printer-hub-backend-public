package cn.mars.marsprinterhub.model.enums;

import lombok.Getter;

@Getter
public enum MqttConnectionTypeEnum {

    NONE("未连接", 0),
    LOCAL("局域网", 1),
    CLOUD("云端", 2);

    private final String text;

    private final int value;

    MqttConnectionTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }
}