package cn.mars.marsprinterhub.bambu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LightInfo {

    @JsonProperty("node")
    private String node;

    @JsonProperty("mode")
    private String mode;

    public LightInfo merge(LightInfo other) {
        if (other == null) {
            return this;
        }

        LightInfo merged = new LightInfo();
        merged.setNode(mergeValue(this.node, other.node));
        merged.setMode(mergeValue(this.mode, other.mode));
        return merged;
    }

    private static <T> T mergeValue(T existing, T newValue) {
        return newValue != null ? newValue : existing;
    }
}
