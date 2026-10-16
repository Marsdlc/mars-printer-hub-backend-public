package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.dto.BambuDeviceDTO;
import cn.mars.marsprinterhub.bambu.model.*;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttMessage;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.bambu.service.BambuImageService;
import cn.mars.marsprinterhub.bambu.service.BambuVideoService;
import cn.mars.marsprinterhub.bambu.service.HmsErrorService;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.TaskCoverEvent;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.manager.StreamConnectionManager;
import cn.mars.marsprinterhub.model.dto.ams.AmsBindRequest;
import cn.mars.marsprinterhub.model.dto.ams.AmsEntityUpdateDTO;
import cn.mars.marsprinterhub.model.entity.*;
import cn.mars.marsprinterhub.model.enums.LedNodeEnum;
import cn.mars.marsprinterhub.model.enums.PrinterModelEnum;
import cn.mars.marsprinterhub.model.vo.*;
import cn.mars.marsprinterhub.service.*;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCache;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import cn.mars.marsprinterhub.utils.BambuConvertUtils;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.mars.marsprinterhub.constant.BambuConstants.TRAY_EXTERNAL;
import static cn.mars.marsprinterhub.constant.BambuConstants.TRAY_NONE;
import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_TASK_PREFIX;
import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;

@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private UserService userService;

    @Resource
    private SpoolService spoolService;

    @Resource
    private PrinterService printerService;

    @Resource
    private AmsService amsService;

    @Resource
    private AmsSlotsService amsSlotsService;

    @Resource
    private BambuMqttService bambuMqttService;

    @Resource
    private BambuCloudService bambuCloudService;

    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private BambuImageService bambuImageService;

    @Resource
    private BambuVideoService bambuVideoService;

    @Resource
    private HmsErrorService hmsErrorService;

    @Resource
    private StreamConnectionManager streamConnectionManager;

    @Resource
    private HomeAssistantService homeAssistantService;

    @Override
    public boolean syncPrintersFromBambu() {
        // 1. 获取当前用户
        User currentUser = userService.getCurrentUser();
        Long currentUserId = currentUser.getId();

        // 2. 从拓竹API获取用户的所有打印机
        List<BambuDeviceDTO> bambuDevices = bambuCloudService.getUserDevices();
        ThrowUtils.throwIf(bambuDevices == null, ErrorCode.NOT_FOUND_ERROR, "获取打印机数据失败");

        List<Printer> syncPrinterCandidates = buildSyncPrinterCandidates(bambuDevices, currentUserId);

        // 3. 循环
        for (BambuDeviceDTO bambuDevice : bambuDevices) {
            try {
                String deviceId = bambuDevice.getDeviceId();
                DeviceStatusCache deviceStatus = null;
                // 3.1 同步场景按自动连接列表建立 mqtt 连接，未入选设备只同步基础信息
                if (bambuMqttService.isConnected(deviceId) ||
                        bambuMqttService.doAutoConnect(deviceId, currentUser, syncPrinterCandidates)) {
                    // 3.2 发起 mqtt 请求，获取设备的完整信息
                    bambuMqttService.pushAllStatus(deviceId);
                    // 等待2秒，等待 mqtt 推送数据
                    Thread.sleep(2000);
                    deviceStatus = deviceStatusCacheService.getDeviceStatus(deviceId);
                }
                // 3.3 同步设备信息到数据库
                Printer printer = syncSingleDevice(bambuDevice, currentUserId, deviceStatus);
                if (printer == null || deviceStatus == null) {
                    continue;
                }
                // 3.4 同步 ams 信息到数据库
                amsService.syncAmsInfo(printer.getId(), currentUserId, deviceStatus.getAmsStatus());
                // 3.5 同步外挂线轴
                amsService.syncAmsVtTray(printer.getId(), currentUserId, deviceStatus.getMessage());
                // 3.6 同步虚拟线轴
                amsService.syncAmsVirSlot(printer.getId(), currentUserId, deviceStatus.getMessage());
            } catch (Exception e) {
                log.warn("同步设备失败，跳过: deviceId={}", bambuDevice.getDeviceId(), e);
            }
        }
        return true;
    }

    private List<Printer> buildSyncPrinterCandidates(List<BambuDeviceDTO> bambuDevices, Long userId) {
        if (CollUtil.isEmpty(bambuDevices)) {
            return Collections.emptyList();
        }
        return bambuDevices.stream()
                .filter(device -> device != null && StrUtil.isNotBlank(device.getDeviceId()))
                .map(device -> {
                    Printer printer = new Printer();
                    printer.setDeviceId(device.getDeviceId());
                    printer.setUserId(userId);
                    printer.setName(device.getName());
                    printer.setModel(device.getDeviceModelName());
                    printer.setAccessCode(device.getDeviceAccessCode());
                    return printer;
                })
                .toList();
    }

    @Override
    public HomeAssistantEntityStateVO getHomeAssistantEntityState(String entityId) {
        return homeAssistantService.getEntityState(entityId);
    }

    @Override
    public HomeAssistantEntityStateVO getPrinterPower(Long printerId) {
        ThrowUtils.throwIf(printerId == null, ErrorCode.PARAMS_ERROR, "打印机 ID 不能为空");
        Long currentUserId = userService.getCurrentUserId();
        Printer printer = printerService.getById(printerId);
        ThrowUtils.throwIf(printer == null || !Objects.equals(printer.getUserId(), currentUserId),
                ErrorCode.NOT_FOUND_ERROR, "打印机不存在");
        if (StrUtil.isBlank(printer.getPowerEntityId())) {
            return null;
        }
        try {
            return homeAssistantService.getEntityState(printer.getPowerEntityId());
        } catch (Exception e) {
            log.warn("获取打印机功率信息失败: printerId={}, entityId={}", printer.getId(), printer.getPowerEntityId(), e);
            return null;
        }
    }

    @Override
    public boolean updateAmsEntities(AmsEntityUpdateDTO request) {
        amsService.updateHomeAssistantEntityIds(
                request.getAmsId(),
                request.getTemperatureEntityId(),
                request.getHumidityEntityId()
        );
        return true;
    }

    private Printer syncSingleDevice(BambuDeviceDTO bambuDevice, Long userId, DeviceStatusCache deviceStatus) {
        String deviceId = bambuDevice.getDeviceId();

        // 只根据 deviceId 查询设备
        LambdaQueryWrapper<Printer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Printer::getDeviceId, deviceId);

        Printer printer = printerService.getOne(queryWrapper);

        if (printer == null) {
            // 数据库不存在该设备，创建新记录
            printer = new Printer();
            printer.setDeviceId(deviceId);
            printer.setUserId(userId);
        } else {
            // 设备已存在，允许换绑用户
            Long oldUserId = printer.getUserId();
            if (!userId.equals(oldUserId)) {
                printer.setUserId(userId);
                // 删除所属 ams
                amsService.removeByUserIdAndPrinterId(oldUserId, printer.getId());
            }
        }

        // 更新设备信息
        printer.setName(bambuDevice.getName());
        printer.setModel(bambuDevice.getDeviceModelName());
        printer.setAccessCode(bambuDevice.getDeviceAccessCode());

        if (deviceStatus != null) {
            printer.setIpAddress(deviceStatus.getIp());
        }

        // 让数据库自动更新 update_time
        printer.setUpdateTime(null);

        boolean success = printerService.saveOrUpdate(printer);
        if (success) {
            redisUtils.set(DEVICE_USER_PREFIX + deviceId, userId);
        }

        return success ? printer : null;
    }

    @Override
    public List<Printer> getDeviceList() {
        // 1. 获取当前登录用户
        Long userId = userService.getCurrentUserId();
        // 2. 获取该用户下所有的打印机
        return printerService.getListByUserId(userId);
    }

    @Override
    public DeviceSimpleVO convertToVO(Printer printer) {
        DeviceSimpleVO deviceSimpleVO = new DeviceSimpleVO();
        deviceSimpleVO.setId(printer.getId());
        deviceSimpleVO.setName(printer.getName());
        PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(printer.getModel());
        deviceSimpleVO.setModel(printerModelEnum == null ? printer.getModel() : printerModelEnum.getText());
        deviceSimpleVO.setDeviceId(printer.getDeviceId());
        deviceSimpleVO.setSort(printer.getSort());
        return deviceSimpleVO;
    }

    @Override
    public DeviceCompleteVO getDeviceCompleteInfo(Long printerId) {
        // 1. 获取当前用户
        Long currentUserId = userService.getCurrentUserId();

        // 2. 获取打印机信息
        Printer printer = printerService.getById(printerId);
        ThrowUtils.throwIf(printer == null || !printer.getUserId().equals(currentUserId),
                ErrorCode.NOT_FOUND_ERROR, "打印机不存在");
        String deviceId = printer.getDeviceId();
        DeviceCompleteVO completeInfo = new DeviceCompleteVO();
        fillPowerInfo(completeInfo, printer);
        completeInfo.setChamberTemp(resolvePrinterChamberTemp(printer, null));

        // 3. 获取设备最新状态信息
        DeviceStatusCache deviceStatus = deviceStatusCacheService.getDeviceStatus(deviceId);
        if (deviceStatus == null) {
            completeInfo.setLastUpdateTime(System.currentTimeMillis());
            return completeInfo;
        }

        // 4. 构建状态信息
        PrinterStatusVO status = buildPrinterStatus(printer.getModel(), printer.getName(), deviceStatus, deviceId);

        // 5. 构建amsList
        List<AmsVO> amsVOList = buildAmsList(deviceStatus, printerId);

        // 6. 组装完整信息
        completeInfo.setStatus(status);
        completeInfo.setAmsList(amsVOList);
        completeInfo.setMqttConnectStatus(bambuMqttService.getConnectionStatus(deviceId).getValue());
        BambuMqttMessage message = deviceStatus.getMessage();
        if (message != null) {
            completeInfo.setChamberLight(getLightMode(message, LedNodeEnum.CHAMBER_LIGHT.getValue()));
            completeInfo.setChamberLight2(getLightMode(message, LedNodeEnum.CHAMBER_LIGHT2.getValue()));
            completeInfo.setWorkLight(getLightMode(message, LedNodeEnum.WORK_LIGHT.getValue()));
            completeInfo.setFans(buildFanStatus(message));
            completeInfo.setBedTemp(getSafeBedTemp(message));
            completeInfo.setChamberTemp(resolvePrinterChamberTemp(printer, message));
            fillNozzleTemperature(completeInfo, message);
            completeInfo.setHmsErrors(hmsErrorService.resolveErrors(message.getHms(), printer.getModel()));
            completeInfo.setLayerNum(deviceStatus.getCurrentLayerNum());
            completeInfo.setTotalLayerNum(deviceStatus.getTotalLayerNum());
        }
        completeInfo.setLastUpdateTime(System.currentTimeMillis());

        return completeInfo;
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

    private void fillNozzleTemperature(DeviceCompleteVO completeInfo, BambuMqttMessage message) {
        BambuConvertUtils.NozzleTemperatureInfo nozzleTemperatureInfo =
                BambuConvertUtils.getNozzleTemperatureInfo(message);
        completeInfo.setActiveNozzle(nozzleTemperatureInfo.getActiveNozzle());
        completeInfo.setNozzleTemp(nozzleTemperatureInfo.getNozzleTemp());
        completeInfo.setTargetNozzleTemp(nozzleTemperatureInfo.getTargetNozzleTemp());
        completeInfo.setLeftNozzleTemp(nozzleTemperatureInfo.getLeftNozzleTemp());
        completeInfo.setLeftTargetNozzleTemp(nozzleTemperatureInfo.getLeftTargetNozzleTemp());
        completeInfo.setRightNozzleTemp(nozzleTemperatureInfo.getRightNozzleTemp());
        completeInfo.setRightTargetNozzleTemp(nozzleTemperatureInfo.getRightTargetNozzleTemp());
    }

    private void fillPowerInfo(DeviceCompleteVO completeInfo, Printer printer) {
        if (completeInfo == null || printer == null || StrUtil.isBlank(printer.getPowerEntityId())) {
            return;
        }
        try {
            completeInfo.setPower(homeAssistantService.getEntityState(printer.getPowerEntityId()));
        } catch (Exception e) {
            log.warn("获取打印机功率信息失败: printerId={}, entityId={}", printer.getId(), printer.getPowerEntityId(), e);
        }
    }

    private Integer resolvePrinterChamberTemp(Printer printer, BambuMqttMessage message) {
        String entityValue = getEntityStateValue(printer == null ? null : printer.getChamberTempEntityId());
        Integer parsedEntityValue = parseInteger(entityValue);
        if (parsedEntityValue != null) {
            return parsedEntityValue;
        }
        return getSafeChamberTemp(message);
    }

    private List<AmsVO> buildAmsList(DeviceStatusCache deviceStatus, Long printerId) {
        if (deviceStatus == null || deviceStatus.getMessage() == null) {
            return Collections.emptyList();
        }
        AmsStatus amsStatus = deviceStatus.getMessage().getAms();
        if (amsStatus == null) {
            return Collections.emptyList();
        }

        List<AmsVO> amsVOList = new ArrayList<>();

        Integer trayNow;
        Integer activeAmsNo;
        Integer activeAmsSlotNo;

        // 尝试获取新版信息（P2S、H2D 等新设备）
        Integer totalNozzles = BambuConvertUtils.getTotalNozzles(deviceStatus.getMessage());
        if (totalNozzles != null) {
            DeviceStatus.Extruder extruder = deviceStatus.getMessage().getDevice().getExtruder();
            Integer state = extruder.getState();
            List<DeviceStatus.Extruder.ExtruderInfo> extruderInfos = extruder.getInfo();
            // 获取到正在使用中的喷嘴
            int actionNozzle = BambuConvertUtils.getActionNozzle(state);

            // 边界检查：如果 actionNozzle 超出范围，使用默认值 0
            if (extruderInfos == null || extruderInfos.isEmpty()) {
                // 如果 extruderInfos 为空，回退到旧版逻辑
                trayNow = amsStatus.getTrayNow();
                activeAmsNo = BambuConvertUtils.getActiveAmsNo(trayNow);
                activeAmsSlotNo = BambuConvertUtils.getActiveAmsSlotNo(trayNow);
            } else {
                // 确保 actionNozzle 在有效范围内
                if (actionNozzle < 0 || actionNozzle >= extruderInfos.size()) {
                    actionNozzle = 0;
                }
                Integer snow = extruderInfos.get(actionNozzle).getSnow();

                // 使用新版解析方法
                activeAmsNo = BambuConvertUtils.getActiveAmsNoBySnow(snow);
                activeAmsSlotNo = BambuConvertUtils.getActiveSlotNoBySnow(snow);
            }
        } else {
            // 旧版设备（P1P、X1C 等）使用 tray_now 字段
            trayNow = amsStatus.getTrayNow();
            activeAmsNo = BambuConvertUtils.getActiveAmsNo(trayNow);
            activeAmsSlotNo = BambuConvertUtils.getActiveAmsSlotNo(trayNow);
        }

        // 处理 AMS 列表
        List<AmsInstance> amsList = amsStatus.getAmsList();
        if (amsList != null && !amsList.isEmpty()) {
            for (AmsInstance amsInstance : amsList) {
                AmsVO amsVO = buildAmsVO(amsInstance, printerId, activeAmsNo, activeAmsSlotNo);
                if (amsVO != null) {
                    amsVOList.add(amsVO);
                }
            }
        }

        // 处理外挂料盘
        AmsTray vtTray = deviceStatus.getMessage().getVtTray();
        if (vtTray != null) {
            AmsVO vtAmsVO = buildVtTrayAmsVO(vtTray, printerId, activeAmsNo, activeAmsSlotNo);
            if (vtAmsVO != null) {
                amsVOList.add(vtAmsVO);
            }
        }

        // 处理虚拟料盘
        List<AmsTray> virSlot = deviceStatus.getMessage().getVirSlot();
        if (CollUtil.isNotEmpty(virSlot)) {
            for (AmsTray amsTray : virSlot) {
                AmsVO amsVO = buildVirSlotAmsVO(amsTray, printerId, activeAmsNo, activeAmsSlotNo, virSlot.size());
                if (amsVO != null) {
                    amsVOList.add(amsVO);
                }
            }
        }

        return amsVOList;
    }

    /**
     * 构建AMS VO对象
     */
    private AmsVO buildAmsVO(AmsInstance amsInstance, Long printerId, Integer activeAmsNo, Integer activeAmsSlotNo) {
        try {
            Integer amsNo = Integer.valueOf(amsInstance.getId());
            Ams ams = amsService.getByPrinterIdAndAmsNo(printerId, amsNo);
            if (ams == null) {
                return null;
            }

            AmsVO amsVO = new AmsVO();
            amsVO.setId(ams.getId());
            amsVO.setAmsNo(amsNo);
            amsVO.setAmsName(BambuConvertUtils.convertAmsName(amsNo, false));
            amsVO.setTrayNow(amsNo.equals(activeAmsNo) ? activeAmsSlotNo : TRAY_NONE);
            amsVO.setHumidityEntityId(ams.getHumidityEntityId());
            amsVO.setTemperatureEntityId(ams.getTemperatureEntityId());
            amsVO.setHumidity(resolveAmsHumidity(ams, amsInstance));
            amsVO.setTemperature(resolveAmsTemperature(ams, amsInstance));
            amsVO.setDrySetting(amsInstance.getDrySetting());
            amsVO.setDryTime(amsInstance.getDryTime());

            List<AmsTray> trayList = amsInstance.getTray();
            if (trayList != null && !trayList.isEmpty()) {
                List<AmsSlotsVO> slotsList = trayList.stream()
                        .map(amsTray -> buildAmsSlotsVO(amsTray, ams.getId()))
                        .filter(Objects::nonNull)
                        .toList();
                amsVO.setSlots(slotsList);
            } else {
                amsVO.setSlots(Collections.emptyList());
            }

            return amsVO;
        } catch (Exception e) {
            log.error("构建AMS VO失败, amsInstanceId: {}", amsInstance.getId(), e);
            return null;
        }
    }

    /**
     * 构建外挂料盘AMS VO对象
     */
    private AmsVO buildVtTrayAmsVO(AmsTray vtTray, Long printerId, Integer activeAmsNo, Integer activeAmsSlotNo) {
        try {
            Integer vtTrayId = Integer.valueOf(vtTray.getId());
            Ams ams = amsService.getByPrinterIdAndAmsNo(printerId, vtTrayId);
            if (ams == null) {
                return null;
            }

            AmsVO vtAmsVO = new AmsVO();
            vtAmsVO.setId(ams.getId());
            vtAmsVO.setAmsNo(vtTrayId);
            vtAmsVO.setAmsName(BambuConvertUtils.convertAmsName(vtTrayId, false));
            vtAmsVO.setTrayNow(vtTrayId.equals(activeAmsNo) ? activeAmsSlotNo : TRAY_NONE);
            vtAmsVO.setHumidityEntityId(ams.getHumidityEntityId());
            vtAmsVO.setTemperatureEntityId(ams.getTemperatureEntityId());
            vtAmsVO.setHumidity(resolveAmsHumidity(ams, null));
            vtAmsVO.setTemperature(resolveAmsTemperature(ams, null));

            AmsSlotsVO amsSlotsVO = buildAmsSlotsVO(vtTray, ams.getId());
            if (amsSlotsVO != null) {
                vtAmsVO.setSlots(Collections.singletonList(amsSlotsVO));
            } else {
                vtAmsVO.setSlots(Collections.emptyList());
            }

            return vtAmsVO;
        } catch (Exception e) {
            log.error("构建外挂料盘AMS VO失败, vtTrayId: {}", vtTray.getId(), e);
            return null;
        }
    }

    /**
     * 构建外挂料盘AMS VO对象
     */
    private AmsVO buildVirSlotAmsVO(AmsTray vtTray, Long printerId, Integer activeAmsNo, Integer activeAmsSlotNo, Integer virSlotNum) {
        try {
            Integer vtTrayId = Integer.valueOf(vtTray.getId());
            Ams ams = amsService.getByPrinterIdAndAmsNo(printerId, vtTrayId);
            if (ams == null) {
                return null;
            }

            AmsVO vtAmsVO = new AmsVO();
            vtAmsVO.setId(ams.getId());
            vtAmsVO.setAmsNo(vtTrayId);
            vtAmsVO.setAmsName(BambuConvertUtils.convertAmsName(vtTrayId, virSlotNum == 2));
            vtAmsVO.setTrayNow(vtTrayId.equals(activeAmsNo) ? activeAmsSlotNo : TRAY_NONE);
            vtAmsVO.setHumidityEntityId(ams.getHumidityEntityId());
            vtAmsVO.setTemperatureEntityId(ams.getTemperatureEntityId());
            vtAmsVO.setHumidity(resolveAmsHumidity(ams, null));
            vtAmsVO.setTemperature(resolveAmsTemperature(ams, null));

            AmsSlotsVO amsSlotsVO = buildAmsSlotsVO(vtTray, ams.getId());
            if (amsSlotsVO != null) {
                vtAmsVO.setSlots(Collections.singletonList(amsSlotsVO));
            } else {
                vtAmsVO.setSlots(Collections.emptyList());
            }

            return vtAmsVO;
        } catch (Exception e) {
            log.error("构建外挂料盘AMS VO失败, vtTrayId: {}", vtTray.getId(), e);
            return null;
        }
    }

    /**
     * 构建 AMS 槽位 VO 对象
     */
    private AmsSlotsVO buildAmsSlotsVO(AmsTray amsTray, Long amsId) {
        try {
            AmsSlotsVO amsSlotsVO = new AmsSlotsVO();
            amsSlotsVO.setTrayInfoIdx(amsTray.getTrayInfoIdx());
            amsSlotsVO.setSlotNo(Integer.valueOf(amsTray.getId()));
            amsSlotsVO.setTrayType(amsTray.getTrayType());

            // 安全处理颜色值
            String trayColor = amsTray.getTrayColor();
            if (trayColor != null && trayColor.length() >= 6) {
                amsSlotsVO.setTrayColor("#" + trayColor.substring(0, 6));
            } else {
                amsSlotsVO.setTrayColor("#FFFFFF"); // 默认白色
            }

            // 获取槽位信息和线轴信息
            AmsSlots amsSlots = amsSlotsService.getByAmsIdAndSlotNo(amsId, amsSlotsVO.getSlotNo());
            if (amsSlots != null) {
                Long spoolId = amsSlots.getSpoolId();
                amsSlotsVO.setSpoolId(spoolId);

                if (spoolId != null) {
                    amsSlotsVO.setSpool(spoolService.convertToVO(spoolService.getById(spoolId)));
                }
            }

            amsSlotsVO.setId(amsSlotsVO.getId());
            amsSlotsVO.setAmsId(amsId);
            return amsSlotsVO;
        } catch (Exception e) {
            log.error("构建AMS槽位VO失败, amsTrayId: {}", amsTray.getId(), e);
            return null;
        }
    }

    private String resolveAmsTemperature(Ams ams, AmsInstance amsInstance) {
        String entityValue = getEntityStateValue(ams == null ? null : ams.getTemperatureEntityId());
        if (StrUtil.isNotBlank(entityValue)) {
            return entityValue;
        }
        return amsInstance == null ? null : amsInstance.getTemp();
    }

    private String resolveAmsHumidity(Ams ams, AmsInstance amsInstance) {
        String entityValue = getEntityStateValue(ams == null ? null : ams.getHumidityEntityId());
        if (StrUtil.isNotBlank(entityValue)) {
            return entityValue;
        }
        if (amsInstance == null || amsInstance.getHumidityRaw() == null) {
            return null;
        }
        return String.valueOf(amsInstance.getHumidityRaw());
    }

    private String getEntityStateValue(String entityId) {
        if (StrUtil.isBlank(entityId)) {
            return null;
        }
        try {
            HomeAssistantEntityStateVO entityState = homeAssistantService.getEntityState(entityId);
            return entityState == null ? null : entityState.getState();
        } catch (Exception e) {
            log.warn("获取 Home Assistant 实体信息失败: entityId={}", entityId, e);
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            log.warn("Home Assistant 实体状态无法解析为数字: state={}", value);
            return null;
        }
    }

    @Override
    public boolean bindSlotSpool(AmsBindRequest request) {
        // 1. 获取请求参数
        Long amsId = request.getAmsId();
        Integer slotNo = request.getSlotNo();
        Long spoolId = request.getSpoolId();
        Boolean syncUpdateDevice = request.getSyncUpdateDevice();
        String trayInfoIdx = request.getTrayInfoIdx();

        // 2. 获取当前用户
        Long currentUserId = userService.getCurrentUserId();

        // 3. 验证AMS是否属于当前用户
        Ams ams = amsService.getById(amsId);
        ThrowUtils.throwIf(ams == null, ErrorCode.NOT_FOUND_ERROR, "AMS 不存在");
        ThrowUtils.throwIf(!ams.getUserId().equals(currentUserId), ErrorCode.NO_AUTH_ERROR, "无权限操作该 AMS");

        // 0 表示解绑；绑定时必须确保耗材属于当前用户
        if (!Objects.equals(spoolId, 0L)) {
            spoolService.getSpoolById(spoolId);
        }

        // 4. 验证该耗材是否已经被绑定
        AmsSlots slotsBySpoolId = amsSlotsService.getBySpoolId(spoolId, currentUserId);
        if (slotsBySpoolId != null) {
            // 先解绑
            boolean unbind = amsSlotsService.bindSlotSpool(slotsBySpoolId.getAmsId(), slotsBySpoolId.getSlotNo(), 0L);
            ThrowUtils.throwIf(!unbind, ErrorCode.SYSTEM_ERROR);
        }

        // 5. 执行绑定操作
        boolean result = amsSlotsService.bindSlotSpool(amsId, slotNo, spoolId);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        // 6. 同步更新设备中ams槽位信息，仅内网部署生效
        if (syncUpdateDevice && StrUtil.isNotBlank(trayInfoIdx) && isIntranet) {
            // 获取打印机信息
            Long printerId = ams.getPrinterId();
            Printer printer = printerService.getById(printerId);
            // 获取耗材信息
            Spool spool = spoolService.getById(spoolId);
            Integer amsNo = ams.getAmsNo();
            // a1特殊处理，修改外挂料盘得设置为255
            if (PrinterModelEnum.getEnumByValue(printer.getModel()) == PrinterModelEnum.A1
                    && Objects.equals(amsNo, TRAY_EXTERNAL)) {
                amsNo = TRAY_NONE;
            }
            // 推送命令
            bambuMqttService.amsFilamentSetting(printer.getDeviceId(), amsNo, slotNo, spool.getColor(), trayInfoIdx);
        }
        return true;
    }

    @Override
    public void switchChamberLight(String deviceId) {
        switchLight(deviceId, LedNodeEnum.CHAMBER_LIGHT.getValue());
    }

    @Override
    public void switchChamberLight2(String deviceId) {
        switchLight(deviceId, LedNodeEnum.CHAMBER_LIGHT2.getValue());
    }

    private void switchLight(String deviceId, String ledNode) {
        // 获取当前用户
        Long currentUserId = userService.getCurrentUserId();
        // 获取设备用户
        String redisUserKey = DEVICE_USER_PREFIX + deviceId;
        // 校验用户权限
        if (redisUtils.hasKey(redisUserKey)) {
            Long userId = redisUtils.getLong(redisUserKey);
            ThrowUtils.throwIf(!currentUserId.equals(userId), ErrorCode.NO_AUTH_ERROR, "无操作权限");
        }
        // 获取设备状态
        DeviceStatusCache deviceStatus = deviceStatusCacheService.getDeviceStatus(deviceId);
        if (deviceStatus != null) {
            String light = ledNode.equals(LedNodeEnum.CHAMBER_LIGHT.getValue()) ? deviceStatus.getChamberLight() : deviceStatus.getChamberLight2();
            if (light.equals("on")) {
                bambuMqttService.turnOffChamberLight(deviceId, ledNode);
            } else {
                bambuMqttService.turnOnChamberLight(deviceId, ledNode);
            }
        }
    }

    private PrinterStatusVO buildPrinterStatus(String model, String name, DeviceStatusCache deviceStatus, String deviceId) {
        if (deviceStatus == null) {
            return null;
        }
        PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(model);
        String redisTaskKey = DEVICE_TASK_PREFIX + deviceId;
        PrinterStatusVO vo = new PrinterStatusVO();
        vo.setName(name);
        vo.setDeviceId(deviceId);
        vo.setModel(printerModelEnum != null ? printerModelEnum.getText() : model);
        vo.setPrintStatus(deviceStatus.getPrintStatus());
        vo.setPrintProgress(deviceStatus.getPrintProgress());
        Object taskImage = redisUtils.get(redisTaskKey);
        if (taskImage == null && deviceStatus.isPrintStarted()) {
            TaskCoverEvent taskCoverEvent = new TaskCoverEvent(deviceId);
            eventPublisher.publish(taskCoverEvent);
        }
        vo.setTaskImage(Objects.toString(taskImage, null));
        vo.setRemainingTime(deviceStatus.getRemainingTime());
        return vo;
    }

    // ==================== 流媒体相关业务逻辑 ====================

    @Override
    public ResponseEntity<StreamingResponseBody> getStreamContent(String deviceId) {
        try {
            // 权限验证 - 在主线程中完成，避免异步线程中访问Session上下文
            Long currentUserId = userService.getCurrentUserId();
            Printer printer = printerService.getByDeviceId(deviceId);
            ThrowUtils.throwIf(printer == null, ErrorCode.NOT_FOUND_ERROR, "打印机不存在");
            ThrowUtils.throwIf(!currentUserId.equals(printer.getUserId()), ErrorCode.NO_AUTH_ERROR, "无操作权限");

            // 创建流媒体会话
            String sessionId = streamConnectionManager.createStreamSession(deviceId, currentUserId);
            AtomicBoolean isStreaming = streamConnectionManager.getStreamingFlag(sessionId);

            // 提前获取必要的信息，避免在异步线程中访问数据库或Session上下文
            final String printerIp = printer.getIpAddress();
            final String accessCode = printer.getAccessCode();
            final String logDeviceId = deviceId; // 用于日志记录

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, bambuImageService.getMjpegContentType());
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            headers.add("Connection", "keep-alive");
            headers.add("X-Stream-Session-Id", sessionId); // 返回会话 ID 给前端

            // 创建流媒体响应体 - 异步执行，不依赖Session上下文
            StreamingResponseBody streamingBody = outputStream -> {
                try {
                    // 使用预先获取的IP和访问码，避免在异步线程中访问数据库
                    bambuImageService.streamMjpegFramesByDeviceId(outputStream, printerIp, accessCode, isStreaming);
                } catch (Exception e) {
                    log.error("流媒体传输异常: deviceId={}, error={}", logDeviceId, e.getMessage(), e);
                    try {
                        bambuImageService.sendErrorFrame(outputStream, "流媒体传输异常: " + e.getMessage());
                    } catch (Exception sendErrorException) {
                        log.error("发送错误帧失败: {}", sendErrorException.getMessage());
                    }
                } finally {
                    // 清理会话
                    streamConnectionManager.cleanupSession(sessionId);
                    // todo 这边执行之后会抛出上下文未初始化的异常，待解决
                }
            };

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(streamingBody);

        } catch (Exception e) {
            // 在设置MJPEG Content-Type之前捕获异常，返回标准错误响应
            log.error("流媒体初始化失败: deviceId={}, error={}", deviceId, e.getMessage(), e);

            // 返回一个错误的StreamingResponseBody，使用text/plain格式避免转换问题
            StreamingResponseBody errorBody = outputStream -> {
                try {
                    String errorMessage = "流媒体初始化失败: " + e.getMessage();
                    outputStream.write(errorMessage.getBytes());
                    outputStream.flush();
                } catch (Exception writeException) {
                    log.error("写入错误信息失败: {}", writeException.getMessage());
                }
            };

            return ResponseEntity.status(500)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
                    .body(errorBody);
        }
    }

    private void disconnectImageStream(String deviceId) {
        Long currentUserId = userService.getCurrentUserId();
        String sessionId = currentUserId + "_" + deviceId;
        streamConnectionManager.disconnectStream(sessionId);
    }

    private String getLiveStream(String deviceId) {
        Printer printer = printerService.getByDeviceId(deviceId);
        ThrowUtils.throwIf(!Objects.equals(userService.getCurrentUserId(), printer.getUserId()), ErrorCode.NO_AUTH_ERROR);

        // 自动启动流
        if (!bambuVideoService.isLiveRunning(deviceId)) {
            boolean started;
            if (StrUtil.isNotBlank(printer.getCustomCameraRtspUrl())) {
                started = bambuVideoService.startLiveStream(printer.getCustomCameraRtspUrl(), deviceId);
            } else {
                started = bambuVideoService.startLiveStream(printer.getIpAddress(), printer.getAccessCode(), deviceId);
            }
            ThrowUtils.throwIf(!started, ErrorCode.CONNECTION_FAILED, "无法启动流媒体");
        }
        // 等待播放列表进入可稳定播放状态，避免首帧出来后马上因为下一片未就绪而卡顿。
        bambuVideoService.waitForReadyPlaylist(deviceId, 10000, bambuVideoService.recommendedStartupSegments());
        // 返回 HLS URL，前端 <video> 标签直接访问
        return bambuVideoService.getStreamUrl(deviceId);
    }

    private void disconnectLiveStream(String deviceId) {
        bambuVideoService.stopLiveStream(deviceId);
    }

    @Override
    public DeviceStreamVO getStreamInfo(String deviceId) {
        Long currentUserId = userService.getCurrentUserId();
        Printer printer = printerService.getByDeviceId(deviceId);
        ThrowUtils.throwIf(printer == null, ErrorCode.NOT_FOUND_ERROR, "打印机不存在");
        ThrowUtils.throwIf(!Objects.equals(currentUserId, printer.getUserId()), ErrorCode.NO_AUTH_ERROR, "无操作权限");

        DeviceStreamVO vo = new DeviceStreamVO();

        // 自定义摄像头优先走 HLS
        if (StrUtil.isNotBlank(printer.getCustomCameraRtspUrl())) {
            String hlsUrl = getLiveStream(deviceId);
            vo.setStreamType("hls");
            vo.setUrl(hlsUrl);
            vo.setFps(null);
            vo.setLatencyHint("low");
            return vo;
        }

        if (preferMjpegStream(printer.getModel())) {
            vo.setStreamType("mjpeg");
            vo.setUrl("/device/stream/content?deviceId=" + deviceId);
            vo.setFps(0.5);
            vo.setLatencyHint("medium");
            return vo;
        }

        String hlsUrl = getLiveStream(deviceId);
        vo.setStreamType("hls");
        vo.setUrl(hlsUrl);
        vo.setFps(null);
        vo.setLatencyHint("low");
        return vo;
    }

    @Override
    public boolean disconnectStream(String deviceId, String type) {
        Long currentUserId = userService.getCurrentUserId();
        Printer printer = printerService.getByDeviceId(deviceId);
        ThrowUtils.throwIf(printer == null, ErrorCode.NOT_FOUND_ERROR, "打印机不存在");
        ThrowUtils.throwIf(!Objects.equals(currentUserId, printer.getUserId()), ErrorCode.NO_AUTH_ERROR, "无操作权限");

        String streamType = StrUtil.blankToDefault(type, "auto").toLowerCase();
        switch (streamType) {
            case "hls":
                disconnectLiveStream(deviceId);
                return true;
            case "mjpeg":
                disconnectImageStream(deviceId);
                return true;
            case "all":
                disconnectImageStream(deviceId);
                disconnectLiveStream(deviceId);
                return true;
            case "auto":
                if (preferMjpegStream(printer.getModel())) {
                    disconnectImageStream(deviceId);
                } else {
                    disconnectLiveStream(deviceId);
                }
                return true;
            default:
                return false;
        }
    }

    private boolean preferMjpegStream(String model) {
        PrinterModelEnum modelEnum = PrinterModelEnum.getEnumByValue(model);
        if (modelEnum == null) {
            return false;
        }
        return modelEnum == PrinterModelEnum.A1
                || modelEnum == PrinterModelEnum.A1MINI
                || modelEnum == PrinterModelEnum.P1P
                || modelEnum == PrinterModelEnum.P1S;
    }

    private String getLightMode(BambuMqttMessage message, String ledNode) {
        if (message == null || message.getLightsReport() == null) return "off";
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
        if (temp == null) {
            return 0;
        }

        return temp & 0xFFFF;
    }

}
