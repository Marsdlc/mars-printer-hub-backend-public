package cn.mars.marsprinterhub.notification.template;

import lombok.Getter;

/**
 * 通知模板类型枚举
 * 定义所有可用的通知模板，用于模板开关配置
 *
 * @author mars
 */
@Getter
public enum NotificationTemplateType {

    PRINT_PAUSED("打印暂停"),
    PRINT_FINISHED("打印完成"),
    AMS_SLOT_UNBIND("槽位自动解绑"),
    AMS_SLOT_EMPTY("槽位为空"),
    AMS_HUMIDITY_HIGH("湿度过高"),
    FILAMENT_NOT_BOUND("未绑定耗材"),
    FILAMENT_NOT_FOUND("耗材未找到"),
    FILAMENT_TYPE_MISMATCH("耗材类型不匹配"),
    FILAMENT_WEIGHT_INSUFFICIENT("耗材重量不足"),
    MQTT_RECONNECT("MQTT 重连");

    /**
     * 模板显示名称
     */
    private final String displayName;

    NotificationTemplateType(String displayName) {
        this.displayName = displayName;
    }
}
