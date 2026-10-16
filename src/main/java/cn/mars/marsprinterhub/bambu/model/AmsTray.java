package cn.mars.marsprinterhub.bambu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AMS料盘信息
 * 包含每个料盘的材料详细信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmsTray {

    @JsonProperty("id")
    private String id; // 料盘ID

    @JsonProperty("remain")
    private Integer remain; // 剩余材料长度

    @JsonProperty("tray_info_idx")
    private String trayInfoIdx; // 材料信息索引

    @JsonProperty("tray_type")
    private String trayType; // 材料类型（如PLA, ABS）

    @JsonProperty("tray_color")
    private String trayColor; // 颜色（RRGGBBAA格式）

    @JsonProperty("nozzle_temp_max")
    private String nozzleTempMax; // 最大喷嘴温度

    @JsonProperty("nozzle_temp_min")
    private String nozzleTempMin; // 最小喷嘴温度

    @JsonProperty("state")
    private Integer state; // 状态字段

    public AmsTray merge(AmsTray other) {
        if (other == null) {
            return this;
        }

        AmsTray merged = new AmsTray();
        merged.setId(mergeValue(this.id, other.id));
        merged.setState(mergeValue(this.state, other.state));

        // 空托盘：保留 id 和 state，但其他字段设为空或默认
        if (other.isEmptyTray()) {
            merged.setRemain(null);
            merged.setTrayInfoIdx(null);
            merged.setTrayType(null);
            merged.setTrayColor(null);
            merged.setNozzleTempMax(null);
            merged.setNozzleTempMin(null);
            return merged;
        }

        // 正常托盘：执行完整 merge
        merged.setRemain(mergeValue(this.remain, other.remain));
        merged.setTrayInfoIdx(mergeValue(this.trayInfoIdx, other.trayInfoIdx));
        merged.setTrayType(mergeValue(this.trayType, other.trayType));
        merged.setTrayColor(mergeValue(this.trayColor, other.trayColor));
        merged.setNozzleTempMax(mergeValue(this.nozzleTempMax, other.nozzleTempMax));
        merged.setNozzleTempMin(mergeValue(this.nozzleTempMin, other.nozzleTempMin));

        return merged;
    }

    private static <T> T mergeValue(T existing, T newValue) {
        return newValue != null ? newValue : existing;
    }

    /**
     * 判断是否为空托盘（没有装料）
     */
    public boolean isEmptyTray() {
        return this.trayInfoIdx == null;
    }
}