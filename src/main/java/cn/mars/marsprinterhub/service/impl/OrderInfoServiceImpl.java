package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.mapper.OrderInfoMapper;
import cn.mars.marsprinterhub.model.dto.order.*;
import cn.mars.marsprinterhub.model.dto.user.UserConfigDTO;
import cn.mars.marsprinterhub.model.entity.OrderInfo;
import cn.mars.marsprinterhub.model.entity.SpoolUsedRecord;
import cn.mars.marsprinterhub.model.enums.OrderQuantityOperationEnum;
import cn.mars.marsprinterhub.model.enums.OrderStatusEnum;
import cn.mars.marsprinterhub.model.enums.TaskStatusEnum;
import cn.mars.marsprinterhub.model.vo.OrderVO;
import cn.mars.marsprinterhub.model.vo.SpoolUsedRecordVO;
import cn.mars.marsprinterhub.model.vo.SpoolVO;
import cn.mars.marsprinterhub.service.OrderInfoService;
import cn.mars.marsprinterhub.service.SpoolUsedRecordService;
import cn.mars.marsprinterhub.service.UserConfigService;
import cn.mars.marsprinterhub.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author mars
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
        implements OrderInfoService {

    // 定义排序字段映射
    private static final Map<String, SFunction<OrderInfo, ?>> SORT_FIELD_MAP = Map.of(
            "id", OrderInfo::getId,
            "orderName", OrderInfo::getOrderName,
            "customerName", OrderInfo::getCustomerName,
            "customerSource", OrderInfo::getCustomerSource,
            "status", OrderInfo::getStatus
    );
    @Resource
    private UserService userService;
    @Resource
    private SpoolUsedRecordService spoolUsedRecordService;
    @Resource
    private UserConfigService userConfigService;

    @Override
    public OrderInfo createOrder(OrderInfo orderInfo) {
        orderInfo.setUserId(userService.getCurrentUserId());
        save(orderInfo);
        return getOrderById(orderInfo.getId());
    }

    @Override
    public OrderInfo updateStatus(Long id, OrderStatusUpdateDTO statusUpdateDTO) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");
        Integer status = statusUpdateDTO.getStatus();
        ThrowUtils.throwIf(status == null, ErrorCode.PARAMS_ERROR, "订单状态不能为空");

        OrderStatusEnum orderStatusEnum = OrderStatusEnum.getEnumByValue(status);
        ThrowUtils.throwIf(orderStatusEnum == null, ErrorCode.PARAMS_ERROR, "订单状态错误");

        Long currentUserId = userService.getCurrentUserId();
        OrderInfo orderInfo = validOrder(id, currentUserId);

        // 不允许重复或非法修改
        if (Objects.equals(orderInfo.getStatus(), orderStatusEnum.getValue())) {
            return orderInfo;
        }

        OrderInfo updateOrder = new OrderInfo();
        updateOrder.setId(id);
        switch (orderStatusEnum) {
            case PROCESSING:
                updateOrder.setStatus(OrderStatusEnum.PROCESSING.getValue());
                if (updateOrder.getStartTime() == null) {
                    updateOrder.setStartTime(new Date());
                }
                break;
            case COMPLETED:
                updateOrder.setStatus(OrderStatusEnum.COMPLETED.getValue());
                updateOrder.setEndTime(new Date());
                calculateOrder(id);
                break;
            case CANCELLED:
                updateOrder.setStatus(OrderStatusEnum.CANCELLED.getValue());
                updateOrder.setEndTime(new Date());
                break;
            case IN_STOCK:
                updateOrder.setStatus(OrderStatusEnum.IN_STOCK.getValue());
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单状态错误");
        }

        updateById(updateOrder);
        return this.getById(id);
    }

    @Override
    public void bindRecords(Long orderId, List<Long> recordIds) {
        ThrowUtils.throwIf(orderId == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");
        ThrowUtils.throwIf(CollUtil.isEmpty(recordIds), ErrorCode.PARAMS_ERROR, "记录 ID 不能为空");
        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        validOrder(orderId, currentUserId);

        // 批量绑定
        for (Long recordId : recordIds) {
            // 获取任务绑定的旧订单
            SpoolUsedRecord dbRecord = spoolUsedRecordService.getById(recordId);
            spoolUsedRecordService.bindOrderRecord(orderId, recordId);
            if (dbRecord != null && dbRecord.getOrderId() != null && Objects.equals(dbRecord.getUserId(), currentUserId)) {
                // 重新计算旧订单的耗材成本
                calculateOrder(dbRecord.getOrderId());
            }
        }

        calculateOrder(orderId);
    }

    @Override
    public void unbindRecord(Long orderId, Long recordId) {
        ThrowUtils.throwIf(orderId == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");
        ThrowUtils.throwIf(recordId == null, ErrorCode.PARAMS_ERROR, "记录 ID 不能为空");
        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        validOrder(orderId, currentUserId);

        spoolUsedRecordService.unbindOrderRecord(orderId, recordId);
        calculateOrder(orderId);
    }

    @Override
    public OrderInfo updateOrder(OrderInfo orderInfo) {
        // 参数校验
        ThrowUtils.throwIf(orderInfo.getId() == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");

        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        validOrder(orderInfo.getId(), currentUserId);

        // 确保不能修改用户 ID
        orderInfo.setUserId(currentUserId);
        boolean update = this.updateById(orderInfo);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新订单失败");

        return this.getById(orderInfo.getId());
    }

    @Override
    public OrderInfo updateOrderAssets(OrderAssetsUpdateDTO updateDTO) {
        // 参数校验
        ThrowUtils.throwIf(updateDTO.getId() == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");

        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        validOrder(updateDTO.getId(), currentUserId);

        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setId(updateDTO.getId());
        updateOrderInfo.setImages(updateDTO.getImages());
        updateOrderInfo.setModels(updateDTO.getModels());

        boolean update = this.updateById(updateOrderInfo);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新订单资源失败");

        return this.getById(updateDTO.getId());
    }

    @Override
    public OrderInfo updateTrackingNumber(OrderTrackingNumberUpdateDTO updateDTO) {
        ThrowUtils.throwIf(updateDTO.getId() == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");

        Long currentUserId = userService.getCurrentUserId();
        validOrder(updateDTO.getId(), currentUserId);

        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setId(updateDTO.getId());
        updateOrderInfo.setTrackingNumber(updateDTO.getTrackingNumber());

        boolean update = this.updateById(updateOrderInfo);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新物流单号失败");

        return this.getById(updateDTO.getId());
    }

    @Override
    public OrderInfo adjustQuantity(OrderQuantityAdjustDTO adjustDTO) {
        ThrowUtils.throwIf(adjustDTO.getOrderId() == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");
        ThrowUtils.throwIf(adjustDTO.getQuantity() == null || adjustDTO.getQuantity() <= 0, ErrorCode.PARAMS_ERROR, "数量必须大于 0");
        ThrowUtils.throwIf(adjustDTO.getOperation() == null, ErrorCode.PARAMS_ERROR, "操作类型不能为空");
        if (adjustDTO.getUnitPrice() != null) {
            ThrowUtils.throwIf(adjustDTO.getUnitPrice().compareTo(BigDecimal.ZERO) < 0, ErrorCode.PARAMS_ERROR, "单价不能小于 0");
        }

        Long currentUserId = userService.getCurrentUserId();
        OrderInfo orderInfo = validOrder(adjustDTO.getOrderId(), currentUserId);

        int currentQuantity = Optional.ofNullable(orderInfo.getQuantity()).orElse(0);
        OrderQuantityOperationEnum operation = adjustDTO.getOperation();
        int delta = parseQuantityDelta(operation, adjustDTO.getQuantity());
        int newQuantity = currentQuantity + delta;
        ThrowUtils.throwIf(newQuantity < 0, ErrorCode.PARAMS_ERROR, "出库后数量不能小于 0");

        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setId(orderInfo.getId());
        updateOrderInfo.setQuantity(newQuantity);
        if (OrderQuantityOperationEnum.OUTBOUND == operation) {
            ThrowUtils.throwIf(adjustDTO.getUnitPrice() == null, ErrorCode.PARAMS_ERROR, "出库时单价不能为空");
            BigDecimal outboundIncome = adjustDTO.getUnitPrice().multiply(BigDecimal.valueOf(adjustDTO.getQuantity()));
            updateOrderInfo.setTotalIncome(defaultZero(orderInfo.getTotalIncome()).add(outboundIncome));
        }

        boolean update = this.updateById(updateOrderInfo);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新订单数量失败");

        return this.getById(orderInfo.getId());
    }

    @Override
    public Boolean deleteOrder(Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");

        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();

        // 检查订单是否存在且属于当前用户
        OrderInfo orderInfo = this.getById(id);
        ThrowUtils.throwIf(orderInfo == null, ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        ThrowUtils.throwIf(!currentUserId.equals(orderInfo.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限操作此订单");

        // 删除订单（逻辑删除）
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "删除订单失败");

        // 把对应的打印记录解除关联
        spoolUsedRecordService.updateByOrderId(id);

        return true;
    }

    @Override
    public OrderInfo getOrderById(Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");

        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();

        OrderInfo orderInfo = this.getById(id);
        ThrowUtils.throwIf(orderInfo == null, ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        ThrowUtils.throwIf(!currentUserId.equals(orderInfo.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限查看此订单");

        return orderInfo;
    }

    @Override
    public OrderSummaryDTO getOrderSummary() {
        Long currentUserId = userService.getCurrentUserId();

        List<OrderInfo> completedOrderList = list(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getUserId, currentUserId)
                        .eq(OrderInfo::getStatus, OrderStatusEnum.COMPLETED.getValue())
        );

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (OrderInfo order : completedOrderList) {
            // 收入
            totalIncome = totalIncome.add(
                    defaultZero(order.getTotalIncome())
            );

            // 单个订单成本
            BigDecimal orderCost =
                    defaultZero(order.getMaterialCost())
                            .add(defaultZero(order.getElectricityCost()))
                            .add(defaultZero(order.getLogisticsCost()))
                            .add(defaultZero(order.getDesignCost()))
                            .add(defaultZero(order.getLaborCost()))
                            .add(defaultZero(order.getOtherCost()));

            totalCost = totalCost.add(orderCost);
        }

        BigDecimal totalProfit = totalIncome.subtract(totalCost);

        return OrderSummaryDTO.builder()
                .totalIncome(totalIncome)
                .totalCost(totalCost)
                .totalProfit(totalProfit)
                .build();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public OrderVO convertToVO(OrderInfo createdOrder) {
        OrderVO orderVO = BeanUtil.copyProperties(createdOrder, OrderVO.class);
        orderVO.setProfit(
                orderVO.getTotalIncome()
                        .subtract(
                                orderVO.getMaterialCost()
                                        .add(orderVO.getElectricityCost())
                                        .add(orderVO.getLogisticsCost())
                                        .add(orderVO.getDesignCost())
                                        .add(orderVO.getLaborCost())
                                        .add(orderVO.getOtherCost())
                        )
        );
        return orderVO;
    }

    @Override
    public IPage<OrderInfo> getOrderPage(OrderQueryDTO queryDTO) {
        Long currentUserId = userService.getCurrentUserId();

        String orderName = queryDTO.getOrderName();
        String customerName = queryDTO.getCustomerName();
        String customerSource = queryDTO.getCustomerSource();
        Integer status = queryDTO.getStatus();

        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<OrderInfo>()
                .like(StrUtil.isNotBlank(orderName), OrderInfo::getOrderName, orderName)
                .like(StrUtil.isNotBlank(customerName), OrderInfo::getCustomerName, customerName)
                .like(StrUtil.isNotBlank(customerSource), OrderInfo::getCustomerSource, customerSource)
                .eq(status != null, OrderInfo::getStatus, status)
                .eq(OrderInfo::getUserId, currentUserId);

        String sortField = queryDTO.getSortField();
        String sortOrder = queryDTO.getSortOrder();
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);

        if (StrUtil.isNotBlank(sortField)) {
            SFunction<OrderInfo, ?> column = SORT_FIELD_MAP.get(sortField);
            if (column != null) {
                if (isAsc) {
                    queryWrapper.orderByAsc(column);
                } else {
                    queryWrapper.orderByDesc(column);
                }
            }
        } else {
            // 默认排序：创建时间倒序
            queryWrapper.orderByDesc(OrderInfo::getCreateTime);
        }

        return this.page(new Page<>(queryDTO.getCurrent(), queryDTO.getSize()), queryWrapper);
    }

    @Override
    public List<OrderInfo> getAllOrders() {
        // 获取当前用户 ID
        Long currentUserId = userService.getCurrentUserId();
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getUserId, currentUserId);
        return list(queryWrapper);
    }

    @Override
    public void calculateOrder(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "订单 ID 不能为空");

        OrderInfo dbOrder = this.getById(id);
        ThrowUtils.throwIf(dbOrder == null, ErrorCode.NOT_FOUND_ERROR, "订单不存在");

        List<SpoolUsedRecord> spoolUsedRecords = spoolUsedRecordService.list(
                new LambdaQueryWrapper<SpoolUsedRecord>()
                        .eq(SpoolUsedRecord::getOrderId, id)
                        .orderByDesc(SpoolUsedRecord::getCreateTime)
        );

        // 转换为 VO
        List<SpoolUsedRecordVO> recordVOList = spoolUsedRecords.stream()
                .map(record -> spoolUsedRecordService.convertToVO(record, true))
                .filter(Objects::nonNull)
                .toList();

        // 计算总耗材成本
        BigDecimal materialCost = recordVOList.stream()
                .map(recordVO -> {
                    SpoolVO spoolVO = recordVO.getSpoolVO();
                    if (spoolVO == null) return BigDecimal.ZERO;

                    BigDecimal price = Optional.ofNullable(spoolVO.getPrice()).orElse(BigDecimal.ZERO);
                    BigDecimal netWeight = Optional.ofNullable(spoolVO.getNetWeight()).orElse(BigDecimal.ONE);
                    BigDecimal actualWeight = Optional.ofNullable(recordVO.getActualWeight()).orElse(BigDecimal.ZERO);

                    if (netWeight.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

                    // 单次成本 = (价格 * 实际使用重量) / 净重
                    return price.multiply(actualWeight)
                            .divide(netWeight, 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算总打印时长（小时）
        List<SpoolUsedRecord> validDurationRecords = spoolUsedRecordService.distinctByTime(
                spoolUsedRecords.stream()
                        .filter(r -> r.getStartTime() != null && r.getEndTime() != null && !r.getEndTime().before(r.getStartTime()))
                        .toList()
        );
        long totalMillis = validDurationRecords.stream()
                .mapToLong(r -> r.getEndTime().getTime() - r.getStartTime().getTime())
                .sum();
        BigDecimal printHours = BigDecimal.valueOf(totalMillis)
                .divide(BigDecimal.valueOf(1000 * 60 * 60), 4, RoundingMode.HALF_UP);

        // 电费 = (功率W / 1000) * 打印时长h * 电费(元/度)
        UserConfigDTO userConfig = userConfigService.getUserConfigByUserId(dbOrder.getUserId());
        BigDecimal electricityCost = printHours
                .multiply(defaultZero(userConfig.getPowerWatt()))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(defaultZero(userConfig.getElectricityPricePerKwh()))
                .setScale(2, RoundingMode.HALF_UP);

        // 更新耗材成本
        OrderInfo update = new OrderInfo();
        update.setId(id);
        update.setMaterialCost(materialCost);
        update.setElectricityCost(electricityCost);
        // 订单开始时间
        if (dbOrder.getStartTime() == null && CollUtil.isNotEmpty(spoolUsedRecords)) {
            // 找出最早的打印记录
            spoolUsedRecords.stream()
                    .filter(record -> record.getStartTime() != null)
                    .min(Comparator.comparing(SpoolUsedRecord::getStartTime))
                    .ifPresent(record -> update.setStartTime(record.getStartTime()));
        }
        updateById(update);
    }

    @Override
    public Double getOrderPrintDuration(Long id) {
        List<SpoolUsedRecord> spoolUsedRecords = spoolUsedRecordService.listSpoolUsedRecordsByOrderId(id);
        List<SpoolUsedRecord> distinctRecords = spoolUsedRecordService.distinctByTime(spoolUsedRecords);

        // 打印时长（小时）
        long totalMillis = distinctRecords.stream()
                .filter(record -> !Objects.equals(TaskStatusEnum.PRINTING.getValue(), record.getStatus()))
                .mapToLong(r -> r.getEndTime().getTime() - r.getStartTime().getTime())
                .sum();

        double hours = totalMillis / 1000.0 / 60 / 60;

        return Math.round(hours * 10) / 10.0;
    }

    private OrderInfo validOrder(Long orderId, Long userId) {
        // 获取订单
        OrderInfo orderInfo = this.getById(orderId);
        ThrowUtils.throwIf(orderInfo == null, ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        ThrowUtils.throwIf(!userId.equals(orderInfo.getUserId()),
                ErrorCode.NO_AUTH_ERROR, "无权操作此订单");

        return orderInfo;
    }

    private int parseQuantityDelta(OrderQuantityOperationEnum operation, Integer quantity) {
        if (operation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "操作类型不能为空");
        }
        return operation.getDeltaFactor() * quantity;
    }
}
