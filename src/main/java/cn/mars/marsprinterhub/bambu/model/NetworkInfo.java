package cn.mars.marsprinterhub.bambu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 网络信息类
 * 对应MQTT消息中的net字段
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkInfo {

    @JsonProperty("conf")
    private Integer conf;

    @JsonProperty("info")
    private List<NetworkInterface> info;

    public NetworkInfo merge(NetworkInfo other) {
        if (other == null) return this;

        NetworkInfo merged = new NetworkInfo();
        merged.setConf(mergeValue(this.conf, other.conf));

        if (this.info != null || other.info != null) {
            List<NetworkInterface> mergedInfo = new ArrayList<>();
            int maxSize = Math.max(
                    this.info != null ? this.info.size() : 0,
                    other.info != null ? other.info.size() : 0
            );

            for (int i = 0; i < maxSize; i++) {
                NetworkInterface existing = this.info != null && i < this.info.size() ? this.info.get(i) : null;
                NetworkInterface newNet = other.info != null && i < other.info.size() ? other.info.get(i) : null;

                if (existing != null) {
                    mergedInfo.add(existing.merge(newNet));
                } else if (newNet != null) {
                    mergedInfo.add(newNet);
                }
            }
            merged.setInfo(mergedInfo);
        }

        return merged;
    }

    private static <T> T mergeValue(T existing, T newValue) {
        return newValue != null ? newValue : existing;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NetworkInterface {

        @JsonProperty("ip")
        private Long ip;

        @JsonProperty("mask")
        private Long mask;

        public NetworkInterface merge(NetworkInterface other) {
            if (other == null) return this;

            NetworkInterface merged = new NetworkInterface();
            merged.setIp(mergeValue(this.ip, other.ip));
            merged.setMask(mergeValue(this.mask, other.mask));
            return merged;
        }

        private static <T> T mergeValue(T existing, T newValue) {
            return newValue != null ? newValue : existing;
        }
    }
}