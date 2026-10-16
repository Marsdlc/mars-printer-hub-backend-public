package cn.mars.marsprinterhub.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.model.AmsInstance;
import cn.mars.marsprinterhub.bambu.model.AmsStatus;
import cn.mars.marsprinterhub.bambu.model.AmsTray;
import cn.mars.marsprinterhub.bambu.service.BambuCloudService;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.OrderRecalculateEvent;
import cn.mars.marsprinterhub.event.events.StalePrintTaskCheckEvent;
import cn.mars.marsprinterhub.model.dto.bambu.AmsDetailMappingDTO;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskDTO;
import cn.mars.marsprinterhub.model.entity.*;
import cn.mars.marsprinterhub.model.enums.DeviceConfigItem;
import cn.mars.marsprinterhub.model.enums.TaskStatusEnum;
import cn.mars.marsprinterhub.service.*;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCache;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static cn.mars.marsprinterhub.constant.RedisConstants.*;

@Service
@Slf4j
public class AutoSpoolProcessor {

    private static final String AUTO_REPLENISH_TITLE = "自动续料";
    private static final String MANUAL_REPLENISH_TITLE = "手动续料";

    @Resource
    @Lazy
    private BambuCloudService bambuCloudService;

    @Resource
    private PrinterService printerService;

    @Resource
    private AmsService amsService;

    @Resource
    private SpoolService spoolService;

    @Resource
    private UserService userService;

    @Resource
    private SpoolUsedRecordService spoolUsedRecordService;

    @Resource
    private SpoolUsedSnapshotsService spoolUsedSnapshotsService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private EventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void startTask(String deviceId) {
        log.info("开始处理打印任务开始事件: deviceId={}", deviceId);

        // 获取基础信息
        Printer printer = printerService.getByDeviceId(deviceId);
        if (printer == null) return;

        Long userId = printer.getUserId();
        User user = userService.getById(userId);
        if (user == null) return;

        // 旧任务兜底属于低优先级补偿逻辑，异步发布事件处理，不阻塞当前新任务建记录。
        eventPublisher.publish(new StalePrintTaskCheckEvent(deviceId));

        // 获取任务信息
        BambuTaskDTO latestTask = bambuCloudService.getLatestTaskForPrinter(user, deviceId, user.getBambuToken());
        if (latestTask == null || CollUtil.isEmpty(latestTask.getAmsDetailMapping())) return;

        Integer status = latestTask.getStatus();
        Long taskId = latestTask.getId(); // Bambu task ID (subtask_id)
        String title = StrUtil.isBlank(latestTask.getDesignTitle()) ? latestTask.getTitle() : latestTask.getDesignTitle();

        // 遍历每个 AMS 槽位创建记录
        for (AmsDetailMappingDTO mapping : latestTask.getAmsDetailMapping()) {
            Integer amsRef = mapping.getAms();
            if (amsRef == null) continue;

            // 检查是否已存在记录
            SpoolUsedRecord existing = spoolUsedRecordService.getByTaskIdAndAmsRef(taskId, amsRef);
            if (existing != null) continue;

            // 创建新记录
            SpoolUsedRecord record = new SpoolUsedRecord();
            record.setUserId(userId);
            record.setTaskId(taskId);
            record.setDeviceId(deviceId);
            record.setAmsRef(amsRef);
            record.setTitle(title);
            record.setStatus(status);
            ZoneId zone = ZoneId.of("Asia/Shanghai"); // 或 ZoneId.systemDefault()
            LocalDateTime start = LocalDateTime.ofInstant(Instant.parse(latestTask.getStartTime()), zone);
            record.setStartTime(Date.from(start.atZone(zone).toInstant()));

            // 尝试关联耗材
            AmsSlots amsSlots = amsService.getAmsSlotsByPrinterIdAndTrayNow(printer.getId(), printer.getModel(), amsRef);
            if (amsSlots != null && amsSlots.getSpoolId() != null) {
                record.setSpoolId(amsSlots.getSpoolId());
            }

            spoolUsedRecordService.save(record);
            log.info("创建打印任务记录: deviceId={}, taskId={}, amsRef={}", deviceId, taskId, amsRef);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(String deviceId, Integer mcPercent, BambuTaskDTO taskDTO) {
        log.info("==========================================");
        log.info("开始处理设备耗材更新: deviceId={}, mcPercent={}", deviceId, mcPercent);
        long startTime = System.currentTimeMillis();

        String redisSnapshotsKey = DEVICE_IMAGES_PREFIX + deviceId;
        String redisTaskKey = DEVICE_TASK_PREFIX + deviceId;
        String redisSummaryKeyFalse = SPOOL_SUMMARY_PREFIX + deviceId + ":false";
        String redisSummaryKeyTrue = SPOOL_SUMMARY_PREFIX + deviceId + ":true";
        String redisUserKey = DEVICE_USER_PREFIX + deviceId;

        // 获取设备归属的用户 id
        Long userId = redisUtils.getLong(redisUserKey);
        if (userId == null) {
            log.warn("Redis中未找到设备用户缓存，重新构建缓存: deviceId={}", deviceId);
            printerService.createCache();
            userId = redisUtils.getLong(redisUserKey);
            if (userId == null) {
                log.error("重新构建缓存后仍未找到用户id: deviceId={}", deviceId);
                return;
            }
        }

        // 获取打印机信息
        Printer printer = printerService.getByDeviceId(deviceId);
        if (printer == null) {
            log.error("打印机信息不存在: deviceId={}", deviceId);
            return;
        }

        // 根据设备 id 获取拓竹 token
        User user = userService.getById(userId);
        if (user == null || user.getBambuToken() == null || user.getBambuTokenExpireTime() == null || user.getBambuTokenExpireTime().before(new Date())) {
            log.error("拓竹token已过期或不存在: userId={}", userId);
            return;
        }

        // 获取打印任务信息
        BambuTaskDTO latestTask;
        if (taskDTO == null) {
            latestTask = bambuCloudService.getLatestTaskForPrinter(user, deviceId, user.getBambuToken());
        } else {
            latestTask = taskDTO;
        }

        if (latestTask == null) {
            log.error("获取打印任务失败: deviceId={}", deviceId);
            return;
        }

        // 打印进度为99，并且状态不为已取消，则将打印进度设置为100
        if (mcPercent == 99 && latestTask.getStatus() != TaskStatusEnum.CANCEL.getValue()) {
            mcPercent = 100;
        }

        String title = StrUtil.isBlank(latestTask.getDesignTitle()) ? latestTask.getTitle() : latestTask.getDesignTitle();
        log.info("获取打印任务信息: deviceId={}, task={}, taskId={}", deviceId, title, latestTask.getId());
        Long taskId = latestTask.getId();

        // 获取耗材使用信息
        List<AmsDetailMappingDTO> amsDetailMapping = latestTask.getAmsDetailMapping();
        log.info("获取耗材使用信息: deviceId={}, amsDetailMappingSize={}", deviceId, amsDetailMapping != null ? amsDetailMapping.size() : 0);
        if (CollUtil.isEmpty(amsDetailMapping)) {
            log.error("AMS详情映射为空: deviceId={}", deviceId);
            return;
        }
        int amsSize = amsDetailMapping.size();
        Set<Long> orderIdsToRecalculate = new HashSet<>();

        Long finalUserId = userId;
        Integer finalMcPercent = mcPercent;
        amsDetailMapping.forEach(amsDetail -> {
            Integer ams = amsDetail.getAms();
            if (ams == null) return;

            // 查找对应的记录
            SpoolUsedRecord spoolUsedRecord = spoolUsedRecordService.getByTaskIdAndAmsRef(taskId, ams);
            if (spoolUsedRecord == null) {
                log.warn("未找到任务记录，尝试补录: taskId={}, amsRef={}", taskId, ams);
                // 可能是旧数据或者异常情况，尝试新建
                spoolUsedRecord = new SpoolUsedRecord();
                spoolUsedRecord.setUserId(finalUserId);
                spoolUsedRecord.setTaskId(taskId);
                spoolUsedRecord.setDeviceId(deviceId);
                spoolUsedRecord.setAmsRef(ams);
                spoolUsedRecord.setTitle(title);
                ZoneId zone = ZoneId.of("Asia/Shanghai"); // 或 ZoneId.systemDefault()
                LocalDateTime start = LocalDateTime.ofInstant(Instant.parse(latestTask.getStartTime()), zone);
                spoolUsedRecord.setStartTime(Date.from(start.atZone(zone).toInstant()));
            }

            // 更新通用信息
            spoolUsedRecord.setDeviceId(deviceId);
            spoolUsedRecord.setEndTime(new Date());
            spoolUsedRecord.setStatus(finalMcPercent == 100 ? TaskStatusEnum.SUCCESS.getValue() : TaskStatusEnum.CANCEL.getValue());
            spoolUsedRecord.setPrintProgress(finalMcPercent);
            Object coverUrl = redisUtils.get(redisTaskKey);
            spoolUsedRecord.setCoverUrl(coverUrl == null ? null : coverUrl.toString());

            // 获取AMS和槽位信息 (此时再次检查绑定情况，即使用户中途才绑定也能关联上)
            AmsSlots amsSlots = amsService.getAmsSlotsByPrinterIdAndTrayNow(printer.getId(), printer.getModel(), ams);
            if (amsSlots != null && amsSlots.getSpoolId() != null) {
                // 有绑定耗材
                Long spoolId = amsSlots.getSpoolId();
                Spool spool = spoolService.getSpoolByIdNotLogin(spoolId);

                if (spool != null) {
                    // 关联耗材
                    spoolUsedRecord.setSpoolId(spoolId);

                    // 多色打印，如果打印进度超过了70，不考虑成功或者失败，都按成功的量来扣
                    Integer finalPercent = (amsSize > 1 && finalMcPercent > 70) ? 100 : finalMcPercent;

                    // 更新耗材使用量 (提取为方法)
                    updateSpoolUsage(amsDetail, deviceId, spool, finalPercent, printer);
                }
            } else {
                log.warn("任务完成时仍未绑定耗材，仅记录任务信息: taskId={}, amsRef={}", taskId, ams);
            }

            // 保存记录
            spoolUsedRecord.setTaskWeight(BigDecimal.valueOf(amsDetail.getWeight()));
            if (spoolUsedRecord.getActualWeight() == null && spoolUsedRecord.getPrintProgress() != null) {
                spoolUsedRecord.setActualWeight(calculateUsedWeight(amsDetail.getWeight(), spoolUsedRecord.getPrintProgress()));
            }
            spoolUsedRecordService.fillCost(spoolUsedRecord);

            spoolUsedRecordService.saveOrUpdate(spoolUsedRecord);

            if (spoolUsedRecord.getOrderId() != null) {
                orderIdsToRecalculate.add(spoolUsedRecord.getOrderId());
            }

            // 创建耗材使用快照 (如果有)
            if (spoolUsedRecord.getId() != null) {
                createSpoolUsedSnapshots(redisSnapshotsKey, deviceId, spoolUsedRecord.getId());
            }
        });

        orderIdsToRecalculate.forEach(orderId -> eventPublisher.publish(new OrderRecalculateEvent(orderId)));

        // 删除缓存
        redisUtils.delete(redisSummaryKeyFalse);
        redisUtils.delete(redisSummaryKeyTrue);
        redisUtils.delete(redisSnapshotsKey);
        // 封面图片暂不删除，直到下一次任务开始时，自动覆盖掉
//        redisUtils.delete(redisTaskKey);

        long endTime = System.currentTimeMillis();
        log.info("设备耗材更新处理完成: deviceId={}, 耗时={}ms", deviceId, (endTime - startTime));
        log.info("==========================================");
    }

    // 修改 updateSpoolUsage 方法，不再负责创建记录，只负责扣减耗材
    private void updateSpoolUsage(AmsDetailMappingDTO amsDetail, String deviceId, Spool spool, Integer mcPercent, Printer printer) {
        Double weight = amsDetail.getWeight();
        log.info("开始更新耗材库存: deviceId={}, spoolId={}, weight={}, mcPercent={}",
                deviceId, spool.getId(), weight, mcPercent);

        if (weight == null || mcPercent == null) return;

        BigDecimal usedWeight = calculateUsedWeight(weight, mcPercent);
        BigDecimal newUsedWeight = spool.getUsedWeight().add(usedWeight);

        spool.setUsedWeight(newUsedWeight);
        if (spool.getFirstUsedTime() == null) {
            spool.setFirstUsedTime(new Date());
        }
        spool.setLastUsedTime(new Date());

        if (sysConfigService.isDeviceConfigEnabled(DeviceConfigItem.AUTO_REPLENISH_SPLIT_DEDUCTION)) {
            if (spool.getUsedWeight().compareTo(spool.getNetWeight()) >= 0) {
                // 原耗材直接归零
                spool.setUsedWeight(spool.getNetWeight());
                // 计算超出部分，开始执行自动续料
                BigDecimal overflowWeight = newUsedWeight.subtract(spool.getNetWeight());
                // 自动续料处理，若未发生自动续料则补记手动续料
                boolean replenished = autoReplenish(amsDetail, overflowWeight, printer);
                if (!replenished) {
                    createManualReplenishRecord(spool, overflowWeight);
                }
            }
        }

        spoolService.updateById(spool);

    }

    /**
     * 自动续料入口：
     * 以当前 amsDetail 对应槽位为起点，从 Redis 记录的后续已使用槽位中顺序查找可续料槽位。
     */
    private boolean autoReplenish(AmsDetailMappingDTO amsDetail, BigDecimal overflowWeight, Printer printer) {
        if (overflowWeight == null || overflowWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        String deviceId = printer.getDeviceId();
        DeviceStatusCache deviceStatus = deviceStatusCacheService.getDeviceStatus(deviceId);
        if (deviceStatus == null) {
            log.warn("设备状态为空，无法进行自动续料: deviceId={}", deviceId);
            return false;
        }
        Integer amsRef = amsDetail.getAms();
        if (amsRef == null) {
            return false;
        }

        AmsSlots currentSlot = amsService.getAmsSlotsByPrinterIdAndTrayNow(printer.getId(), printer.getModel(), amsRef);
        if (currentSlot == null || currentSlot.getAmsId() == null || currentSlot.getSlotNo() == null) {
            return false;
        }
        Ams currentAms = amsService.getById(currentSlot.getAmsId());
        if (currentAms == null || currentAms.getAmsNo() == null) {
            return false;
        }

        AmsSlotRef currentSlotRef = getUsedSlotRef(deviceId, currentAms.getAmsNo(), currentSlot.getSlotNo());
        AmsTray currentTray = getTrayFromCacheOrStatus(currentSlotRef, deviceStatus.getAmsStatus(), currentAms.getAmsNo(), currentSlot.getSlotNo());
        if (currentTray == null || StrUtil.isBlank(currentTray.getTrayType()) || StrUtil.isBlank(currentTray.getTrayColor())) {
            log.warn("当前槽位托盘信息缺失，跳过自动续料: deviceId={}, amsNo={}, slotNo={}",
                    deviceId, currentAms.getAmsNo(), currentSlot.getSlotNo());
            return false;
        }

        List<AmsSlotRef> candidateRefs = getFollowingUsedSlots(deviceId, currentAms.getAmsNo(), currentSlot.getSlotNo());
        if (CollUtil.isEmpty(candidateRefs)) {
            return false;
        }

        // 后续槽位按打印过程中的实际切换顺序轮询，直到超出重量被完全分摊。
        BigDecimal remainingOverflowWeight = overflowWeight;
        boolean replenished = false;
        for (AmsSlotRef candidateRef : candidateRefs) {
            if (remainingOverflowWeight.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            Ams candidateAms = amsService.getAmsByPrinterIdAndAmsNo(printer.getId(), candidateRef.amsNo());
            if (candidateAms == null) {
                continue;
            }
            AmsSlots candidateSlot = amsService.getSlotsListByUserId(printer.getUserId()).stream()
                    .filter(slot -> Objects.equals(slot.getAmsId(), candidateAms.getId()))
                    .filter(slot -> Objects.equals(slot.getSlotNo(), candidateRef.slotNo()))
                    .findFirst()
                    .orElse(null);
            if (candidateSlot == null || candidateSlot.getSpoolId() == null) {
                continue;
            }

            AmsTray candidateTray = getTrayFromCacheOrStatus(candidateRef, deviceStatus.getAmsStatus(), candidateRef.amsNo(), candidateRef.slotNo());
            if (!isSameTray(currentTray, candidateTray)) {
                continue;
            }

            Spool candidateSpool = spoolService.getSpoolByIdNotLogin(candidateSlot.getSpoolId());
            if (candidateSpool == null || candidateSpool.getNetWeight() == null) {
                continue;
            }

            BigDecimal candidateUsedWeight = candidateSpool.getUsedWeight() == null ? BigDecimal.ZERO : candidateSpool.getUsedWeight();
            BigDecimal candidateRemainingWeight = candidateSpool.getNetWeight().subtract(candidateUsedWeight);
            if (candidateRemainingWeight.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal currentDeductWeight = remainingOverflowWeight.min(candidateRemainingWeight);
            applyAutoReplenishUsage(candidateSpool, currentDeductWeight);
            replenished = true;

            remainingOverflowWeight = remainingOverflowWeight.subtract(currentDeductWeight);
            log.info("自动续料扣减成功: deviceId={}, amsNo={}, slotNo={}, spoolId={}, deductWeight={}",
                    deviceId, candidateRef.amsNo(), candidateRef.slotNo(), candidateSpool.getId(), currentDeductWeight);
        }

        return replenished;
    }

    private void createManualReplenishRecord(Spool spool, BigDecimal remainingOverflowWeight) {
        if (spool == null || remainingOverflowWeight == null || remainingOverflowWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Date now = new Date();
        spoolUsedRecordService.createSimpleRecord(
                spool,
                remainingOverflowWeight.setScale(2, RoundingMode.HALF_UP),
                MANUAL_REPLENISH_TITLE,
                now,
                now
        );
        log.info("自动续料未命中，已补记手动续料记录: spoolId={}, usedWeight={}",
                spool.getId(), remainingOverflowWeight);
    }

    /**
     * 从 Redis 的槽位使用轨迹中，取出“当前槽位之后”出现的所有槽位。
     */
    private List<AmsSlotRef> getFollowingUsedSlots(String deviceId, Integer currentAmsNo, Integer currentSlotNo) {
        List<AmsSlotRef> orderedSlots = getOrderedUsedSlots(deviceId);
        if (CollUtil.isEmpty(orderedSlots)) {
            return List.of();
        }

        int currentIndex = -1;
        for (int i = 0; i < orderedSlots.size(); i++) {
            AmsSlotRef slotRef = orderedSlots.get(i);
            if (Objects.equals(currentAmsNo, slotRef.amsNo()) && Objects.equals(currentSlotNo, slotRef.slotNo())) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0 || currentIndex + 1 >= orderedSlots.size()) {
            return List.of();
        }

        return new ArrayList<>(orderedSlots.subList(currentIndex + 1, orderedSlots.size()));
    }

    private AmsSlotRef getUsedSlotRef(String deviceId, Integer amsNo, Integer slotNo) {
        return getOrderedUsedSlots(deviceId).stream()
                .filter(slotRef -> Objects.equals(amsNo, slotRef.amsNo()) && Objects.equals(slotNo, slotRef.slotNo()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从 Redis 的槽位使用轨迹中取出完整轨迹，优先保留打印时记录下来的托盘信息。
     */
    private List<AmsSlotRef> getOrderedUsedSlots(String deviceId) {
        String redisAmsKey = DEVICE_TASK_AMS_PREFIX + deviceId;
        Set<Object> objects = redisUtils.zGetAll(redisAmsKey);
        if (CollUtil.isEmpty(objects)) {
            return List.of();
        }

        List<Object> orderedSlots = new ArrayList<>(objects);
        List<AmsSlotRef> result = new ArrayList<>();
        for (Object orderedSlot : orderedSlots) {
            AmsSlotRef slotRef = parseAmsSlotRef(orderedSlot);
            if (slotRef == null) {
                continue;
            }
            result.add(slotRef);
        }
        return result;
    }

    /**
     * 解析 Redis 中的 amsNo_slotNo|trayInfoIdx|trayType|trayColor 结构，兼容旧的 amsNo_slotNo 格式。
     */
    private AmsSlotRef parseAmsSlotRef(Object value) {
        if (value == null) {
            return null;
        }
        String[] segments = String.valueOf(value).split("\\|", -1);
        String[] parts = segments[0].split("_");
        if (parts.length != 2 || !StrUtil.isAllCharMatch(parts[0], Character::isDigit) || !StrUtil.isAllCharMatch(parts[1], Character::isDigit)) {
            return null;
        }
        return new AmsSlotRef(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                segments.length > 1 ? segments[1] : null,
                segments.length > 2 ? segments[2] : null,
                segments.length > 3 ? segments[3] : null
        );
    }

    private AmsTray getTrayFromCacheOrStatus(AmsSlotRef slotRef, AmsStatus amsStatus, Integer amsNo, Integer slotNo) {
        AmsTray cacheTray = slotRef == null ? null : slotRef.toTray();
        if (cacheTray != null && (StrUtil.isNotBlank(cacheTray.getTrayInfoIdx())
                || StrUtil.isNotBlank(cacheTray.getTrayType())
                || StrUtil.isNotBlank(cacheTray.getTrayColor()))) {
            return cacheTray;
        }
        return getTray(amsStatus, amsNo, slotNo);
    }

    /**
     * 从实时设备状态中获取指定 amsNo/slotNo 的托盘信息。
     */
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

    /**
     * 自动续料要求 trayInfoIdx、trayType、trayColor 三个字段都一致。
     */
    private boolean isSameTray(AmsTray currentTray, AmsTray candidateTray) {
        if (currentTray == null || candidateTray == null) {
            return false;
        }
        return StrUtil.equalsIgnoreCase(normalizeTrayValue(currentTray.getTrayInfoIdx()), normalizeTrayValue(candidateTray.getTrayInfoIdx()))
                && StrUtil.equalsIgnoreCase(normalizeTrayValue(currentTray.getTrayType()), normalizeTrayValue(candidateTray.getTrayType()))
                && StrUtil.equalsIgnoreCase(normalizeTrayValue(currentTray.getTrayColor()), normalizeTrayValue(candidateTray.getTrayColor()));
    }

    /**
     * 托盘字段统一做 trim / 去 #，避免颜色格式差异影响比较。
     */
    private String normalizeTrayValue(String value) {
        return StrUtil.blankToDefault(StrUtil.removePrefixIgnoreCase(StrUtil.trim(value), "#"), "");
    }

    /**
     * 内部自动续料扣重：
     * 直接更新耗材重量并补一条“自动续料”使用记录，不依赖登录态。
     */
    private void applyAutoReplenishUsage(Spool spool, BigDecimal usedWeight) {
        if (spool == null || usedWeight == null || usedWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Date now = new Date();
        BigDecimal currentUsedWeight = spool.getUsedWeight() == null ? BigDecimal.ZERO : spool.getUsedWeight();
        BigDecimal newUsedWeight = currentUsedWeight.add(usedWeight);
        if (spool.getNetWeight() != null && newUsedWeight.compareTo(spool.getNetWeight()) > 0) {
            newUsedWeight = spool.getNetWeight();
        }
        spool.setUsedWeight(newUsedWeight);
        if (spool.getFirstUsedTime() == null) {
            spool.setFirstUsedTime(now);
        }
        spool.setLastUsedTime(now);
        spoolService.updateById(spool);
        spoolUsedRecordService.createSimpleRecord(spool, usedWeight, AUTO_REPLENISH_TITLE, now, now);
    }

    private BigDecimal calculateUsedWeight(Double weight, Integer mcPercent) {
        return BigDecimal.valueOf(weight * mcPercent / 100.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void createSpoolUsedSnapshots(String redisSnapshotsKey, String deviceId, Long recordId) {
        Map<Object, Object> allSnapshots = redisUtils.hGetAll(redisSnapshotsKey);

        if (MapUtil.isEmpty(allSnapshots)) {
            log.warn("快照数据为空，跳过保存: deviceId={}, recordId={}", deviceId, recordId);
            return;
        }

        try {
            // 转换快照
            List<SpoolUsedSnapshots> insertList = getSpoolUsedSnapshots(recordId, allSnapshots);

            if (CollUtil.isNotEmpty(insertList)) {
                spoolUsedSnapshotsService.insertBatchIgnore(insertList);
            }
        } catch (Exception e) {
            log.error("保存耗材快照失败，deviceId={}, recordId={}, error={}", deviceId, recordId, e.getMessage(), e);
        }
    }

    private List<SpoolUsedSnapshots> getSpoolUsedSnapshots(Long recordId, Map<Object, Object> allSnapshots) {
        List<SpoolUsedSnapshots> insertList = new ArrayList<>(allSnapshots.size());
        int skipCount = 0;
        for (Map.Entry<Object, Object> entry : allSnapshots.entrySet()) {
            Object percentKey = entry.getKey();
            Object imageVal = entry.getValue();

            if (percentKey == null || imageVal == null) {
                skipCount++;
                continue; // 跳过脏数据
            }

            SpoolUsedSnapshots snapshots = new SpoolUsedSnapshots();
            snapshots.setRecordId(recordId);
            try {
                snapshots.setPercent(Integer.parseInt(percentKey.toString()));
                snapshots.setImageUrl(imageVal.toString());
                insertList.add(snapshots);
            } catch (NumberFormatException e) {
                log.warn("跳过无效的快照数据: recordId={}, percentKey={}, error={}", recordId, percentKey, e.getMessage());
                skipCount++;
            }
        }
        if (skipCount > 0) {
            log.info("跳过无效快照数据: recordId={}, skipCount={}", recordId, skipCount);
        }
        return insertList;
    }

    private record AmsSlotRef(Integer amsNo, Integer slotNo, String trayInfoIdx, String trayType, String trayColor) {
        private AmsTray toTray() {
            AmsTray tray = new AmsTray();
            tray.setTrayInfoIdx(trayInfoIdx);
            tray.setTrayType(trayType);
            tray.setTrayColor(trayColor);
            return tray;
        }
    }
}
