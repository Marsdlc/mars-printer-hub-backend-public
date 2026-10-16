package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.mapper.SpoolMapper;
import cn.mars.marsprinterhub.model.dto.spool.SpoolQueryDTO;
import cn.mars.marsprinterhub.model.entity.Spool;
import cn.mars.marsprinterhub.model.entity.SpoolUsedRecord;
import cn.mars.marsprinterhub.model.enums.PrintingAppearanceEnum;
import cn.mars.marsprinterhub.model.enums.TaskStatusEnum;
import cn.mars.marsprinterhub.model.vo.SpoolHomeSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolQueryOptionsVO;
import cn.mars.marsprinterhub.model.vo.SpoolSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolVO;
import cn.mars.marsprinterhub.service.AmsSlotsService;
import cn.mars.marsprinterhub.service.SpoolService;
import cn.mars.marsprinterhub.service.SpoolUsedRecordService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static cn.mars.marsprinterhub.constant.RedisConstants.SPOOL_SUMMARY_PREFIX;

/**
 * 针对表【spools(耗材料盘表)】的数据库操作Service实现
 */
@Service
@Slf4j
public class SpoolServiceImpl extends ServiceImpl<SpoolMapper, Spool>
        implements SpoolService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 定义排序字段映射
    private static final Map<String, SFunction<Spool, ?>> SORT_FIELD_MAP = Map.of(
            "id", Spool::getId,
            "name", Spool::getName,
            "materialType", Spool::getMaterialType,
            "appearance", Spool::getAppearance,
            "price", Spool::getPrice
    );
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private SpoolUsedRecordService spoolUsedRecordService;
    @Resource
    private AmsSlotsService amsSlotsService;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public Spool createSpool(Spool spool, int num) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(spool.getName()), ErrorCode.PARAMS_ERROR, "耗材名称不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(spool.getMaterialType()), ErrorCode.PARAMS_ERROR, "材料类型不能为空");
        ThrowUtils.throwIf(spool.getNetWeight() == null || spool.getNetWeight().doubleValue() <= 0,
                ErrorCode.PARAMS_ERROR, "净重必须大于0");
        ThrowUtils.throwIf(spool.getSpoolWeight() == null || spool.getSpoolWeight().doubleValue() <= 0,
                ErrorCode.PARAMS_ERROR, "料盘重量必须大于0");

        // 设置当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        spool.setUserId(currentUserId);

        // 颜色值转换为大写
        if (StrUtil.isNotBlank(spool.getColor())) {
            spool.setColor(spool.getColor().toUpperCase());
        }

        // 设置初始已使用重量为0
        if (spool.getUsedWeight() == null) {
            spool.setUsedWeight(java.math.BigDecimal.ZERO);
        }

        // 设置外观字段，如果为空则设为普通
        if (spool.getAppearance() == null) {
            spool.setAppearance(PrintingAppearanceEnum.NORMAL.getValue());
        }

        // 保存耗材
        for (int i = 1; i <= num; i++) {
            Spool newSpool = new Spool();
            BeanUtils.copyProperties(spool, newSpool);
            newSpool.setId(null);
            this.save(newSpool);
        }

        // 删除缓存
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + currentUserId + ":false");
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + currentUserId + ":true");
        return spool;
    }

    @Override
    public Spool updateSpool(Spool spool) {
        // 参数校验
        ThrowUtils.throwIf(spool.getId() == null, ErrorCode.PARAMS_ERROR, "耗材 ID 不能为空");

        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();

        // 检查耗材是否存在且属于当前用户
        validSpool(spool.getId(), currentUserId);

        // 确保不能修改用户 ID
        spool.setUserId(currentUserId);

        // 颜色值转换为大写
        if (StrUtil.isNotBlank(spool.getColor())) {
            spool.setColor(spool.getColor().toUpperCase());
        }

        // 更新耗材
        boolean update = this.updateById(spool);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新耗材失败");

        // 删除缓存
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + currentUserId + ":false");
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + currentUserId + ":true");

        return this.getById(spool.getId());
    }

    @Override
    public boolean deleteSpool(Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "耗材 ID 不能为空");

        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();

        // 检查耗材是否存在且属于当前用户
        Spool spool = this.getById(id);
        ThrowUtils.throwIf(spool == null, ErrorCode.NOT_FOUND_ERROR, "耗材不存在");
        ThrowUtils.throwIf(!currentUserId.equals(spool.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限操作此耗材");

        // 删除耗材（逻辑删除）
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "删除耗材失败");

        // 删除耗材使用记录
        spoolUsedRecordService.removeBySpoolId(id);

        // 解除耗材绑定
        amsSlotsService.updateToNullBySpoolId(id);

        // 删除缓存
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + currentUserId + ":false");
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + currentUserId + ":true");
        return true;
    }

    @Override
    public Spool getSpoolById(Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "耗材 ID 不能为空");

        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();

        Spool spool = this.getById(id);
        ThrowUtils.throwIf(spool == null, ErrorCode.NOT_FOUND_ERROR, "耗材不存在");
        ThrowUtils.throwIf(!currentUserId.equals(spool.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限查看此耗材");

        return spool;
    }

    @Override
    public Spool getSpoolByIdNotLogin(Long id) {
        return this.getById(id);
    }

    @Override
    public IPage<Spool> getSpoolPage(SpoolQueryDTO queryDTO) {
        Long currentUserId = userService.getCurrentUserId();
        String keyword = StrUtil.trim(queryDTO.getKeyword());
        final Long keywordId = StrUtil.isNotBlank(keyword) && StrUtil.isNumeric(keyword)
                ? Long.parseLong(keyword)
                : null;

        // 构建条件
        LambdaQueryWrapper<Spool> queryWrapper = new LambdaQueryWrapper<Spool>()
                .eq(queryDTO.getId() != null, Spool::getId, queryDTO.getId())
                .eq(Spool::getUserId, currentUserId)
                .like(StrUtil.isNotBlank(queryDTO.getName()), Spool::getName, queryDTO.getName())
                .and(StrUtil.isNotBlank(keyword), wrapper -> {
                    if (keywordId != null) {
                        wrapper.eq(Spool::getId, keywordId).or().like(Spool::getName, keyword);
                    } else {
                        wrapper.like(Spool::getName, keyword);
                    }
                })
                .in(CollUtil.isNotEmpty(queryDTO.getMaterialType()), Spool::getMaterialType, queryDTO.getMaterialType())
                .like(StrUtil.isNotBlank(queryDTO.getBrand()), Spool::getBrand, queryDTO.getBrand())
                .ge(queryDTO.getMinPrice() != null, Spool::getPrice, queryDTO.getMinPrice())
                .le(queryDTO.getMaxPrice() != null, Spool::getPrice, queryDTO.getMaxPrice())
                .in(CollUtil.isNotEmpty(queryDTO.getAppearance()), Spool::getAppearance, queryDTO.getAppearance());

        Integer status = queryDTO.getStatus();
        if (status != null) {
            switch (status) {
                case 0 -> // 未使用：已使用重量 = 0
                        queryWrapper.eq(Spool::getUsedWeight, BigDecimal.ZERO);

                case 1 -> { // 使用中：AMS 中已绑定的耗材
                    List<Long> bindSpoolIds = amsSlotsService.getBindSpoolIds(currentUserId);
                    if (CollUtil.isNotEmpty(bindSpoolIds)) {
                        queryWrapper.in(Spool::getId, bindSpoolIds);
                    } else {
                        // 没有绑定的耗材，永远不匹配
                        queryWrapper.eq(Spool::getId, 0);
                    }
                }

                case 2 -> // 消耗完：已使用重量 >= 净重
                        queryWrapper.apply("used_weight >= net_weight");

                case 3 -> // 可使用：已使用重量 < 净重
                        queryWrapper.apply("used_weight < net_weight");

                default -> {
                    // 其他情况不加条件
                }
            }
        }

        // 排序逻辑
        String sortField = queryDTO.getSortField();
        boolean isAsc = "asc".equalsIgnoreCase(queryDTO.getSortOrder());

        if (StrUtil.isNotBlank(sortField)) {
            if ("remainingWeight".equals(sortField)) {
                // 特殊处理：净重 - 已使用重量
                queryWrapper.last("ORDER BY (net_weight - used_weight) " + (isAsc ? "ASC" : "DESC"));
            } else {
                SFunction<Spool, ?> column = SORT_FIELD_MAP.get(sortField);
                if (column != null) {
                    if (isAsc) {
                        queryWrapper.orderByAsc(column);
                    } else {
                        queryWrapper.orderByDesc(column);
                    }
                }
            }
        } else {
            // 默认排序：按最后使用时间降序
            queryWrapper.orderByDesc(Spool::getLastUsedTime, Spool::getId);
        }

        return this.page(new Page<>(queryDTO.getCurrent(), queryDTO.getSize()), queryWrapper);
    }

    @Override
    public Spool useSpool(Long spoolId, BigDecimal thisUsedWeight, String title) {
        Spool spool = validSpool(spoolId, userService.getCurrentUserId());

        // 累加本次使用量
        BigDecimal newUsedWeight = (spool.getUsedWeight() == null ? BigDecimal.ZERO : spool.getUsedWeight())
                .add(thisUsedWeight);

        // 更新数据库和缓存
        Spool updatedSpool = updateSpoolWeight(spool, newUsedWeight);

        // 记录本次使用日志
        createSpoolUsedRecord(updatedSpool, thisUsedWeight, title);

        return updatedSpool;
    }

    @Override
    public Spool checkSpool(Long spoolId, BigDecimal usedWeight) {
        Spool spool = validSpool(spoolId, userService.getCurrentUserId());
        return updateSpoolWeight(spool, usedWeight);
    }

    private Spool updateSpoolWeight(Spool spool, BigDecimal usedWeightValue) {
        if (spool.getNetWeight() != null && usedWeightValue.compareTo(spool.getNetWeight()) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "使用重量超过耗材净重");
        }

        // 更新已使用重量
        spool.setUsedWeight(usedWeightValue);

        Date now = new Date();
        if (spool.getFirstUsedTime() == null) {
            spool.setFirstUsedTime(now);
        }
        spool.setLastUsedTime(now);

        // 保存到数据库
        boolean success = this.updateById(spool);
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "更新耗材失败");

        // 删除缓存
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + spool.getUserId() + ":false");
        redisUtils.delete(SPOOL_SUMMARY_PREFIX + spool.getUserId() + ":true");

        return spool;
    }

    private Spool validSpool(Long spoolId, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(spoolId == null, ErrorCode.PARAMS_ERROR, "耗材 ID 不能为空");

        // 获取耗材
        Spool spool = this.getById(spoolId);
        ThrowUtils.throwIf(spool == null, ErrorCode.NOT_FOUND_ERROR, "耗材不存在");
        ThrowUtils.throwIf(!userId.equals(spool.getUserId()),
                ErrorCode.NO_AUTH_ERROR, "无权操作此耗材");

        return spool;
    }

    @Override
    public SpoolVO convertToVO(Spool spool) {
        SpoolVO spoolVO = BeanUtil.copyProperties(spool, SpoolVO.class);

        // 计算剩余重量
        if (spool.getNetWeight() != null && spool.getUsedWeight() != null) {
            BigDecimal remainingWeight = spool.getNetWeight().subtract(spool.getUsedWeight());
            spoolVO.setRemainingWeight(remainingWeight);
        } else if (spool.getNetWeight() != null) {
            spoolVO.setRemainingWeight(spool.getNetWeight());
        } else {
            spoolVO.setRemainingWeight(BigDecimal.ZERO);
        }

        // 计算剩余百分比
        if (spool.getNetWeight() != null && spool.getUsedWeight() != null &&
                spool.getNetWeight().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remainingWeight = spool.getNetWeight().subtract(spool.getUsedWeight());
            BigDecimal remainingPercentage = remainingWeight
                    .divide(spool.getNetWeight(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
            spoolVO.setRemainingPercentage(remainingPercentage);
        } else if (spool.getNetWeight() != null) {
            spoolVO.setRemainingPercentage(new BigDecimal("100"));
        } else {
            spoolVO.setRemainingPercentage(BigDecimal.ZERO);
        }

        return spoolVO;
    }

    @Override
    public SpoolHomeSummaryVO convertToHomeSummaryVO(SpoolVO spoolVO) {
        return BeanUtil.copyProperties(spoolVO, SpoolHomeSummaryVO.class);
    }

    @Override
    public SpoolSummaryVO getSpoolSummary(boolean excludeFinished, String brand, String materialType) {
        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        String normalizedBrand = StrUtil.trimToNull(brand);
        String normalizedMaterialType = StrUtil.trimToNull(materialType);

        String key = SPOOL_SUMMARY_PREFIX + currentUserId + ":" + excludeFinished
                + ":" + StrUtil.nullToDefault(normalizedBrand, "")
                + ":" + StrUtil.nullToDefault(normalizedMaterialType, "");
        // 先从 Redis 取
        String json = (String) redisUtils.get(key);
        if (json != null) {
            return JSONUtil.toBean(json, SpoolSummaryVO.class);
        }

        // 查询当前用户的所有耗材
        LambdaQueryWrapper<Spool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Spool::getUserId, currentUserId)
                .eq(Spool::getIsDelete, 0)
                .eq(StrUtil.isNotBlank(normalizedBrand), Spool::getBrand, normalizedBrand)
                .eq(StrUtil.isNotBlank(normalizedMaterialType), Spool::getMaterialType, normalizedMaterialType);
        List<Spool> spools = this.list(queryWrapper);

        // 过滤掉已用完的耗材
        if (excludeFinished) {
            spools = spools.stream()
                    .filter(spool -> {
                        if (spool.getNetWeight() == null || spool.getUsedWeight() == null) return true;
                        return spool.getUsedWeight().compareTo(spool.getNetWeight()) < 0;
                    })
                    .toList();
        }

        SpoolSummaryVO summary = new SpoolSummaryVO();
        fillRecordCostSummary(currentUserId, summary);
        summary.setRecent30DaysInboundStats(buildRecent30DaysInboundStats(spools));

        if (spools.isEmpty()) {
            // 如果没有耗材，返回空汇总信息
            summary.setTotalCount(0L);
            summary.setTotalPrice(BigDecimal.ZERO);
            summary.setTotalNetWeight(BigDecimal.ZERO);
            summary.setTotalUsedWeight(BigDecimal.ZERO);
            summary.setTotalRemainingWeight(BigDecimal.ZERO);
            return summary;
        }

        // 基础统计
        summary.setTotalCount((long) spools.size());

        BigDecimal totalPrice = spools.stream()
                .map(Spool::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalPrice(totalPrice);

        BigDecimal totalNetWeight = spools.stream()
                .map(Spool::getNetWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalNetWeight(totalNetWeight);

        BigDecimal totalUsedWeight = spools.stream()
                .map(Spool::getUsedWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalUsedWeight(totalUsedWeight);

        BigDecimal totalRemainingWeight = totalNetWeight.subtract(totalUsedWeight);
        summary.setTotalRemainingWeight(totalRemainingWeight);

        // 材料类型分布（按数量降序）
        Map<String, Long> materialTypeDistribution = spools.stream()
                .filter(spool -> StrUtil.isNotBlank(spool.getMaterialType()))
                .collect(Collectors.groupingBy(Spool::getMaterialType, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        summary.setMaterialTypeDistribution(materialTypeDistribution);

        // 品牌分布（按数量降序）
        Map<String, Long> brandDistribution = spools.stream()
                .filter(spool -> StrUtil.isNotBlank(spool.getBrand()))
                .collect(Collectors.groupingBy(Spool::getBrand, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        summary.setBrandDistribution(brandDistribution);

        // 颜色分布（按数量降序）
        Map<String, Long> colorDistribution = spools.stream()
                .filter(spool -> StrUtil.isNotBlank(spool.getColor()))
                .collect(Collectors.groupingBy(Spool::getColor, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        summary.setColorDistribution(colorDistribution);

        // 外观分布（按数量降序）
        Map<String, Long> appearanceDistribution = spools.stream()
                .peek(spool -> spool.setAppearance(spool.getAppearance() == null ? 0 : spool.getAppearance()))
                .collect(Collectors.groupingBy(
                        spool -> String.valueOf(spool.getAppearance()),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        summary.setAppearanceDistribution(appearanceDistribution);

        // 使用状态分布
        SpoolSummaryVO.UsageStatusDistribution usageStatus = new SpoolSummaryVO.UsageStatusDistribution();
        long unused = spools.stream()
                .filter(spool -> spool.getUsedWeight() == null || spool.getUsedWeight().compareTo(BigDecimal.ZERO) == 0)
                .count();
        long finished = spools.stream()
                .filter(spool -> {
                    if (spool.getNetWeight() == null || spool.getUsedWeight() == null) return false;
                    return spool.getUsedWeight().compareTo(spool.getNetWeight()) >= 0;
                })
                .count();
        long inUse = spools.size() - unused - finished;

        usageStatus.setUnused(unused);
        usageStatus.setInUse(inUse);
        usageStatus.setFinished(finished);
        summary.setUsageStatusDistribution(usageStatus);

        // 价格区间分布
        List<SpoolSummaryVO.PriceRangeDistribution> priceRanges = new ArrayList<>();
        if (spools.stream().anyMatch(spool -> spool.getPrice() != null)) {
            // 定义价格区间
            priceRanges.add(createPriceRange("0-10元", BigDecimal.ZERO, new BigDecimal("10"), spools));
            priceRanges.add(createPriceRange("10-20元", new BigDecimal("10"), new BigDecimal("20"), spools));
            priceRanges.add(createPriceRange("20-30元", new BigDecimal("20"), new BigDecimal("30"), spools));
            priceRanges.add(createPriceRange("30-40元", new BigDecimal("30"), new BigDecimal("40"), spools));
            priceRanges.add(createPriceRange("40-50元", new BigDecimal("40"), new BigDecimal("50"), spools));
            priceRanges.add(createPriceRange(">50元", new BigDecimal("50"), null, spools));
        }
        summary.setPriceRangeDistribution(priceRanges);

        // 存入 redis,缓存1分钟
        redisUtils.set(key, JSONUtil.toJsonStr(summary), 60);
        return summary;
    }

    private List<SpoolSummaryVO.DailyInboundStats> buildRecent30DaysInboundStats(List<Spool> spools) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);

        Map<LocalDate, List<Spool>> inboundByDate = spools.stream()
                .filter(spool -> spool.getCreateTime() != null)
                .collect(Collectors.groupingBy(spool ->
                        spool.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                ));

        List<SpoolSummaryVO.DailyInboundStats> stats = new ArrayList<>(30);
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            List<Spool> dailySpools = inboundByDate.getOrDefault(date, Collections.emptyList());
            BigDecimal amount = dailySpools.stream()
                    .map(Spool::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            SpoolSummaryVO.DailyInboundStats dailyInboundStats = new SpoolSummaryVO.DailyInboundStats();
            dailyInboundStats.setDate(date.format(DATE_FORMATTER));
            dailyInboundStats.setCount((long) dailySpools.size());
            dailyInboundStats.setAmount(amount);
            stats.add(dailyInboundStats);
        }
        return stats;
    }

    private void fillRecordCostSummary(Long userId, SpoolSummaryVO summary) {
        List<SpoolUsedRecord> records = spoolUsedRecordService.list(
                new LambdaQueryWrapper<SpoolUsedRecord>()
                        .eq(SpoolUsedRecord::getUserId, userId)
        );
        BigDecimal totalCost = records.stream()
                .map(SpoolUsedRecord::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalElectricityCost = records.stream()
                .map(SpoolUsedRecord::getElectricityCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDepreciationCost = records.stream()
                .map(SpoolUsedRecord::getDepreciationCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalCost(totalCost);
        summary.setTotalElectricityCost(totalElectricityCost);
        summary.setTotalDepreciationCost(totalDepreciationCost);
    }

    @Override
    public List<Spool> getAllSpools() {
        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        LambdaQueryWrapper<Spool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Spool::getUserId, currentUserId);
        return list(queryWrapper);
    }

    @Override
    public SpoolQueryOptionsVO getOptions() {
        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        SpoolQueryOptionsVO optionsVO = new SpoolQueryOptionsVO();
        optionsVO.setMaterialTypes(
                getDistinctValues("material_type", String.class, currentUserId)
        );
        optionsVO.setBrand(
                getDistinctValues("brand", String.class, currentUserId)
        );
        optionsVO.setAppearance(
                getDistinctValues("appearance", Integer.class, currentUserId)
        );
        return optionsVO;
    }

    @Override
    public List<Spool> getLatest5Spools() {
        Page<Spool> page = new Page<>(1, 5);
        return this.page(
                page,
                new LambdaQueryWrapper<Spool>()
                        .eq(Spool::getUserId, userService.getCurrentUserId())
                        .orderByDesc(Spool::getCreateTime)
        ).getRecords();
    }

    @Override
    public List<Spool> getSpoolsByRemainingPercentageLessThan10() {
        return this.list(
                new LambdaQueryWrapper<Spool>()
                        .eq(Spool::getUserId, userService.getCurrentUserId())
                        .apply("(net_weight - used_weight) > 0 AND (net_weight - used_weight) < (net_weight * 0.1)")
                        .orderByDesc(Spool::getUsedWeight)
        );
    }

    @Override
    public String getSpoolNameById(Long id) {
        return this.getById(id).getName();
    }

    private <T> List<T> getDistinctValues(String fieldName, Class<T> type, Long userId) {
        QueryWrapper<Spool> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(fieldName, "COUNT(*) AS item_count");
        queryWrapper.eq("user_id", userId);
        queryWrapper.isNotNull(fieldName);
        queryWrapper.groupBy(fieldName);
        queryWrapper.orderByDesc("item_count");
        return this.listMaps(queryWrapper).stream()
                .map(item -> item.get(fieldName))
                .filter(Objects::nonNull)
                .map(type::cast)
                .toList();
    }


    private SpoolSummaryVO.PriceRangeDistribution createPriceRange(String range, BigDecimal min, BigDecimal max, List<Spool> spools) {
        SpoolSummaryVO.PriceRangeDistribution priceRange = new SpoolSummaryVO.PriceRangeDistribution();
        priceRange.setPriceRange(range);
        priceRange.setMinPrice(min);
        priceRange.setMaxPrice(max);

        long count = spools.stream()
                .filter(spool -> spool.getPrice() != null)
                .filter(spool -> {
                    BigDecimal price = spool.getPrice();
                    boolean minCheck = min == null || price.compareTo(min) >= 0;
                    boolean maxCheck = max == null || price.compareTo(max) < 0;
                    return minCheck && maxCheck;
                })
                .count();

        priceRange.setCount(count);
        return priceRange;
    }

    private void createSpoolUsedRecord(Spool spool, BigDecimal usedWeight,  String title) {
        Date now = new Date();
        spoolUsedRecordService.createSimpleRecord(spool, usedWeight, title, now, now);
    }
}
