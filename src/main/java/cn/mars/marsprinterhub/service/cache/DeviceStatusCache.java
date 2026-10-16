package cn.mars.marsprinterhub.service.cache;

import cn.mars.marsprinterhub.bambu.model.AmsStatus;
import cn.mars.marsprinterhub.bambu.model.LightInfo;
import cn.mars.marsprinterhub.bambu.model.NetworkInfo;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttMessage;
import cn.mars.marsprinterhub.model.enums.PrinterStatusEnum;
import cn.mars.marsprinterhub.utils.NetworkUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 设备状态缓存数据模型
 * 用于Redis存储设备状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceStatusCache implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * MQTT消息对象，只存核心字段
     */
    private BambuMqttMessage message;

    /**
     * 状态更新时间戳
     */
    private String timestamp;

    // ------------------ 动态计算字段，不参与序列化 ------------------

    @JsonIgnore
    public String getPrintStatus() {
        return message != null ? message.getGcodeState() : null;
    }

    @JsonIgnore
    public Integer getPrintProgress() {
        return message != null ? message.getMcPercent() : null;
    }

    @JsonIgnore
    public Integer getCurrentLayerNum() {
        return message != null ? message.getLayerNum() : null;
    }

    @JsonIgnore
    public Integer getTotalLayerNum() {
        return message != null ? message.getTotalLayerNum() : null;
    }

    @JsonIgnore
    public Integer getRemainingTime() {
        return message != null ? message.getMcRemainingTime() : null;
    }

    @JsonIgnore
    public Integer getMcPercent() {
        return message != null ? message.getMcPercent() : null;
    }

    @JsonIgnore
    public Integer getCurrentLayer() {
        return message != null ? message.getLayerNum() : null;
    }

    @JsonIgnore
    public Integer getTotalLayers() {
        return message != null ? message.getTotalLayerNum() : null;
    }

    @JsonIgnore
    public AmsStatus getAmsStatus() {
        return message != null ? message.getAms() : null;
    }

    @JsonIgnore
    public boolean isPrintStarted() {
        String status = getPrintStatus();
        return PrinterStatusEnum.RUNNING.getValue().equals(status) || PrinterStatusEnum.PREPARE.getValue().equals(status);
    }

    @JsonIgnore
    public boolean isPrintFinished() {
        return PrinterStatusEnum.FINISH.getValue().equals(getPrintStatus());
    }

    @JsonIgnore
    public boolean isPrintPaused() {
        return PrinterStatusEnum.PAUSE.getValue().equals(getPrintStatus());
    }

    @JsonIgnore
    public boolean isPrintFailed() {
        return PrinterStatusEnum.FAILED.getValue().equals(getPrintStatus());
    }

    @JsonIgnore
    public String getChamberLight() {
        if (message == null || message.getLightsReport() == null) {
            return "off";
        }
        return message.getLightsReport()
                .stream()
                .filter(lightInfo -> "chamber_light".equals(lightInfo.getNode()))
                .findFirst()
                .map(LightInfo::getMode)
                .orElse("off");
    }

    @JsonIgnore
    public String getChamberLight2() {
        if (message == null || message.getLightsReport() == null) {
            return "off";
        }
        return message.getLightsReport()
                .stream()
                .filter(lightInfo -> "chamber_light2".equals(lightInfo.getNode()))
                .findFirst()
                .map(LightInfo::getMode)
                .orElse("off");
    }

    @JsonIgnore
    public String getStatusSummary() {
        if (message == null) return "";
        StringBuilder summary = new StringBuilder();
        String status = getPrintStatus();
        if (status != null) summary.append("状态: ").append(status);
        Integer progress = getPrintProgress();
        if (progress != null) summary.append(", 进度: ").append(progress).append("%");
        Integer remaining = getRemainingTime();
        if (remaining != null) summary.append(", 剩余: ").append(remaining).append("分钟");
        Integer current = getCurrentLayer();
        Integer total = getTotalLayers();
        if (current != null && total != null) summary.append(", 层数: ").append(current).append("/").append(total);
        return summary.toString();
    }

    @JsonIgnore
    public String getIp() {
        if (message == null) return "";
        NetworkInfo net = message.getNet();
        if (net == null) return "";
        List<NetworkInfo.NetworkInterface> info = net.getInfo();
        if (info == null) return "";
        NetworkInfo.NetworkInterface first = info.getFirst();
        if (first == null) return "";
        Long ip = first.getIp();
        if (ip == null) return "";
        return NetworkUtils.getIpAddressFromInt(ip);
    }
}