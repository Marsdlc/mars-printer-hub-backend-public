package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.OrderRecalculateEvent;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.mapper.SpoolUsedRecordMapper;
import cn.mars.marsprinterhub.model.dto.record.RecordQueryDTO;
import cn.mars.marsprinterhub.model.dto.user.UserConfigDTO;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.Spool;
import cn.mars.marsprinterhub.model.entity.SpoolUsedRecord;
import cn.mars.marsprinterhub.model.enums.TaskStatusEnum;
import cn.mars.marsprinterhub.model.vo.SpoolUsedRecordHomeSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolUsedRecordVO;
import cn.mars.marsprinterhub.service.*;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.mars.marsprinterhub.constant.RedisConstants.SPOOL_SUMMARY_PREFIX;

/**
 * 针对表【spool_used_records(耗材使用记录)】的数据库操作Service实现
 */
@Service
@Slf4j
public class SpoolUsedRecordServiceImpl extends ServiceImpl<SpoolUsedRecordMapper, SpoolUsedRecord>
        implements SpoolUsedRecordService {

    @Resource
    private UserService userService;

    @Resource
    private SpoolService spoolService;

    @Resource
    private PrinterService printerService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserConfigService userConfigService;

    @Resource
    private EventPublisher eventPublisher;

    @Override
    public IPage<SpoolUsedRecord> getRecordPage(RecordQueryDTO queryDTO) {
        Long currentUserId = userService.getCurrentUserId();

        LambdaQueryWrapper<SpoolUsedRecord> queryWrapper = new LambdaQueryWrapper<SpoolUsedRecord>()
                .eq(SpoolUsedRecord::getUserId, currentUserId)
                .like(StrUtil.isNotBlank(queryDTO.getTitle()), SpoolUsedRecord::getTitle, queryDTO.getTitle())
                .eq(queryDTO.getSpoolId() != null, SpoolUsedRecord::getSpoolId, queryDTO.getSpoolId())
                .eq(StrUtil.isNotBlank(queryDTO.getDeviceId()), SpoolUsedRecord::getDeviceId, queryDTO.getDeviceId())
                .eq(queryDTO.getStatus() != null, SpoolUsedRecord::getStatus, queryDTO.getStatus())
                .eq(queryDTO.getOrderId() != null, SpoolUsedRecord::getOrderId, queryDTO.getOrderId())
                .orderByDesc(SpoolUsedRecord::getCreateTime);
        Integer isBindOrder = queryDTO.getIsBindOrder();
        if (isBindOrder != null && isBindOrder == 1) {
            queryWrapper.isNotNull(SpoolUsedRecord::getOrderId);
        } else if (isBindOrder != null && isBindOrder == 0) {
            queryWrapper.isNull(SpoolUsedRecord::getOrderId);
        }
        return this.page(new Page<>(queryDTO.getCurrent(), queryDTO.getSize()), queryWrapper);
    }

    @Override
    public SpoolUsedRecord getBySpoolUsedRecordId(Long id) {
        // 校验参数
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "参数为空");

        // 获取当前用户ID
        Long currentUserId = userService.getCurrentUserId();

        SpoolUsedRecord spoolUsedRecord = this.getById(id);
        ThrowUtils.throwIf(spoolUsedRecord == null, ErrorCode.NOT_FOUND_ERROR, "记录不存在");
        ThrowUtils.throwIf(!currentUserId.equals(spoolUsedRecord.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限查看此记录");

        return spoolUsedRecord;
    }

    @Override
    public List<SpoolUsedRecord> listSpoolUsedRecordsBySpoolId(Long spoolId) {
        ThrowUtils.throwIf(spoolId == null, ErrorCode.PARAMS_ERROR, "参数为空");

        // 获取当前用户ID
        Long currentUserId = userService.getCurrentUserId();

        return this.list(
                new LambdaQueryWrapper<SpoolUsedRecord>()
                        .eq(SpoolUsedRecord::getSpoolId, spoolId)
                        .eq(SpoolUsedRecord::getUserId, currentUserId)
                        .orderByDesc(SpoolUsedRecord::getCreateTime)
        );
    }

    @Override
    public List<SpoolUsedRecord> listSpoolUsedRecordsByOrderId(Long orderId) {
        ThrowUtils.throwIf(orderId == null, ErrorCode.PARAMS_ERROR, "参数为空");

        // 获取当前用户ID
        Long currentUserId = userService.getCurrentUserId();

        return this.list(
                new LambdaQueryWrapper<SpoolUsedRecord>()
                        .eq(SpoolUsedRecord::getOrderId, orderId)
                        .eq(SpoolUsedRecord::getUserId, currentUserId)
                        .orderByDesc(SpoolUsedRecord::getCreateTime)
        );
    }

    @Override
    public List<SpoolUsedRecord> getLatest10Records() {
        return this.list(
                new LambdaQueryWrapper<SpoolUsedRecord>()
                        .eq(SpoolUsedRecord::getUserId, userService.getCurrentUserId())
                        .orderByDesc(SpoolUsedRecord::getCreateTime)
                        .last("limit 10")
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferSpoolUsedRecord(Long id, Long newSpoolId) {
        // 参数校验
        ThrowUtils.throwIf(id == null || newSpoolId == null, ErrorCode.PARAMS_ERROR, "参数为空");

        Long currentUserId = userService.getCurrentUserId();

        // 获取记录信息
        SpoolUsedRecord spoolUsedRecord = this.getById(id);
        ThrowUtils.throwIf(spoolUsedRecord == null, ErrorCode.NOT_FOUND_ERROR, "记录不存在");
        ThrowUtils.throwIf(!currentUserId.equals(spoolUsedRecord.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限修改此记录");

        // 获取新耗材
        Spool newSpool = spoolService.getSpoolById(newSpoolId);
        ThrowUtils.throwIf(newSpool == null, ErrorCode.NOT_FOUND_ERROR, "耗材不存在");
        ThrowUtils.throwIf(!currentUserId.equals(newSpool.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限修改此耗材");

        // 使用重量（兜底为0）
        BigDecimal actualWeight = Optional.ofNullable(spoolUsedRecord.getActualWeight()).orElse(BigDecimal.ZERO);
        BigDecimal newSpoolUsed = Optional.ofNullable(newSpool.getUsedWeight()).orElse(BigDecimal.ZERO);

        // 如果原记录本来就有耗材，则需要扣减原耗材的重量
        if (spoolUsedRecord.getSpoolId() != null) {
            // 获取旧耗材
            Spool currentSpool = spoolService.getSpoolById(spoolUsedRecord.getSpoolId());
            ThrowUtils.throwIf(currentSpool == null, ErrorCode.NOT_FOUND_ERROR, "原耗材不存在");
            ThrowUtils.throwIf(!currentUserId.equals(currentSpool.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限修改原耗材");

            BigDecimal currentSpoolUsed = Optional.ofNullable(currentSpool.getUsedWeight()).orElse(BigDecimal.ZERO);
            // 旧耗材扣回
            BigDecimal remain = currentSpoolUsed.subtract(actualWeight);
            ThrowUtils.throwIf(remain.compareTo(BigDecimal.ZERO) < 0, ErrorCode.OPERATION_ERROR, "旧耗材重量不足，无法迁移");
            currentSpool.setUsedWeight(remain);

            // 批量更新
            newSpool.setUsedWeight(newSpoolUsed.add(actualWeight));
            spoolService.updateBatchById(List.of(newSpool, currentSpool));
        } else {
            // 如果原记录没有耗材，直接给新耗材增加重量
            newSpool.setUsedWeight(newSpoolUsed.add(actualWeight));
            spoolService.updateById(newSpool);
        }

        // 删除缓存
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + currentUserId + ":false");
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + currentUserId + ":true");

        // 更新使用记录的耗材ID
        spoolUsedRecord.setSpoolId(newSpoolId);
        fillCost(spoolUsedRecord);
        this.updateById(spoolUsedRecord);

        Long orderId = spoolUsedRecord.getOrderId();
        if (orderId != null) {
            eventPublisher.publish(new OrderRecalculateEvent(orderId));
        }
    }

    @Override
    public void removeBySpoolId(Long spoolId) {
        this.remove(
                new LambdaQueryWrapper<SpoolUsedRecord>()
                        .eq(SpoolUsedRecord::getSpoolId, spoolId)
        );
    }

    @Override
    public SpoolUsedRecordVO convertToVO(SpoolUsedRecord spoolUsedRecord, boolean getSpoolVO) {
        if (spoolUsedRecord.getCost() == null) {
            fillCost(spoolUsedRecord);
        }
        Map<String, Printer> printerMap = new HashMap<>();
        Printer byDeviceId = printerService.getByDeviceId(spoolUsedRecord.getDeviceId());
        if (byDeviceId != null) {
            printerMap.put(byDeviceId.getDeviceId(), byDeviceId);
        }
        Map<Long, Spool> spoolMap = Collections.emptyMap();
        if (getSpoolVO && spoolUsedRecord.getSpoolId() != null) {
            Spool spool = spoolService.getSpoolByIdNotLogin(spoolUsedRecord.getSpoolId());
            if (spool != null) {
                spoolMap = Map.of(spool.getId(), spool);
            }
        }
        return buildVO(spoolUsedRecord, getSpoolVO, spoolMap, printerMap);
    }

    @Override
    public List<SpoolUsedRecordVO> convertToVOList(List<SpoolUsedRecord> spoolUsedRecords, boolean getSpoolVO) {
        if (spoolUsedRecords == null || spoolUsedRecords.isEmpty()) {
            return Collections.emptyList();
        }

        spoolUsedRecords.forEach(record -> {
            if (record.getCost() == null) {
                fillCost(record);
            }
        });

        Set<String> deviceIds = spoolUsedRecords.stream()
                .map(SpoolUsedRecord::getDeviceId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, Printer> printerMap = deviceIds.isEmpty()
                ? Collections.emptyMap()
                : printerService.list(
                        new LambdaQueryWrapper<Printer>()
                                .eq(Printer::getUserId, userService.getCurrentUserId())
                                .in(Printer::getDeviceId, deviceIds)
                ).stream().collect(Collectors.toMap(Printer::getDeviceId, Function.identity(), (left, right) -> left));

        Map<Long, Spool> spoolMap = Collections.emptyMap();
        if (getSpoolVO) {
            Set<Long> spoolIds = spoolUsedRecords.stream()
                    .map(SpoolUsedRecord::getSpoolId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!spoolIds.isEmpty()) {
                spoolMap = spoolService.listByIds(spoolIds).stream()
                        .collect(Collectors.toMap(Spool::getId, Function.identity(), (left, right) -> left));
            }
        }

        Map<Long, Spool> finalSpoolMap = spoolMap;
        return spoolUsedRecords.stream()
                .map(record -> buildVO(record, getSpoolVO, finalSpoolMap, printerMap))
                .toList();
    }

    private SpoolUsedRecordVO buildVO(SpoolUsedRecord spoolUsedRecord, boolean getSpoolVO, Map<Long, Spool> spoolMap,
                                      Map<String, Printer> printerMap) {
        SpoolUsedRecordVO spoolUsedRecordVO = BeanUtil.copyProperties(spoolUsedRecord, SpoolUsedRecordVO.class);
        TaskStatusEnum enumByValue = TaskStatusEnum.getEnumByValue(spoolUsedRecord.getStatus());
        if (enumByValue != null) {
            spoolUsedRecordVO.setStatusText(enumByValue.getText());
        }
        if (getSpoolVO && spoolUsedRecordVO.getSpoolId() != null) {
            Spool spool = spoolMap.get(spoolUsedRecordVO.getSpoolId());
            if (spool != null) {
                spoolUsedRecordVO.setSpoolVO(spoolService.convertToVO(spool));
            }
        }
        if (StrUtil.isNotBlank(spoolUsedRecord.getDeviceId())) {
            Printer printer = printerMap.get(spoolUsedRecord.getDeviceId());
            if (printer != null) {
                spoolUsedRecordVO.setDeviceName(printer.getName());
            }
        }
        return spoolUsedRecordVO;
    }

    @Override
    public SpoolUsedRecordHomeSummaryVO convertToHomeSummaryVO(SpoolUsedRecord spoolUsedRecord) {
        if (spoolUsedRecord.getCost() == null) {
            fillCost(spoolUsedRecord);
        }
        SpoolUsedRecordHomeSummaryVO spoolUsedRecordHomeSummaryVO = BeanUtil.copyProperties(spoolUsedRecord, SpoolUsedRecordHomeSummaryVO.class);
        TaskStatusEnum enumByValue = TaskStatusEnum.getEnumByValue(spoolUsedRecord.getStatus());
        if (enumByValue != null) {
            spoolUsedRecordHomeSummaryVO.setStatusText(enumByValue.getText());
        }
        if (spoolUsedRecordHomeSummaryVO.getSpoolId() != null) {
            spoolUsedRecordHomeSummaryVO.setSpoolName(spoolService.getSpoolNameById(spoolUsedRecordHomeSummaryVO.getSpoolId()));
        }
        return spoolUsedRecordHomeSummaryVO;
    }

    @Override
    public List<SpoolUsedRecord> getWeekUsedRecords() {
        Date date = new Date();
        return list(
                new LambdaQueryWrapper<SpoolUsedRecord>()
                        .between(SpoolUsedRecord::getCreateTime, DateUtil.beginOfWeek(date), DateUtil.endOfWeek(date))
                        .eq(SpoolUsedRecord::getUserId, userService.getCurrentUserId())
                        .in(SpoolUsedRecord::getStatus, TaskStatusEnum.SUCCESS.getValue(), TaskStatusEnum.CANCEL.getValue())
        );
    }

    @Override
    public void bindOrderRecord(Long orderId, Long recordId) {
        // 获取当前用户ID
        Long currentUserId = userService.getCurrentUserId();

        SpoolUsedRecord spoolUsedRecord = this.getById(recordId);
        ThrowUtils.throwIf(spoolUsedRecord == null, ErrorCode.NOT_FOUND_ERROR, "记录不存在");
        ThrowUtils.throwIf(!currentUserId.equals(spoolUsedRecord.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限操作此记录");

        SpoolUsedRecord update = new SpoolUsedRecord();
        update.setId(recordId);
        update.setOrderId(orderId);
        update.setCost(fillCost(spoolUsedRecord));
        this.updateById(update);
    }

    @Override
    public void unbindOrderRecord(Long orderId, Long recordId) {
        Long currentUserId = userService.getCurrentUserId();
        SpoolUsedRecord record = this.getById(recordId);
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "记录不存在");
        ThrowUtils.throwIf(!Objects.equals(currentUserId, record.getUserId()),
                ErrorCode.NO_AUTH_ERROR, "无权限操作此记录");
        ThrowUtils.throwIf(!Objects.equals(orderId, record.getOrderId()),
                ErrorCode.PARAMS_ERROR, "记录未绑定到该订单");

        boolean updated = lambdaUpdate()
                .eq(SpoolUsedRecord::getId, recordId)
                .eq(SpoolUsedRecord::getUserId, currentUserId)
                .eq(SpoolUsedRecord::getOrderId, orderId)
                .set(SpoolUsedRecord::getOrderId, null)
                .update();
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "解绑订单记录失败");
    }

    @Override
    public boolean deleteSpoolUsedRecord(Long id) {
        // 获取当前用户ID
        Long currentUserId = userService.getCurrentUserId();

        SpoolUsedRecord spoolUsedRecord = this.getById(id);
        ThrowUtils.throwIf(spoolUsedRecord == null, ErrorCode.NOT_FOUND_ERROR, "记录不存在");
        ThrowUtils.throwIf(!currentUserId.equals(spoolUsedRecord.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限操作此记录");

        // 对应耗材增加回使用重量
        BigDecimal actualWeight = spoolUsedRecord.getActualWeight();
        Long spoolId = spoolUsedRecord.getSpoolId();
        if (spoolId != null && actualWeight != null) {
            Spool spool = spoolService.getSpoolById(spoolId);
            if (spool != null) {
                BigDecimal weight = spool.getUsedWeight().subtract(actualWeight);
                if (weight.compareTo(BigDecimal.ZERO) < 0) {
                    weight = BigDecimal.ZERO;
                }
                spoolService.checkSpool(spool.getId(), weight);
            }
        }
        // 删除记录
        return this.removeById(id);
    }

    @Override
    public List<SpoolUsedRecord> distinctByTime(List<SpoolUsedRecord> spoolUsedRecords) {
        return spoolUsedRecords.stream()
                .collect(Collectors.toMap(
                        r -> {
                            if (r.getTaskId() != null) {
                                return "TASK_" + r.getTaskId();
                            }
                            return "TIME_" + r.getStartTime().getTime() + "_" + r.getEndTime().getTime();
                        },
                        Function.identity(),
                        (r1, r2) -> r1
                ))
                .values()
                .stream()
                .toList();
    }

    @Override
    public void updateByOrderId(Long orderId) {
        lambdaUpdate()
                .eq(SpoolUsedRecord::getOrderId, orderId)
                .set(SpoolUsedRecord::getOrderId, null)
                .update();
    }

    @Override
    public SpoolUsedRecord getByTaskIdAndAmsRef(Long taskId, Integer amsRef) {
        if (taskId == null || amsRef == null) {
            return null;
        }
        return lambdaQuery()
                .eq(SpoolUsedRecord::getTaskId, taskId)
                .eq(SpoolUsedRecord::getAmsRef, amsRef)
                .one();
    }

    @Override
    public BigDecimal fillCost(SpoolUsedRecord record) {
        if (record == null) {
            return BigDecimal.ZERO;
        }

        if (record.getUserId() == null) {
            record.setCost(BigDecimal.ZERO);
            record.setElectricityCost(BigDecimal.ZERO);
            record.setDepreciationCost(BigDecimal.ZERO);
            return BigDecimal.ZERO;
        }
        UserConfigDTO config = userConfigService.getUserConfigByUserId(record.getUserId());

        BigDecimal durationHours = BigDecimal.ZERO;
        if (record.getStartTime() != null && record.getEndTime() != null && !record.getEndTime().before(record.getStartTime())) {
            long millis = record.getEndTime().getTime() - record.getStartTime().getTime();
            durationHours = BigDecimal.valueOf(millis)
                    .divide(BigDecimal.valueOf(1000 * 60 * 60), 4, RoundingMode.HALF_UP);
        }

        BigDecimal powerKw = defaultZero(config.getPowerWatt())
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        BigDecimal electricityCost = durationHours
                .multiply(powerKw.multiply(defaultZero(config.getElectricityPricePerKwh())))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal depreciationCost = durationHours
                .multiply(defaultZero(config.getDepreciationPerHour()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal materialCost = BigDecimal.ZERO;
        if (record.getSpoolId() != null) {
            Spool spool = spoolService.getSpoolByIdNotLogin(record.getSpoolId());
            if (spool != null) {
                BigDecimal price = defaultZero(spool.getPrice());
                BigDecimal netWeight = defaultZero(spool.getNetWeight());
                BigDecimal actualWeight = defaultZero(record.getActualWeight());
                if (netWeight.compareTo(BigDecimal.ZERO) > 0) {
                    materialCost = price.multiply(actualWeight)
                            .divide(netWeight, 6, RoundingMode.HALF_UP);
                }
            }
        }

        BigDecimal totalCost = electricityCost.add(depreciationCost).add(materialCost).setScale(2, RoundingMode.HALF_UP);
        record.setElectricityCost(electricityCost);
        record.setDepreciationCost(depreciationCost);
        record.setCost(totalCost);
        return totalCost;
    }

    @Override
    public void createSimpleRecord(Spool spool, BigDecimal usedWeight, String title, Date startTime, Date endTime) {
        if (spool == null || usedWeight == null || usedWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        SpoolUsedRecord usedRecord = new SpoolUsedRecord();
        usedRecord.setUserId(spool.getUserId());
        usedRecord.setSpoolId(spool.getId());
        usedRecord.setTitle(title);
        usedRecord.setStatus(TaskStatusEnum.SUCCESS.getValue());
        usedRecord.setPrintProgress(100);
        usedRecord.setTaskWeight(usedWeight);
        usedRecord.setActualWeight(usedWeight);
        usedRecord.setStartTime(startTime);
        usedRecord.setEndTime(endTime);
        fillCost(usedRecord);
        save(usedRecord);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
