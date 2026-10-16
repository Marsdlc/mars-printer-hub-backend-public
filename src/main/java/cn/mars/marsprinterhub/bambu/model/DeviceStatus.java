package cn.mars.marsprinterhub.bambu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 只保留 device.snow 与 ctc 相关信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceStatus {

    private Extruder extruder;

    private Ctc ctc;

    private Airduct airduct;

    public DeviceStatus merge(DeviceStatus other) {
        if (other == null) {
            return this;
        }

        DeviceStatus merged = new DeviceStatus();
        merged.setExtruder(this.extruder != null ? this.extruder.merge(other.extruder) : other.extruder);
        merged.setCtc(this.ctc != null ? this.ctc.merge(other.ctc) : other.ctc);
        merged.setAirduct(this.airduct != null ? this.airduct.merge(other.airduct) : other.airduct);
        return merged;
    }

    // ========================= Extruder =========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Extruder {

        private List<ExtruderInfo> info;

        private Integer state;

        public Extruder merge(Extruder other) {
            if (other == null) {
                return this;
            }

            Extruder merged = new Extruder();
            merged.setInfo(mergeSnowList(this.info, other.info));
            merged.setState(other.state != null ? other.state : this.state);
            return merged;
        }

        private static List<ExtruderInfo> mergeSnowList(List<ExtruderInfo> existing, List<ExtruderInfo> incoming) {
            if (existing != null || incoming != null) {
                List<ExtruderInfo> merged = new ArrayList<>();
                int maxSize = Math.max(
                        existing != null ? existing.size() : 0,
                        incoming != null ? incoming.size() : 0
                );

                for (int i = 0; i < maxSize; i++) {
                    ExtruderInfo current = existing != null && i < existing.size() ? existing.get(i) : null;
                    ExtruderInfo next = incoming != null && i < incoming.size() ? incoming.get(i) : null;

                    if (current != null) {
                        merged.add(current.merge(next));
                    } else if (next != null) {
                        merged.add(next);
                    }
                }
                return merged;
            }
            return null;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ExtruderInfo {

            private Integer id;

            private Integer snow;

            /**
             * 新版 MQTT 字段：低 16 位为当前喷嘴温度，高 16 位为目标喷嘴温度。
             */
            private Integer temp;

            public ExtruderInfo merge(ExtruderInfo other) {
                if (other == null) {
                    return this;
                }

                ExtruderInfo merged = new ExtruderInfo();
                merged.setId(other.id != null ? other.id : this.id);
                merged.setSnow(other.snow != null ? other.snow : this.snow);
                merged.setTemp(other.temp != null ? other.temp : this.temp);
                return merged;
            }
        }
    }

    // ========================= CTC =========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ctc {

        private CtcInfo info;

        private Integer state;

        public Ctc merge(Ctc other) {
            if (other == null) {
                return this;
            }

            Ctc merged = new Ctc();
            merged.setState(other.state != null ? other.state : this.state);
            merged.setInfo(this.info != null ? this.info.merge(other.info) : other.info);
            return merged;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CtcInfo {

            private Integer temp;

            public CtcInfo merge(CtcInfo other) {
                if (other == null) {
                    return this;
                }

                CtcInfo merged = new CtcInfo();
                merged.setTemp(other.temp != null ? other.temp : this.temp);
                return merged;
            }
        }
    }

    // ========================= Airduct =========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Airduct {

        private List<AirductPart> parts;

        private Integer state;

        public Airduct merge(Airduct other) {
            if (other == null) {
                return this;
            }

            Airduct merged = new Airduct();
            merged.setParts(mergeParts(this.parts, other.parts));
            merged.setState(other.state != null ? other.state : this.state);
            return merged;
        }

        private static List<AirductPart> mergeParts(List<AirductPart> existing, List<AirductPart> incoming) {
            if (existing != null || incoming != null) {
                List<AirductPart> merged = new ArrayList<>();
                int maxSize = Math.max(
                        existing != null ? existing.size() : 0,
                        incoming != null ? incoming.size() : 0
                );

                for (int i = 0; i < maxSize; i++) {
                    AirductPart current = existing != null && i < existing.size() ? existing.get(i) : null;
                    AirductPart next = incoming != null && i < incoming.size() ? incoming.get(i) : null;

                    if (current != null) {
                        merged.add(current.merge(next));
                    } else if (next != null) {
                        merged.add(next);
                    }
                }
                return merged;
            }
            return null;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AirductPart {

            private Integer id;

            /**
             * 风道部件状态。对 id=160 的 secondary auxiliary fan，该值已经是百分比。
             */
            private Integer state;

            public AirductPart merge(AirductPart other) {
                if (other == null) {
                    return this;
                }

                AirductPart merged = new AirductPart();
                merged.setId(other.id != null ? other.id : this.id);
                merged.setState(other.state != null ? other.state : this.state);
                return merged;
            }
        }
    }
}
