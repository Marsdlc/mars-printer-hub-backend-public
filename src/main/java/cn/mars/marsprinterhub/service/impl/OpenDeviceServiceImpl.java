package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.model.AmsInstance;
import cn.mars.marsprinterhub.bambu.model.AmsStatus;
import cn.mars.marsprinterhub.bambu.model.AmsTray;
import cn.mars.marsprinterhub.bambu.model.DeviceStatus;
import cn.mars.marsprinterhub.bambu.model.LightInfo;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttMessage;
import cn.mars.marsprinterhub.bambu.service.HmsErrorService;
import cn.mars.marsprinterhub.constant.RedisConstants;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.TaskCoverEvent;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.enums.LedNodeEnum;
import cn.mars.marsprinterhub.model.enums.PrinterModelEnum;
import cn.mars.marsprinterhub.model.vo.FanStatusVO;
import cn.mars.marsprinterhub.model.vo.OpenDeviceInfoVO;
import cn.mars.marsprinterhub.service.OpenDeviceService;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCache;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import cn.mars.marsprinterhub.utils.BambuConvertUtils;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.mars.marsprinterhub.constant.BambuConstants.TRAY_EXTERNAL;
import static cn.mars.marsprinterhub.constant.BambuConstants.TRAY_NONE;
import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_TASK_PREFIX;

@Service
public class OpenDeviceServiceImpl implements OpenDeviceService {

    @Resource
    private PrinterService printerService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private HmsErrorService hmsErrorService;

    @Override
    public OpenDeviceInfoVO getDeviceInfo(Long userId, String deviceId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "Open API 调用身份缺失");
        ThrowUtils.throwIf(StrUtil.isBlank(deviceId), ErrorCode.PARAMS_ERROR, "设备 ID 不能为空");

        Long cachedUserId = redisUtils.getLong(RedisConstants.DEVICE_USER_PREFIX + deviceId);
        ThrowUtils.throwIf(cachedUserId == null, ErrorCode.NO_AUTH_ERROR, "无权访问该设备");
        ThrowUtils.throwIf(!Objects.equals(cachedUserId, userId), ErrorCode.NO_AUTH_ERROR, "无权访问该设备");

        Printer printer = printerService.getByDeviceId(deviceId);
        ThrowUtils.throwIf(printer == null || !Objects.equals(printer.getUserId(), userId),
                ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        ThrowUtils.throwIf(!Objects.equals(cachedUserId, printer.getUserId()), ErrorCode.NO_AUTH_ERROR, "设备归属校验不一致");

        OpenDeviceInfoVO info = new OpenDeviceInfoVO();
        DeviceStatusCache deviceStatus = deviceStatusCacheService.getDeviceStatus(deviceId);
        fillPrinterSummary(info, printer, deviceStatus, deviceId);
        if (deviceStatus == null) {
            return info;
        }

        BambuMqttMessage message = deviceStatus.getMessage();
        if (message != null) {
            info.setChamberLight(getLightMode(message, LedNodeEnum.CHAMBER_LIGHT.getValue()));
            info.setChamberLight2(getLightMode(message, LedNodeEnum.CHAMBER_LIGHT2.getValue()));
            info.setWorkLight(getLightMode(message, LedNodeEnum.WORK_LIGHT.getValue()));
            info.setFans(buildFanStatus(message));
            info.setBedTemp(getSafeBedTemp(message));
            info.setChamberTemp(getSafeChamberTemp(message));
            info.setLayerNum(deviceStatus.getCurrentLayerNum());
            info.setTotalLayerNum(deviceStatus.getTotalLayerNum());
            info.setTrayType(resolveActiveTrayType(message));
            fillNozzleTemperature(info, message);
            info.setHmsErrors(hmsErrorService.resolveErrors(message.getHms(), printer.getModel()));
        }
        return info;
    }

    private FanStatusVO buildFanStatus(BambuMqttMessage message) {
        if (message == null) {
            return null;
        }
        FanStatusVO vo = new FanStatusVO();
        vo.setCoolingFanSpeedPercentage(message.getCoolingFanSpeedPercentage());
        vo.setAuxFanSpeedPercentage(message.getAuxFanSpeedPercentage());
        vo.setChamberFanSpeedPercentage(message.getChamberFanSpeedPercentage());
        vo.setSecondaryAuxFanSpeedPercentage(message.getSecondaryAuxFanSpeedPercentage());
        return vo;
    }

    private void fillNozzleTemperature(OpenDeviceInfoVO info, BambuMqttMessage message) {
        BambuConvertUtils.NozzleTemperatureInfo nozzleTemperatureInfo =
                BambuConvertUtils.getNozzleTemperatureInfo(message);
        info.setActiveNozzle(nozzleTemperatureInfo.getActiveNozzle());
        info.setNozzleTemp(nozzleTemperatureInfo.getNozzleTemp());
        info.setTargetNozzleTemp(nozzleTemperatureInfo.getTargetNozzleTemp());
        info.setLeftNozzleTemp(nozzleTemperatureInfo.getLeftNozzleTemp());
        info.setLeftTargetNozzleTemp(nozzleTemperatureInfo.getLeftTargetNozzleTemp());
        info.setRightNozzleTemp(nozzleTemperatureInfo.getRightNozzleTemp());
        info.setRightTargetNozzleTemp(nozzleTemperatureInfo.getRightTargetNozzleTemp());
    }

    private void fillPrinterSummary(OpenDeviceInfoVO info, Printer printer, DeviceStatusCache deviceStatus, String deviceId) {
        info.setName(printer.getName());
        info.setDeviceId(deviceId);
        PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(printer.getModel());
        String redisTaskKey = DEVICE_TASK_PREFIX + deviceId;
        info.setModel(printerModelEnum != null ? printerModelEnum.getText() : printer.getModel());
        if (deviceStatus == null) {
            return;
        }
        info.setPrintStatus(deviceStatus.getPrintStatus());
        info.setPrintProgress(deviceStatus.getPrintProgress());
        Object taskImage = redisUtils.get(redisTaskKey);
        if (taskImage == null && deviceStatus.isPrintStarted()) {
            eventPublisher.publish(new TaskCoverEvent(deviceId));
        }
        info.setTaskImage(Objects.toString(taskImage, null));
        info.setRemainingTime(deviceStatus.getRemainingTime());
    }

    private String getLightMode(BambuMqttMessage message, String ledNode) {
        if (message == null || message.getLightsReport() == null) {
            return "off";
        }
        return message.getLightsReport().stream()
                .filter(lightInfo -> ledNode.equals(lightInfo.getNode()))
                .findFirst()
                .map(LightInfo::getMode)
                .orElse("off");
    }

    private int getSafeBedTemp(BambuMqttMessage message) {
        if (message == null) {
            return 0;
        }
        Integer temp = message.getBedTemper();
        return temp != null ? temp : 0;
    }

    private int getSafeChamberTemp(BambuMqttMessage message) {
        if (message == null
                || message.getDevice() == null
                || message.getDevice().getCtc() == null
                || message.getDevice().getCtc().getInfo() == null) {
            return 0;
        }
        Integer temp = message.getDevice().getCtc().getInfo().getTemp();
        return temp != null ? temp : 0;
    }

    private String resolveActiveTrayType(BambuMqttMessage message) {
        if (message == null || message.getAms() == null) {
            return null;
        }
        ActiveTrayRef activeTrayRef = resolveActiveTrayRef(message);
        if (activeTrayRef == null) {
            return null;
        }
        if (Objects.equals(activeTrayRef.amsNo, TRAY_EXTERNAL)) {
            AmsTray vtTray = message.getVtTray();
            return vtTray == null ? null : vtTray.getTrayType();
        }

        AmsStatus amsStatus = message.getAms();
        List<AmsInstance> amsList = amsStatus.getAmsList();
        if (amsList == null || amsList.isEmpty()) {
            return null;
        }
        for (AmsInstance amsInstance : amsList) {
            Integer amsNo = parseInteger(amsInstance.getId());
            if (!Objects.equals(amsNo, activeTrayRef.amsNo) || amsInstance.getTray() == null) {
                continue;
            }
            for (AmsTray tray : amsInstance.getTray()) {
                Integer slotNo = parseInteger(tray.getId());
                if (Objects.equals(slotNo, activeTrayRef.slotNo)) {
                    return tray.getTrayType();
                }
            }
        }
        return null;
    }

    private ActiveTrayRef resolveActiveTrayRef(BambuMqttMessage message) {
        Integer totalNozzles = BambuConvertUtils.getTotalNozzles(message);
        if (totalNozzles != null && message.getDevice() != null && message.getDevice().getExtruder() != null) {
            DeviceStatus.Extruder extruder = message.getDevice().getExtruder();
            Integer state = extruder.getState();
            List<DeviceStatus.Extruder.ExtruderInfo> extruderInfos = extruder.getInfo();
            if (state != null && extruderInfos != null && !extruderInfos.isEmpty()) {
                int actionNozzle = BambuConvertUtils.getActionNozzle(state);
                if (actionNozzle < 0 || actionNozzle >= extruderInfos.size()) {
                    actionNozzle = 0;
                }
                Integer snow = extruderInfos.get(actionNozzle).getSnow();
                Integer activeAmsNo = BambuConvertUtils.getActiveAmsNoBySnow(snow);
                Integer activeSlotNo = BambuConvertUtils.getActiveSlotNoBySnow(snow);
                if (!Objects.equals(activeAmsNo, TRAY_NONE)) {
                    return new ActiveTrayRef(activeAmsNo, activeSlotNo);
                }
            }
        }
        Integer trayNow = message.getAms().getTrayNow();
        if (trayNow == null) {
            return null;
        }
        Integer activeAmsNo = BambuConvertUtils.getActiveAmsNo(trayNow);
        Integer activeSlotNo = BambuConvertUtils.getActiveAmsSlotNo(trayNow);
        if (Objects.equals(activeAmsNo, TRAY_NONE)) {
            return null;
        }
        return new ActiveTrayRef(activeAmsNo, activeSlotNo);
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record ActiveTrayRef(Integer amsNo, Integer slotNo) {
    }
}
