package cn.mars.marsprinterhub.bambu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AMS实例信息
 * 每个AMS单元的详细状态
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmsInstance {

    @JsonProperty("id")
    private String id; // AMS索引

    @JsonProperty("humidity")
    private String humidity; // 湿度指数

    @JsonProperty("humidity_raw")
    private Integer humidityRaw; // 原始湿度值

    @JsonProperty("temp")
    private String temp; // 温度

    @JsonProperty("tray")
    private List<AmsTray> tray; // 料盘数组

    @JsonProperty("dry_setting")
    private DrySetting drySetting; // 烘干设置

    @JsonProperty("dry_time")
    private Integer dryTime; // 烘干剩余时间（分钟）

    @JsonProperty("info")
    private Object info; // 信息字段

    public AmsInstance merge(AmsInstance other) {
        if (other == null) {
            return this;
        }

        AmsInstance merged = new AmsInstance();
        merged.setId(mergeValue(this.id, other.id));
        merged.setHumidity(mergeValue(this.humidity, other.humidity));
        merged.setHumidityRaw(mergeValue(this.humidityRaw, other.humidityRaw));
        merged.setTemp(mergeValue(this.temp, other.temp));
        merged.setDrySetting(mergeValue(this.drySetting, other.drySetting));
        merged.setDryTime(mergeValue(this.dryTime, other.dryTime));
        merged.setInfo(mergeValue(this.info, other.info));

        // 合并 tray 列表
        if (this.tray != null || other.tray != null) {
            List<AmsTray> mergedTrays = new ArrayList<>();
            int maxSize = Math.max(
                    this.tray != null ? this.tray.size() : 0,
                    other.tray != null ? other.tray.size() : 0
            );

            for (int i = 0; i < maxSize; i++) {
                AmsTray existingTray = this.tray != null && i < this.tray.size() ? this.tray.get(i) : null;
                AmsTray newTray = other.tray != null && i < other.tray.size() ? other.tray.get(i) : null;

                if (existingTray != null) {
                    mergedTrays.add(existingTray.merge(newTray));
                } else if (newTray != null) {
                    mergedTrays.add(newTray);
                }
            }
            merged.setTray(mergedTrays);
        }

        return merged;
    }

    private static <T> T mergeValue(T existing, T newValue) {
        return newValue != null ? newValue : existing;
    }
}