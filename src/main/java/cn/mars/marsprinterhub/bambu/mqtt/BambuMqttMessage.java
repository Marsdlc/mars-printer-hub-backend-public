package cn.mars.marsprinterhub.bambu.mqtt;

import cn.mars.marsprinterhub.bambu.model.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Bambu打印机MQTT消息简化类
 * 只保留核心的打印状态信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BambuMqttMessage {

    /**
     * 打印进度百分比
     */
    @JsonProperty("mc_percent")
    private Integer mcPercent;

    /**
     * 当前层数
     */
    @JsonProperty("layer_num")
    private Integer layerNum;

    /**
     * 总层数
     */
    @JsonProperty("total_layer_num")
    private Integer totalLayerNum;

    /**
     * 剩余时间
     */
    @JsonProperty("mc_remaining_time")
    private Integer mcRemainingTime;

    /**
     * G-code状态
     */
    @JsonProperty("gcode_state")
    private String gcodeState;

    /**
     * 外挂料盘
     */
    @JsonProperty("vt_tray")
    private AmsTray vtTray;

    /**
     * 外挂料盘
     */
    @JsonProperty("vir_slot")
    private List<AmsTray> virSlot;

    /**
     * AMS相关字段
     */
    @JsonProperty("ams")
    private AmsStatus ams;

    /**
     * 网络信息
     */
    @JsonProperty("net")
    private NetworkInfo net;

    /**
     * 灯光信息
     */
    @JsonProperty("lights_report")
    private List<LightInfo> lightsReport;

    /**
     * 部件冷却风扇原始速度，Bambu 上报为 0-15 档位。
     */
    @JsonProperty("cooling_fan_speed")
    private Integer coolingFanSpeed;

    /**
     * 辅助风扇原始速度，Bambu 上报为 0-15 档位。
     */
    @JsonProperty("big_fan1_speed")
    private Integer bigFan1Speed;

    /**
     * 腔体风扇原始速度，Bambu 上报为 0-15 档位。
     */
    @JsonProperty("big_fan2_speed")
    private Integer bigFan2Speed;

    /**
     * 设备信息
     */
    @JsonProperty("device")
    private DeviceStatus device;

    /**
     * 热床温度
     */
    @JsonProperty("bed_temper")
    private Integer bedTemper;

    /**
     * 旧版 MQTT 字段：当前喷嘴温度
     */
    @JsonProperty("nozzle_temper")
    private Double nozzleTemper;

    /**
     * 旧版 MQTT 字段：目标喷嘴温度
     */
    @JsonProperty("nozzle_target_temper")
    private Double nozzleTargetTemper;

    /**
     * HMS错误列表
     */
    @JsonProperty("hms")
    private List<HmsError> hms;

    public boolean isEmpty() {
        return mcPercent == null &&
                layerNum == null &&
                totalLayerNum == null &&
                mcRemainingTime == null &&
                gcodeState == null &&
                vtTray == null &&
                virSlot == null &&
                ams == null &&
                net == null &&
                lightsReport == null &&
                coolingFanSpeed == null &&
                bigFan1Speed == null &&
                bigFan2Speed == null &&
                bedTemper == null &&
                nozzleTemper == null &&
                nozzleTargetTemper == null &&
                hms == null &&
                device == null;
    }

    public Integer getCoolingFanSpeedPercentage() {
        return toFanPercentage(coolingFanSpeed);
    }

    public Integer getAuxFanSpeedPercentage() {
        return toFanPercentage(bigFan1Speed);
    }

    public Integer getChamberFanSpeedPercentage() {
        return toFanPercentage(bigFan2Speed);
    }

    public Integer getSecondaryAuxFanSpeedPercentage() {
        if (device == null || device.getAirduct() == null || device.getAirduct().getParts() == null) {
            return null;
        }
        return device.getAirduct().getParts().stream()
                .filter(part -> part != null && Integer.valueOf(160).equals(part.getId()))
                .map(DeviceStatus.Airduct.AirductPart::getState)
                .findFirst()
                .orElse(null);
    }

    private static Integer toFanPercentage(Integer speed) {
        if (speed == null) {
            return null;
        }
        if (speed <= 0) {
            return 0;
        }
        int percentage = (int) Math.ceil(speed / 15D * 100D);
        return Math.min(100, (int) Math.ceil(percentage / 10D) * 10);
    }
}
