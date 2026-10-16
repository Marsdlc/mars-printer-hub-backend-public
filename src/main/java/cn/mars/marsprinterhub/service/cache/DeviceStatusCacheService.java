package cn.mars.marsprinterhub.service.cache;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.model.AmsInstance;
import cn.mars.marsprinterhub.bambu.model.AmsStatus;
import cn.mars.marsprinterhub.bambu.model.AmsTray;
import cn.mars.marsprinterhub.bambu.model.DeviceStatus;
import cn.mars.marsprinterhub.bambu.model.HmsError;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttMessage;
import cn.mars.marsprinterhub.constant.RedisConstants;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.*;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.model.enums.PrinterStatusEnum;
import cn.mars.marsprinterhub.utils.BambuConvertUtils;
import cn.mars.marsprinterhub.utils.DeepCopyUtils;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.mars.marsprinterhub.constant.RedisConstants.*;

/**
 * 设备状态缓存服务
 * 负责设备状态的Redis操作，包括状态存储、获取和历史记录管理
 */
@Slf4j
@Service
public class DeviceStatusCacheService {

    // 缓存过期时间（秒）
    private static final long STATUS_EXPIRE_SECONDS = 24 * 60 * 60;  // 设备状态24小时过期
    private static final Set<Integer> SNAPSHOT_POINTS =
            Set.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 99);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    @Value("${bambu.intranet}")
    private boolean isIntranet;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private EventPublisher eventPublisher;

    private static <T> T mergeValue(T existing, T newValue) {
        return newValue != null ? newValue : existing;
    }

    /**
     * 仅保存设备状态
     *
     * @param deviceId 设备ID
     * @param message  MQTT消息对象
     */
    public void saveDeviceStatusOnly(String deviceId, BambuMqttMessage message) {
        if (shuttingDown.get()) {
            return;
        }

        if (message == null || message.isEmpty()) {
            return;
        }

        try {
            DeviceStatusCache existingStatus = getDeviceStatus(deviceId);
            BambuMqttMessage mergedMessage = processStatusUpdate(deviceId, existingStatus, message);
            saveStatusToCache(deviceId, mergedMessage);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("LettuceConnectionFactory was destroyed")) {
                return;
            }
            log.error("保存设备状态失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存设备状态失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("保存设备状态失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存设备状态失败: " + e.getMessage());
        }
    }

    /**
     * 处理状态更新逻辑
     */
    private BambuMqttMessage processStatusUpdate(String deviceId, DeviceStatusCache existingStatus, BambuMqttMessage newMessage) {
        if (existingStatus != null && existingStatus.getMessage() != null) {

            // ---- 先深拷贝保存 lastStatus ----
            DeviceStatusCache lastStatus = DeepCopyUtils.deepCopy(existingStatus, DeviceStatusCache.class);
            saveLastStatus(deviceId, lastStatus);

            // ---- 再合并 ----
            BambuMqttMessage mergedMessage = mergeMessages(existingStatus.getMessage(), newMessage);

            // 状态变化事件
            handleStatusChanges(existingStatus, mergedMessage);

            // ---- 使用 lastStatus 对比，而不是 existingStatus ----
            if (isIntranet) {
                Integer lastPercent = lastStatus.getMessage().getMcPercent();
                Integer newPercent = mergedMessage.getMcPercent();

                if (lastPercent != null && newPercent != null) {
                    if (SNAPSHOT_POINTS.contains(newPercent) && !SNAPSHOT_POINTS.contains(lastPercent)) {
                        DeviceSnapshotRequestEvent snapshotEvent = new DeviceSnapshotRequestEvent(
                                deviceId, newPercent, DeviceSnapshotRequestEvent.SnapshotType.PROGRESS_MILESTONE
                        );
                        eventPublisher.publish(snapshotEvent);
                    }
                }
            }

            notifyDeviceUpdate(deviceId, existingStatus.getMessage(), mergedMessage);
            return mergedMessage;
        }

        return newMessage;
    }

    /**
     * 保存上一次状态
     */
    private void saveLastStatus(String deviceId, DeviceStatusCache existingStatus) {
        String lastStatusKey = DEVICE_LAST_STATUS_PREFIX + deviceId;
        redisUtils.set(lastStatusKey, existingStatus, STATUS_EXPIRE_SECONDS);
    }

    /**
     * 保存状态到缓存
     */
    private void saveStatusToCache(String deviceId, BambuMqttMessage message) {
        String statusKey = DEVICE_STATUS_PREFIX + deviceId;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        DeviceStatusCache statusCache = DeviceStatusCache.builder()
                .deviceId(deviceId)
                .message(message)
                .timestamp(timestamp)
                .build();

        redisUtils.set(statusKey, statusCache, STATUS_EXPIRE_SECONDS);
    }

    /**
     * 检查关键字段变化并通知前端
     */
    private void notifyDeviceUpdate(String deviceId, BambuMqttMessage existingMessage, BambuMqttMessage mergedMessage) {
        if (existingMessage == null || mergedMessage == null) {
            return;
        }

        if (hasKeyFieldsChanged(existingMessage, mergedMessage)) {
            sendSseNotification(deviceId);
        }
    }

    /**
     * 检查关键字段是否发生变化
     */
    private boolean hasKeyFieldsChanged(BambuMqttMessage existing, BambuMqttMessage merged) {
        // 先检查简单字段，如果有变化就直接返回true，避免复杂对象比较
        if (ObjUtil.notEqual(existing.getMcPercent(), merged.getMcPercent()) ||
                ObjUtil.notEqual(existing.getGcodeState(), merged.getGcodeState()) ||
                ObjUtil.notEqual(existing.getLayerNum(), merged.getLayerNum()) ||
                ObjUtil.notEqual(existing.getTotalLayerNum(), merged.getTotalLayerNum()) ||
                ObjUtil.notEqual(existing.getMcRemainingTime(), merged.getMcRemainingTime()) ||
                ObjUtil.notEqual(existing.getCoolingFanSpeedPercentage(), merged.getCoolingFanSpeedPercentage()) ||
                ObjUtil.notEqual(existing.getAuxFanSpeedPercentage(), merged.getAuxFanSpeedPercentage()) ||
                ObjUtil.notEqual(existing.getChamberFanSpeedPercentage(), merged.getChamberFanSpeedPercentage()) ||
                ObjUtil.notEqual(existing.getSecondaryAuxFanSpeedPercentage(), merged.getSecondaryAuxFanSpeedPercentage()) ||
                hasHmsListChanged(existing.getHms(), merged.getHms()) ||
                ObjUtil.notEqual(existing.getLightsReport(), merged.getLightsReport())) {
            return true;
        }

        // 只有简单字段都没变化时，才检查复杂对象
        return hasComplexObjectsChanged(existing, merged);
    }

    /**
     * 检查 HMS 错误列表是否发生变化。
     * null 和空列表都表示当前没有 HMS 错误，不触发无意义刷新。
     */
    private boolean hasHmsListChanged(List<HmsError> existing, List<HmsError> merged) {
        boolean existingEmpty = existing == null || existing.isEmpty();
        boolean mergedEmpty = merged == null || merged.isEmpty();
        if (existingEmpty && mergedEmpty) {
            return false;
        }
        if (existingEmpty || mergedEmpty) {
            return true;
        }
        return ObjUtil.notEqual(existing, merged);
    }

    /**
     * 检查复杂对象是否发生变化（基于具体字段比较）
     */
    private boolean hasComplexObjectsChanged(BambuMqttMessage existing, BambuMqttMessage merged) {
        return hasAmsStatusChanged(existing.getAms(), merged.getAms()) ||
                hasAmsTrayChanged(existing.getVtTray(), merged.getVtTray());
    }

    /**
     * 检查AmsStatus对象是否发生变化（比较关键字段）
     */
    private boolean hasAmsStatusChanged(AmsStatus existing, AmsStatus merged) {
        // 引用相同或都为null
        if (existing == merged) {
            return false;
        }

        // 一个为null一个不为null
        if (existing == null || merged == null) {
            return true;
        }

        // 比较关键字段
        return ObjUtil.notEqual(existing.getTrayNow(), merged.getTrayNow()) ||
                ObjUtil.notEqual(existing.getTrayTar(), merged.getTrayTar()) ||
                ObjUtil.notEqual(existing.getPowerOnFlag(), merged.getPowerOnFlag()) ||
                hasAmsListChanged(existing.getAmsList(), merged.getAmsList());
    }

    /**
     * 检查AmsTray对象是否发生变化（比较关键字段）
     */
    private boolean hasAmsTrayChanged(AmsTray existing, AmsTray merged) {
        // 引用相同或都为null
        if (existing == merged) {
            return false;
        }

        // 一个为null一个不为null
        if (existing == null || merged == null) {
            return true;
        }

        // 比较关键字段（用户最关心的字段）
        return ObjUtil.notEqual(existing.getRemain(), merged.getRemain()) ||
                ObjUtil.notEqual(existing.getTrayInfoIdx(), merged.getTrayInfoIdx()) ||
                ObjUtil.notEqual(existing.getTrayType(), merged.getTrayType()) ||
                ObjUtil.notEqual(existing.getTrayColor(), merged.getTrayColor()) ||
                ObjUtil.notEqual(existing.getId(), merged.getId());
    }

    /**
     * 检查AMS实例列表是否发生变化
     */
    private boolean hasAmsListChanged(List<AmsInstance> existing, List<AmsInstance> merged) {
        // 引用相同或都为null
        if (existing == merged) {
            return false;
        }

        // 一个为null一个不为null
        if (existing == null || merged == null) {
            return true;
        }

        // 大小不同
        if (existing.size() != merged.size()) {
            return true;
        }

        // 逐个比较AMS实例（排除温湿度字段）
        for (int i = 0; i < existing.size(); i++) {
            AmsInstance existingInstance = existing.get(i);
            AmsInstance mergedInstance = merged.get(i);

            if (hasAmsInstanceChanged(existingInstance, mergedInstance)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 发送SSE通知
     */
    private void sendSseNotification(String deviceId) {
        Object userId = redisUtils.get(DEVICE_USER_PREFIX + deviceId);
        if (userId != null) {
            SseNotificationEvent event = new SseNotificationEvent(
                    Long.parseLong(userId.toString()), deviceId);
            eventPublisher.publish(event);
        }
    }

    private void handleStatusChanges(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        if (existingStatus == null || mergedMessage == null) {
            return;
        }
        // 打印生命周期相关事件
        handlePrintLifecycleChanges(existingStatus, mergedMessage);
        // AMS 相关告警和解绑逻辑
        handleAmsRelatedChanges(existingStatus, mergedMessage);
        // 派生缓存状态记录
        recordDerivedState(existingStatus, mergedMessage);
    }

    private void handlePrintLifecycleChanges(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        // 状态由 PREPARE -> RUNNING，视为打印正式开始
        if (isPrepareToRunning(existingStatus, mergedMessage)) {
            PrinterStartEvent printerStartEvent = new PrinterStartEvent(existingStatus.getDeviceId());
            eventPublisher.publish(printerStartEvent);
        }
        // 状态由 RUNNING -> PAUSE，视为打印暂停
        if (isRunningToPause(existingStatus, mergedMessage)) {
            PrinterPausedEvent printerPausedEvent = new PrinterPausedEvent(existingStatus.getDeviceId());
            eventPublisher.publish(printerPausedEvent);
        }
        // 状态由 RUNNING -> FINISH，视为打印完成
        if (isRunningToFinish(existingStatus, mergedMessage)) {
            PrinterFinishedEvent printerFinishedEvent = new PrinterFinishedEvent(existingStatus.getDeviceId());
            eventPublisher.publish(printerFinishedEvent);
        }

        // 当前层高从 0 -> 1，说明首层开始，可触发封面抓取
        if (isFirstLayerStarted(existingStatus, mergedMessage)) {
            TaskCoverEvent taskCoverEvent = new TaskCoverEvent(existingStatus.getDeviceId());
            eventPublisher.publish(taskCoverEvent);
        }

        // 状态由 RUNNING/PAUSE -> FINISH/FAILED，视为打印任务结束
        if (isPrintCompleted(existingStatus, mergedMessage)) {
            PrintTaskCompletedEvent printTaskCompletedEvent = new PrintTaskCompletedEvent(
                    existingStatus.getDeviceId(),
                    mergedMessage.getMcPercent(),
                    mergedMessage.getGcodeState()
            );
            eventPublisher.publish(printTaskCompletedEvent);
        }
    }

    private boolean isPrepareToRunning(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        return Objects.equals(existingStatus.getPrintStatus(), PrinterStatusEnum.PREPARE.getValue())
                && Objects.equals(mergedMessage.getGcodeState(), PrinterStatusEnum.RUNNING.getValue());
    }

    private boolean isRunningToPause(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        return Objects.equals(existingStatus.getPrintStatus(), PrinterStatusEnum.RUNNING.getValue())
                && Objects.equals(mergedMessage.getGcodeState(), PrinterStatusEnum.PAUSE.getValue());
    }

    private boolean isRunningToFinish(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        return Objects.equals(existingStatus.getPrintStatus(), PrinterStatusEnum.RUNNING.getValue())
                && Objects.equals(mergedMessage.getGcodeState(), PrinterStatusEnum.FINISH.getValue());
    }

    private boolean isFirstLayerStarted(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        return Objects.equals(existingStatus.getCurrentLayer(), 0)
                && Objects.equals(mergedMessage.getLayerNum(), 1);
    }

    private boolean isPrintCompleted(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        return (existingStatus.isPrintStarted() || existingStatus.isPrintPaused())
                && (Objects.equals(mergedMessage.getGcodeState(), PrinterStatusEnum.FINISH.getValue())
                || Objects.equals(mergedMessage.getGcodeState(), PrinterStatusEnum.FAILED.getValue()));
    }

    private void handleAmsRelatedChanges(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        // ams湿度过高检测
        handleAmsHumidityTooHighWarning(existingStatus.getDeviceId(), existingStatus.getMessage(), mergedMessage);

        // 处理槽位变空后自动解绑
        handleAmsSlotEmptyEvent(existingStatus.getDeviceId(), existingStatus.getMessage(), mergedMessage);
    }

    private void recordDerivedState(DeviceStatusCache existingStatus, BambuMqttMessage mergedMessage) {
        // 记录ams使用的槽位
        recordAmsSlot(existingStatus.getDeviceId(), mergedMessage);
    }

    private void recordAmsSlot(String deviceId, BambuMqttMessage message) {
        if (message == null) return;
        if (message.getDevice() == null) return;
        if (message.getDevice().getExtruder() == null) return;
        String redisKey = RedisConstants.DEVICE_TASK_AMS_PREFIX + deviceId;
        Integer state = message.getDevice().getExtruder().getState();
        Integer amsNo;
        Integer slotNo;
        // 优先使用新版的状态
        if (state != null) {
            int actionNozzle = BambuConvertUtils.getActionNozzle(state);
            List<DeviceStatus.Extruder.ExtruderInfo> extruderInfos = message.getDevice().getExtruder().getInfo();
            if (CollUtil.isEmpty(extruderInfos)) return;
            if (actionNozzle < 0 || actionNozzle >= extruderInfos.size()) return;
            DeviceStatus.Extruder.ExtruderInfo extruderInfo = extruderInfos.get(actionNozzle);
            if (extruderInfo == null) return;
            Integer snow = extruderInfo.getSnow();
            if (snow == null) return;
            amsNo = BambuConvertUtils.getActiveAmsNoBySnow(snow);
            slotNo = BambuConvertUtils.getActiveSlotNoBySnow(snow);
        } else {
            AmsStatus ams = message.getAms();
            if (ams == null) return;
            Integer trayNow = ams.getTrayNow();
            if (trayNow == null) return;
            amsNo = BambuConvertUtils.getActiveAmsNo(trayNow);
            slotNo = BambuConvertUtils.getActiveAmsSlotNo(trayNow);
        }
        AmsTray currentTray = getTray(message.getAms(), amsNo, slotNo);
        redisUtils.zSet(redisKey, buildAmsSlotCacheValue(amsNo, slotNo, currentTray));
    }

    private String buildAmsSlotCacheValue(Integer amsNo, Integer slotNo, AmsTray tray) {
        if (amsNo == null || slotNo == null) {
            return null;
        }
        return amsNo + "_" + slotNo + "|" +
                normalizeSlotCacheValue(tray == null ? null : tray.getTrayInfoIdx()) + "|" +
                normalizeSlotCacheValue(tray == null ? null : tray.getTrayType()) + "|" +
                normalizeSlotCacheValue(tray == null ? null : tray.getTrayColor());
    }

    private String normalizeSlotCacheValue(String value) {
        return StrUtil.blankToDefault(value, "");
    }

    private AmsTray getTray(AmsStatus amsStatus, Integer amsNo, Integer slotNo) {
        if (amsStatus == null || CollUtil.isEmpty(amsStatus.getAmsList()) || amsNo == null || slotNo == null) {
            return null;
        }
        for (AmsInstance amsInstance : amsStatus.getAmsList()) {
            if (amsInstance == null || !ObjUtil.equal(amsInstance.getId(), String.valueOf(amsNo))) {
                continue;
            }
            List<AmsTray> trays = amsInstance.getTray();
            if (CollUtil.isEmpty(trays) || slotNo < 0 || slotNo >= trays.size()) {
                return null;
            }
            return trays.get(slotNo);
        }
        return null;
    }

    private void handleAmsSlotEmptyEvent(String deviceId, BambuMqttMessage message, BambuMqttMessage mergedMessage) {
        if (message == null || mergedMessage == null) return;

        List<AmsInstance> oldAmsList = Optional.ofNullable(message.getAms()).map(AmsStatus::getAmsList).orElse(null);
        List<AmsInstance> newAmsList = Optional.ofNullable(mergedMessage.getAms()).map(AmsStatus::getAmsList).orElse(null);

        if (oldAmsList == null || newAmsList == null) return;

        int amsCount = Math.min(oldAmsList.size(), newAmsList.size());

        for (int i = 0; i < amsCount; i++) {

            List<AmsTray> oldTray = Optional.ofNullable(oldAmsList.get(i).getTray()).orElse(List.of());
            List<AmsTray> newTray = Optional.ofNullable(newAmsList.get(i).getTray()).orElse(List.of());

            int trayCount = Math.min(oldTray.size(), newTray.size());

            for (int j = 0; j < trayCount; j++) {

                AmsTray oldTrayItem = oldTray.get(j);
                AmsTray newTrayItem = newTray.get(j);

                boolean oldEmpty = oldTrayItem == null || oldTrayItem.isEmptyTray();
                boolean newEmpty = newTrayItem == null || newTrayItem.isEmptyTray();

                // 非空 -> 空  或  非空 -> null
                if (!oldEmpty && newEmpty) {
                    eventPublisher.publish(new AmsSlotAutoUnbindEvent(deviceId, i, j));
                    eventPublisher.publish(new AmsSlotChangeEmptyEvent(deviceId, i, j));
                }
            }
        }
    }

    private void handleAmsHumidityTooHighWarning(String deviceId, BambuMqttMessage message, BambuMqttMessage mergedMessage) {
        if (message == null || mergedMessage == null) return;
        if (message.getAms() == null || mergedMessage.getAms() == null) return;
        if (message.getAms().getAmsList() == null || mergedMessage.getAms().getAmsList() == null) return;

        List<AmsInstance> oldList = message.getAms().getAmsList();
        List<AmsInstance> newList = mergedMessage.getAms().getAmsList();

        for (int i = 0; i < oldList.size(); i++) {
            AmsInstance oldInst = oldList.get(i);
            AmsInstance newInst = newList.size() > i ? newList.get(i) : null;
            if (oldInst == null || newInst == null) continue;
            if (oldInst.getHumidityRaw() == null || newInst.getHumidityRaw() == null) continue;

            // 当湿度从安全变为超标时触发事件
            int threshold = 20;
            if (oldInst.getHumidityRaw() < threshold && newInst.getHumidityRaw() > threshold) {
                eventPublisher.publish(new AmsHumidityTooHighEvent(deviceId, i));
            }
        }
    }

    /**
     * 合并MQTT消息（增量更新）
     *
     * @param existing   现有消息
     * @param newMessage 新消息
     * @return 合并后的消息
     */
    private BambuMqttMessage mergeMessages(BambuMqttMessage existing, BambuMqttMessage newMessage) {
        if (existing == null) {
            return newMessage;
        }
        if (newMessage == null) {
            return existing;
        }

        // 返回合并后的消息
        return mergeMessage(existing, newMessage);
    }

    /**
     * 深度合并PrintStatus对象
     *
     * @param existing 现有PrintStatus
     * @param newPrint 新PrintStatus
     * @return 合并后的PrintStatus
     */
    private BambuMqttMessage mergeMessage(
            BambuMqttMessage existing,
            BambuMqttMessage newPrint) {

        if (existing == null) return newPrint;
        if (newPrint == null) return existing;

        BambuMqttMessage merged = new BambuMqttMessage();

        // 基础字段
        merged.setMcPercent(mergeValue(existing.getMcPercent(), newPrint.getMcPercent()));
        merged.setBedTemper(mergeValue(existing.getBedTemper(), newPrint.getBedTemper()));
        merged.setLayerNum(mergeValue(existing.getLayerNum(), newPrint.getLayerNum()));
        merged.setTotalLayerNum(mergeValue(existing.getTotalLayerNum(), newPrint.getTotalLayerNum()));
        merged.setMcRemainingTime(mergeValue(existing.getMcRemainingTime(), newPrint.getMcRemainingTime()));
        merged.setGcodeState(mergeValue(existing.getGcodeState(), newPrint.getGcodeState()));
        merged.setNozzleTemper(mergeValue(existing.getNozzleTemper(), newPrint.getNozzleTemper()));
        merged.setNozzleTargetTemper(mergeValue(existing.getNozzleTargetTemper(), newPrint.getNozzleTargetTemper()));
        merged.setCoolingFanSpeed(mergeValue(existing.getCoolingFanSpeed(), newPrint.getCoolingFanSpeed()));
        merged.setBigFan1Speed(mergeValue(existing.getBigFan1Speed(), newPrint.getBigFan1Speed()));
        merged.setBigFan2Speed(mergeValue(existing.getBigFan2Speed(), newPrint.getBigFan2Speed()));
        merged.setHms(mergeValue(existing.getHms(), newPrint.getHms()));

        // 嵌套对象调用各自 merge 方法
        merged.setVtTray(existing.getVtTray() != null ? existing.getVtTray().merge(newPrint.getVtTray()) : newPrint.getVtTray());
        merged.setVirSlot(mergeValue(existing.getVirSlot(), newPrint.getVirSlot()));
        merged.setAms(existing.getAms() != null ? existing.getAms().merge(newPrint.getAms()) : newPrint.getAms());
        merged.setNet(existing.getNet() != null ? existing.getNet().merge(newPrint.getNet()) : newPrint.getNet());

        // 灯光
        merged.setLightsReport(mergeValue(existing.getLightsReport(), newPrint.getLightsReport()));

        // 设备状态
        merged.setDevice(existing.getDevice() != null ? existing.getDevice().merge(newPrint.getDevice()) : newPrint.getDevice());
        return merged;
    }

    /**
     * 获取设备当前状态
     *
     * @param deviceId 设备ID
     * @return 设备状态缓存对象
     */
    public DeviceStatusCache getDeviceStatus(String deviceId) {
        return getStatusFromCache(DEVICE_STATUS_PREFIX + deviceId, deviceId, "设备状态");
    }

    /**
     * 安全地将Redis中的对象转换为DeviceStatusCache
     *
     * @param result   Redis中获取的对象
     * @param deviceId 设备ID
     * @return DeviceStatusCache对象
     */
    private DeviceStatusCache convertToDeviceStatusCache(Object result, String deviceId) {
        try {
            // 如果已经是正确的类型，直接返回
            if (result instanceof DeviceStatusCache cache) {
                return cache;
            }

            // 转换为DeviceStatusCache
            DeviceStatusCache cache = convertObjectToCache(result);

            // 确保deviceId不为空
            if (cache.getDeviceId() == null) {
                cache.setDeviceId(deviceId);
            }

            return cache;

        } catch (JsonProcessingException e) {
            log.error("JSON转换失败: deviceId={}, error={}", deviceId, e.getMessage());
            cleanupCorruptedCache(deviceId);
            return null;
        } catch (Exception e) {
            log.error("转换Redis数据时发生未知错误: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将对象转换为DeviceStatusCache
     */
    private DeviceStatusCache convertObjectToCache(Object result) throws JsonProcessingException {
        String jsonString = objectMapper.writeValueAsString(result);
        return objectMapper.readValue(jsonString, DeviceStatusCache.class);
    }

    /**
     * 清理损坏的缓存数据
     */
    private void cleanupCorruptedCache(String deviceId) {
        String statusKey = DEVICE_STATUS_PREFIX + deviceId;
        redisUtils.delete(statusKey);
    }

    /**
     * 获取设备上一次状态
     *
     * @param deviceId 设备ID
     * @return 上一次设备状态缓存对象
     */
    public DeviceStatusCache getLastStatus(String deviceId) {
        return getStatusFromCache(DEVICE_LAST_STATUS_PREFIX + deviceId, deviceId, "设备上一次状态");
    }

    /**
     * 从缓存获取状态的通用方法
     */
    private DeviceStatusCache getStatusFromCache(String cacheKey, String deviceId, String statusType) {
        try {
            Object result = redisUtils.get(cacheKey);

            if (result == null) {
                return null;
            }

            return convertToDeviceStatusCache(result, deviceId);

        } catch (Exception e) {
            log.error("获取{}失败: deviceId={}, error={}", statusType, deviceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除设备状态缓存
     *
     * @param deviceId 设备ID
     */
    public void deleteDeviceStatus(String deviceId) {
        try {
            String statusKey = DEVICE_STATUS_PREFIX + deviceId;
            String lastStatusKey = DEVICE_LAST_STATUS_PREFIX + deviceId;

            redisUtils.delete(statusKey);
            redisUtils.delete(lastStatusKey);

        } catch (Exception e) {
            log.error("删除设备状态缓存失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 检查单个AMS实例是否发生变化（排除温湿度字段）
     */
    private boolean hasAmsInstanceChanged(AmsInstance existing, AmsInstance merged) {
        // 引用相同
        if (existing == merged) {
            return false;
        }

        // 一个为null一个不为null
        if (existing == null || merged == null) {
            return true;
        }

        // 比较关键字段（排除humidity、humidityRaw、temp）
        return ObjUtil.notEqual(existing.getId(), merged.getId()) ||
                ObjUtil.notEqual(existing.getInfo(), merged.getInfo()) ||
                ObjUtil.notEqual(existing.getDrySetting(), merged.getDrySetting()) ||
                ObjUtil.notEqual(existing.getDryTime(), merged.getDryTime()) ||
                hasAmsTrayListChanged(existing.getTray(), merged.getTray());
    }

    /**
     * 检查AMS料盘列表是否发生变化
     */
    private boolean hasAmsTrayListChanged(List<AmsTray> existing, List<AmsTray> merged) {
        // 引用相同或都为null
        if (existing == merged) {
            return false;
        }

        // 一个为null一个不为null
        if (existing == null || merged == null) {
            return true;
        }

        // 大小不同
        if (existing.size() != merged.size()) {
            return true;
        }

        // 逐个比较料盘
        for (int i = 0; i < existing.size(); i++) {
            AmsTray existingTray = existing.get(i);
            AmsTray mergedTray = merged.get(i);

            if (hasAmsTrayChanged(existingTray, mergedTray)) {
                return true;
            }
        }

        return false;
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        shuttingDown.set(true);
        log.info("检测到应用关闭事件，停止设备状态缓存写入");
    }
}
