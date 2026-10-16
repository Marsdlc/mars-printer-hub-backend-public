package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.order.OrderAssetsUpdateDTO;
import cn.mars.marsprinterhub.model.dto.order.OrderQuantityAdjustDTO;
import cn.mars.marsprinterhub.model.dto.order.OrderQueryDTO;
import cn.mars.marsprinterhub.model.dto.order.OrderStatusUpdateDTO;
import cn.mars.marsprinterhub.model.dto.order.OrderSummaryDTO;
import cn.mars.marsprinterhub.model.dto.order.OrderTrackingNumberUpdateDTO;
import cn.mars.marsprinterhub.model.entity.OrderInfo;
import cn.mars.marsprinterhub.model.vo.OrderVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;

import java.util.List;

/**
 * @author mars
 * @description 针对表【order_info(订单信息表)】的数据库操作Service
 * @createDate 2025-11-11 10:22:00
 */
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 创建订单
     *
     * @param orderInfo 订单信息
     * @return 创建的订单信息
     */
    OrderInfo createOrder(OrderInfo orderInfo);

    /**
     * 更新订单状态
     *
     * @param id              订单 ID
     * @param statusUpdateDTO 订单状态更新信息
     * @return 更新的订单信息
     */
    OrderInfo updateStatus(Long id, OrderStatusUpdateDTO statusUpdateDTO);

    /**
     * 绑定记录
     *
     * @param id       订单 ID
     * @param recordId 记录 ids
     */
    void bindRecords(Long id, List<Long> recordId);

    /**
     * 解绑记录
     *
     * @param orderId  订单 ID
     * @param recordId 记录 ID
     */
    void unbindRecord(Long orderId, Long recordId);

    /**
     * 更新订单信息
     *
     * @param orderInfo 订单信息
     * @return 更新的订单信息
     */
    OrderInfo updateOrder(OrderInfo orderInfo);

    /**
     * 更新订单资产（相关图片、模型）
     *
     * @param updateDTO 资产更新DTO
     * @return 更新后的订单信息
     */
    OrderInfo updateOrderAssets(OrderAssetsUpdateDTO updateDTO);

    /**
     * 更新物流单号
     *
     * @param updateDTO 物流单号更新DTO
     * @return 更新后的订单信息
     */
    OrderInfo updateTrackingNumber(OrderTrackingNumberUpdateDTO updateDTO);

    /**
     * 调整订单数量
     *
     * @param adjustDTO 数量调整参数
     * @return 更新后的订单信息
     */
    OrderInfo adjustQuantity(OrderQuantityAdjustDTO adjustDTO);

    /**
     * 删除订单
     *
     * @param id 订单 ID
     * @return 删除结果
     */
    Boolean deleteOrder(Long id);

    /**
     * 根据ID获取订单信息
     *
     * @param id 订单 ID
     * @return 订单信息
     */
    OrderInfo getOrderById(Long id);

    /**
     * 获取订单摘要信息
     *
     * @return 订单摘要信息
     */
    OrderSummaryDTO getOrderSummary();

    /**
     * 将订单信息转换为订单视图对象
     *
     * @param orderInfo 订单信息
     * @return 订单视图对象
     */
    OrderVO convertToVO(OrderInfo orderInfo);

    /**
     * 获取订单分页列表
     *
     * @param queryDTO 查询参数
     * @return 订单分页列表
     */
    IPage<OrderInfo> getOrderPage(@Valid OrderQueryDTO queryDTO);

    /**
     * 获取所有订单
     *
     * @return 所有订单列表
     */
    List<OrderInfo> getAllOrders();

    /**
     * 计算订单成本
     *
     * @param id 订单 ID
     */
    void calculateOrder(Long id);

    /**
     * 获取订单打印时长
     *
     * @param id 订单 ID
     * @return 打印时长
     */
    Double getOrderPrintDuration(Long id);

}
