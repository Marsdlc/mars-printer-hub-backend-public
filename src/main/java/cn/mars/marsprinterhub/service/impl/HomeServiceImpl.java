package cn.mars.marsprinterhub.service.impl;

import cn.hutool.json.JSONUtil;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttService;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.SpoolUsedRecord;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.PrinterModelEnum;
import cn.mars.marsprinterhub.model.enums.PrinterStatusEnum;
import cn.mars.marsprinterhub.model.enums.TaskStatusEnum;
import cn.mars.marsprinterhub.model.vo.HomeVO;
import cn.mars.marsprinterhub.model.vo.PrinterHomeSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolHomeSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolUsedRecordHomeSummaryVO;
import cn.mars.marsprinterhub.service.*;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCache;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.mars.marsprinterhub.constant.RedisConstants.HOME_SUMMARY_PREFIX;

@Service
@Slf4j
public class HomeServiceImpl implements HomeService {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private SpoolService spoolService;

    @Resource
    private SpoolUsedRecordService spoolUsedRecordService;

    @Resource
    private PrinterService printerService;

    @Resource
    private BambuMqttService bambuMqttService;

    @Resource
    private UserService userService;

    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public HomeVO getHomeSummary() {
        User currentUser = userService.getCurrentUser();
        Long currentUserId = currentUser.getId();
        String key = HOME_SUMMARY_PREFIX + currentUserId;
        // 先从 Redis 取
        String json = (String) redisUtils.get(key);
        if (json != null) {
            return JSONUtil.toBean(json, HomeVO.class);
        }

        HomeVO homeVO = new HomeVO();

        // 获取当前用户本周任务
        List<SpoolUsedRecord> weekUsedRecords = spoolUsedRecordService.getWeekUsedRecords();
        // 按 startTime + endTime 去重（避免多色打印重复记录）
        List<SpoolUsedRecord> distinctRecords = spoolUsedRecordService.distinctByTime(weekUsedRecords);

        // 打印时长（小时）
        long totalMillis = distinctRecords.stream()
                .mapToLong(r -> r.getEndTime().getTime() - r.getStartTime().getTime())
                .sum();
        // 保留一位小数，不加单位
        double totalHours = totalMillis / 1000.0 / 60 / 60;
        homeVO.setWeeklyUsedTime(Math.round(totalHours * 10) / 10.0);

        // 成功任务数
        long successCount = distinctRecords.stream()
                .filter(r -> Objects.equals(r.getStatus(), TaskStatusEnum.SUCCESS.getValue()))
                .count();
        homeVO.setSuccessfulPrints((int) successCount);

        // 耗材总消耗量
        BigDecimal totalUsedWeight = weekUsedRecords.stream()
                .map(SpoolUsedRecord::getActualWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        homeVO.setTotalUsedSpoolsWeight(totalUsedWeight);

        // 获取所有打印机
        List<Printer> printerList = printerService.getListByUserId(currentUserId);
        if (!isIntranet && !currentUser.isVip()) {
            printerList = printerList.stream()
                    .filter(printer -> bambuMqttService.isConnected(printer.getDeviceId()))
                    .toList();
        }
        List<PrinterHomeSummaryVO> printerHomeSummaryVOList = printerList.stream()
                .map(printer -> {
                    String deviceId = printer.getDeviceId();
                    DeviceStatusCache deviceStatus = deviceStatusCacheService.getDeviceStatus(deviceId);
                    PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(printer.getModel());
                    String modelText = printerModelEnum == null ? printer.getModel() : printerModelEnum.getText();

                    return PrinterHomeSummaryVO.builder()
                            .id(printer.getId())
                            .name(printer.getName())
                            .deviceId(deviceId)
                            .model(modelText)
                            .status(deviceStatus == null || deviceStatus.getPrintStatus() == null ? PrinterStatusEnum.OFFLINE.getValue() : deviceStatus.getPrintStatus())
                            .mcPercent(deviceStatus == null || deviceStatus.getPrintProgress() == null ? 0 : deviceStatus.getMcPercent())
                            .remainingTime(deviceStatus == null || deviceStatus.getRemainingTime() == null ? 0 : deviceStatus.getRemainingTime())
                            .sort(printer.getSort())
                            .build();
                })
                .collect(Collectors.toList());

        homeVO.setPrinters(printerHomeSummaryVOList);

        // 最近添加的耗材(最新5个)
        List<SpoolHomeSummaryVO> spoolsLast5 = spoolService.getLatest5Spools().stream()
                .map(spoolService::convertToVO)
                .map(spoolService::convertToHomeSummaryVO)
                .toList();
        homeVO.setRecentlyAdded(spoolsLast5);

        // 即将用完的耗材(剩余重量<10%)
        List<SpoolHomeSummaryVO> lowStock = spoolService.getSpoolsByRemainingPercentageLessThan10().stream()
                .map(spoolService::convertToVO)
                .map(spoolService::convertToHomeSummaryVO)
                .toList();
        homeVO.setLowStock(lowStock);

        // 最近10条使用记录
        List<SpoolUsedRecord> spoolUsedRecords = spoolUsedRecordService.getLatest10Records();
        List<SpoolUsedRecordHomeSummaryVO> spoolUsedRecordVOList = spoolUsedRecords.stream()
                .map(spoolUsedRecordService::convertToHomeSummaryVO)
                .toList();

        homeVO.setLast10UsedRecords(spoolUsedRecordVOList);

        // 存入 redis
        long timeout = isIntranet ? 10 : 60;
        redisUtils.set(key, JSONUtil.toJsonStr(homeVO), timeout);
        return homeVO;
    }
}
