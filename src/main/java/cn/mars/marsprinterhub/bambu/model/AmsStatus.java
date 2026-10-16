package cn.mars.marsprinterhub.bambu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AMS（自动材料系统）状态信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmsStatus {

    @JsonProperty("ams")
    private List<AmsInstance> amsList; // AMS实例列表

    @JsonProperty("tray_now")
    private Integer trayNow; // 当前活动料盘索引（255=无活动，254=外部线轴）

    @JsonProperty("tray_tar")
    private String trayTar; // 目标料盘索引

    @JsonProperty("power_on_flag")
    private Boolean powerOnFlag; // 开机标志

    public AmsStatus merge(AmsStatus other) {
        if (other == null) return this;

        AmsStatus merged = new AmsStatus();
        merged.setTrayNow(mergeValue(this.trayNow, other.trayNow));
        merged.setTrayTar(mergeValue(this.trayTar, other.trayTar));
        merged.setPowerOnFlag(mergeValue(this.powerOnFlag, other.powerOnFlag));

        // 合并 amsList
        if (this.amsList != null || other.amsList != null) {
            List<AmsInstance> mergedAms = new ArrayList<>();
            int maxSize = Math.max(
                    this.amsList != null ? this.amsList.size() : 0,
                    other.amsList != null ? other.amsList.size() : 0
            );

            for (int i = 0; i < maxSize; i++) {
                AmsInstance existing = this.amsList != null && i < this.amsList.size() ? this.amsList.get(i) : null;
                AmsInstance newInst = other.amsList != null && i < other.amsList.size() ? other.amsList.get(i) : null;

                if (existing != null) {
                    mergedAms.add(existing.merge(newInst));
                } else if (newInst != null) {
                    mergedAms.add(newInst);
                }
            }
            merged.setAmsList(mergedAms);
        }

        return merged;
    }

    private static <T> T mergeValue(T existing, T newValue) {
        return newValue != null ? newValue : existing;
    }
}