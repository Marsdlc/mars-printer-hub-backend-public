package cn.mars.marsprinterhub.model.enums;

import lombok.Getter;

/**
 * 设备配置项枚举
 * 定义所有可用的设备配置开关
 *
 * @author mars
 */
@Getter
public enum DeviceConfigItem {

    AUTO_SNAPSHOT("自动保存快照", true),
    AUTO_REPLENISH_SPLIT_DEDUCTION("自动续料拆分扣重", false),
    AMS_SLOT_AUTO_UNBIND("AMS 槽位自动解绑", false),
    P1S_AUTO_OFF_LIGHT("P1S 开始打印时自动关灯", false),
    MQTT_AUTO_RECONNECT("MQTT 自动重连", true);

    /**
     * 配置项显示名称
     */
    private final String displayName;

    /**
     * 默认值
     */
    private final boolean defaultValue;

    DeviceConfigItem(String displayName, boolean defaultValue) {
        this.displayName = displayName;
        this.defaultValue = defaultValue;
    }
}
