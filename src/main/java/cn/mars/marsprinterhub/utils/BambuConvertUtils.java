package cn.mars.marsprinterhub.utils;

import cn.mars.marsprinterhub.bambu.model.DeviceStatus;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttMessage;
import lombok.Data;

import java.util.List;

import static cn.mars.marsprinterhub.constant.BambuConstants.TRAY_EXTERNAL;
import static cn.mars.marsprinterhub.constant.BambuConstants.TRAY_NONE;

public class BambuConvertUtils {

    private static final int RIGHT_NOZZLE_INDEX = 0;
    private static final int LEFT_NOZZLE_INDEX = 1;

    public static Integer getTotalNozzles(BambuMqttMessage message) {
        try {
            Integer state = message.getDevice().getExtruder().getState();
            if (state == null) {
                return null;
            }

            int low8 = state & 0xFF;
            return low8 & 0xF;
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer getActionNozzle(Integer state) {
        // 取低 8 位
        int low8 = state & 0xFF;

        // 次低 4 位 -> 活动喷嘴索引
        return (low8 >> 4) & 0xF;
    }

    public static NozzleTemperatureInfo getNozzleTemperatureInfo(BambuMqttMessage message) {
        NozzleTemperatureInfo info = new NozzleTemperatureInfo();
        if (message == null) {
            return info;
        }

        if (fillNozzleTemperatureFromExtruder(message, info)) {
            return info;
        }

        info.setNozzleTemp(roundTemperature(message.getNozzleTemper()));
        info.setTargetNozzleTemp(roundTemperature(message.getNozzleTargetTemper()));
        if (info.getNozzleTemp() != null || info.getTargetNozzleTemp() != null) {
            info.setActiveNozzle("single");
        }
        return info;
    }

    private static boolean fillNozzleTemperatureFromExtruder(BambuMqttMessage message, NozzleTemperatureInfo info) {
        DeviceStatus device = message.getDevice();
        if (device == null || device.getExtruder() == null) {
            return false;
        }

        List<DeviceStatus.Extruder.ExtruderInfo> extruderInfos = device.getExtruder().getInfo();
        if (extruderInfos == null || extruderInfos.isEmpty()) {
            return false;
        }

        boolean hasTemperature = false;
        for (int i = 0; i < extruderInfos.size(); i++) {
            DeviceStatus.Extruder.ExtruderInfo extruderInfo = extruderInfos.get(i);
            if (extruderInfo == null || extruderInfo.getTemp() == null) {
                continue;
            }

            int nozzleIndex = extruderInfo.getId() != null ? extruderInfo.getId() : i;
            Integer currentTemp = unpackCurrentTemperature(extruderInfo.getTemp());
            Integer targetTemp = unpackTargetTemperature(extruderInfo.getTemp());
            if (nozzleIndex == RIGHT_NOZZLE_INDEX) {
                info.setRightNozzleTemp(currentTemp);
                info.setRightTargetNozzleTemp(targetTemp);
            } else if (nozzleIndex == LEFT_NOZZLE_INDEX) {
                info.setLeftNozzleTemp(currentTemp);
                info.setLeftTargetNozzleTemp(targetTemp);
            }
            hasTemperature = true;
        }

        if (!hasTemperature) {
            return false;
        }

        Integer activeNozzleIndex = resolveActiveNozzleIndex(device.getExtruder());
        if (activeNozzleIndex != null && activeNozzleIndex == LEFT_NOZZLE_INDEX) {
            info.setNozzleTemp(info.getLeftNozzleTemp());
            info.setTargetNozzleTemp(info.getLeftTargetNozzleTemp());
            info.setActiveNozzle("left");
        } else {
            info.setNozzleTemp(info.getRightNozzleTemp());
            info.setTargetNozzleTemp(info.getRightTargetNozzleTemp());
            info.setActiveNozzle("right");
        }
        return true;
    }

    private static Integer resolveActiveNozzleIndex(DeviceStatus.Extruder extruder) {
        Integer state = extruder.getState();
        if (state == null) {
            return RIGHT_NOZZLE_INDEX;
        }
        return getActionNozzle(state);
    }

    private static Integer unpackCurrentTemperature(Integer packedTemperature) {
        return packedTemperature == null ? null : packedTemperature & 0xFFFF;
    }

    private static Integer unpackTargetTemperature(Integer packedTemperature) {
        return packedTemperature == null ? null : (packedTemperature >> 16) & 0xFFFF;
    }

    private static Integer roundTemperature(Double temperature) {
        return temperature == null ? null : Math.toIntExact(Math.round(temperature));
    }

    /**
     * 从 snow 值获取活动的 AMS 编号（新版设备使用）
     * snow 格式: 高8位=amsIndex, 低2位=trayIndex
     *
     * @param snow extruder.info[x].snow 的值
     * @return AMS编号，255=外置耗材架1，254=外置耗材架2，0-3=普通AMS，128+=AMS HT
     */
    public static Integer getActiveAmsNoBySnow(Integer snow) {
        if (snow == null) {
            return TRAY_NONE;
        }
        int amsIndex = snow >> 8;
        int trayIndex = snow & 0x3;

        // 外置耗材架1: amsIndex=255, trayIndex=0
        if (amsIndex == 255) {
            return TRAY_NONE; // 255都统一表示外置/无活动
        }
        // 外置耗材架2: amsIndex=254, trayIndex=0
        if (amsIndex == 254) {
            return (trayIndex == 0) ? TRAY_EXTERNAL : TRAY_NONE;
        }
        return amsIndex;
    }

    /**
     * 从 snow 值获取活动的槽位编号（新版设备使用）
     *
     * @param snow extruder.info[x].snow 的值
     * @return 槽位编号，0-3=具体槽位，TRAY_NONE=无槽位
     */
    public static Integer getActiveSlotNoBySnow(Integer snow) {
        if (snow == null) {
            return TRAY_NONE;
        }
        int amsIndex = snow >> 8;
        int trayIndex = snow & 0x3;

        // 外置耗材架1激活: amsIndex=255, trayIndex=0
        if (amsIndex == 255 && trayIndex == 0) {
            return 0;
        }
        // 外置耗材架2激活: amsIndex=254, trayIndex=0
        if (amsIndex == 254 && trayIndex == 0) {
            return 0;
        }
        // 无槽位激活 (如 0xFFFF 或 0xFEFF)
        if (amsIndex >= 254 && trayIndex != 0) {
            return TRAY_NONE;
        }
        return trayIndex;
    }

    /**
     * 判断是否为外置耗材架激活（新版设备使用）
     *
     * @param snow extruder.info[x].snow 的值
     * @return true=外置耗材架激活，false=AMS激活或无激活
     */
    public static boolean isExternalSpoolActiveBySnow(Integer snow) {
        if (snow == null) {
            return false;
        }
        int amsIndex = snow >> 8;
        int trayIndex = snow & 0x3;
        // 外置耗材架1: amsIndex=255, trayIndex=0
        // 外置耗材架2: amsIndex=254, trayIndex=0
        return (amsIndex == 255 || amsIndex == 254) && trayIndex == 0;
    }

    /**
     * 获取活动的槽位编号（旧版设备使用）
     */
    public static Integer getActiveAmsSlotNo(Integer trayNow) {
        if (trayNow == null || trayNow.equals(TRAY_NONE) || trayNow.equals(TRAY_EXTERNAL)) {
            return trayNow;
        }
        return trayNow & 0x3;
    }

    public static Integer getActiveAmsNo(Integer trayNow) {
        if (trayNow.equals(TRAY_NONE) || trayNow.equals(TRAY_EXTERNAL)) {
            return trayNow;
        }
        return trayNow >> 2;
    }

    public static String convertAmsName(int amsNo, boolean hasMore) {
        if (amsNo == 254) {
            return "外挂料盘" + (hasMore ? "（左）" : "");
        }
        if (amsNo == 255) {
            return "外挂料盘" + (hasMore ? "（右）" : "");
        }
        if (amsNo >= 0 && amsNo <= 3) {
            // A, B, C, D
            char letter = (char) ('A' + amsNo);
            return "AMS " + letter;
        }
        // https://wiki.bambulab.com/zh/ams/manual/multi-model-AMS-compatibility-guide
        if (amsNo >= 128 && amsNo <= 136) {
            char letter = (char) ('A' + amsNo - 128);
            return "HT " + letter;
        }

        // 兜底
        return "AMS " + amsNo;
    }

    @Data
    public static class NozzleTemperatureInfo {
        /**
         * 活动喷嘴：single/left/right。
         */
        private String activeNozzle;
        private Integer nozzleTemp;
        private Integer targetNozzleTemp;
        private Integer leftNozzleTemp;
        private Integer leftTargetNozzleTemp;
        private Integer rightNozzleTemp;
        private Integer rightTargetNozzleTemp;
    }
}
