package cn.mars.marsprinterhub.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.model.dto.order.*;
import cn.mars.marsprinterhub.model.dto.system.AliyunMarketExpressQueryResponseDTO;
import cn.mars.marsprinterhub.model.entity.OrderInfo;
import cn.mars.marsprinterhub.model.enums.OrderStatusEnum;
import cn.mars.marsprinterhub.model.vo.OrderVO;
import cn.mars.marsprinterhub.service.AliyunMarketExpressService;
import cn.mars.marsprinterhub.service.OrderDraftService;
import cn.mars.marsprinterhub.service.OrderInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 订单管理控制器
 * 提供订单的增删改查、导入导出、使用记录等功能
 *
 * @author mars
 */
@RestController
@RequestMapping("/order")
@Tag(name = "OrderInfo", description = "订单相关接口")
@Slf4j
public class OrderController {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private OrderDraftService orderDraftService;

    @Resource
    private AliyunMarketExpressService aliyunMarketExpressService;

    @Operation(summary = "创建订单")
    @PostMapping("/add")
    public BaseResponse<OrderVO> addOrder(@Valid @RequestBody OrderCreateDTO createDTO) {
        OrderInfo order = BeanUtil.copyProperties(createDTO, OrderInfo.class);
        OrderInfo orderInfo = orderInfoService.createOrder(order);
        return success(orderInfoService.convertToVO(orderInfo));
    }

    @Operation(summary = "更新订单状态")
    @PostMapping("/{id}/status")
    public BaseResponse<OrderVO> updateStatus(@PathVariable Long id,
                                              @RequestBody OrderStatusUpdateDTO statusUpdateDTO) {
        OrderInfo orderInfo = orderInfoService.updateStatus(id, statusUpdateDTO);
        return success(orderInfoService.convertToVO(orderInfo));
    }

    @Operation(summary = "订单绑定打印记录")
    @PostMapping("/bind")
    public BaseResponse<Boolean> bindRecords(@RequestBody OrderBindRecordsDTO bindRecordsDTO) {
        orderInfoService.bindRecords(bindRecordsDTO.getOrderId(), bindRecordsDTO.getRecordIds());
        return success(true);
    }

    @Operation(summary = "订单解绑打印记录")
    @PostMapping("/unbind")
    public BaseResponse<Boolean> unbindRecord(@RequestBody OrderUnbindRecordDTO unbindRecordDTO) {
        orderInfoService.unbindRecord(unbindRecordDTO.getOrderId(), unbindRecordDTO.getRecordId());
        return success(true);
    }

    @Operation(summary = "更新订单")
    @PutMapping("/update")
    public BaseResponse<OrderVO> updateOrder(@Valid @RequestBody OrderUpdateDTO updateDTO) {
        OrderInfo order = BeanUtil.copyProperties(updateDTO, OrderInfo.class);
        OrderInfo orderInfo = orderInfoService.updateOrder(order);
        return success(orderInfoService.convertToVO(orderInfo));
    }

    @Operation(summary = "更新订单图片和模型资源")
    @PutMapping("/update/assets")
    public BaseResponse<OrderVO> updateOrderAssets(@Valid @RequestBody OrderAssetsUpdateDTO updateDTO) {
        OrderInfo orderInfo = orderInfoService.updateOrderAssets(updateDTO);
        return success(orderInfoService.convertToVO(orderInfo));
    }

    @Operation(summary = "更新订单物流单号")
    @PutMapping("/update/tracking-number")
    public BaseResponse<OrderVO> updateTrackingNumber(@Valid @RequestBody OrderTrackingNumberUpdateDTO updateDTO) {
        OrderInfo orderInfo = orderInfoService.updateTrackingNumber(updateDTO);
        return success(orderInfoService.convertToVO(orderInfo));
    }

    @Operation(summary = "调整订单数量")
    @PutMapping("/update/quantity")
    public BaseResponse<OrderVO> adjustQuantity(@Valid @RequestBody OrderQuantityAdjustDTO adjustDTO) {
        OrderInfo orderInfo = orderInfoService.adjustQuantity(adjustDTO);
        return success(orderInfoService.convertToVO(orderInfo));
    }

    @Operation(summary = "更新订单其他成本")
    @PutMapping("/update/other-cost")
    public BaseResponse<OrderVO> updateOrderOtherCost(@Valid @RequestBody OrderOtherCostUpdateDTO updateDTO) {
        OrderInfo order = BeanUtil.copyProperties(updateDTO, OrderInfo.class);
        OrderInfo orderInfo = orderInfoService.updateOrder(order);
        return success(orderInfoService.convertToVO(orderInfo));
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteOrder(
            @Parameter(description = "订单 ID", required = true)
            @PathVariable Long id) {
        return success(orderInfoService.deleteOrder(id));
    }

    @Operation(summary = "获取订单")
    @GetMapping("/{id}")
    public BaseResponse<OrderVO> getOrder(
            @Parameter(description = "订单 ID", required = true)
            @PathVariable Long id) {
        OrderInfo order = orderInfoService.getOrderById(id);
        return success(orderInfoService.convertToVO(order));
    }

    @Operation(summary = "获取订单汇总信息")
    @GetMapping("/summary")
    public BaseResponse<OrderSummaryDTO> getOrderSummary() {
        return success(orderInfoService.getOrderSummary());
    }

    @Operation(summary = "获取订单打印时长")
    @GetMapping("/{id}/print-duration")
    public BaseResponse<Double> getOrderPrintDuration(
            @Parameter(description = "订单 ID", required = true)
            @PathVariable Long id) {
        return success(orderInfoService.getOrderPrintDuration(id));
    }

    @Operation(summary = "查询物流信息")
    @GetMapping("/logistics")
    public BaseResponse<AliyunMarketExpressQueryResponseDTO> queryLogistics(@Valid @ParameterObject OrderLogisticsQueryDTO queryDTO) {
        return success(aliyunMarketExpressService.queryExpress(queryDTO.getNumber(), queryDTO.getType(), queryDTO.getMobile()));
    }

    @Operation(summary = "分页获取订单列表")
    @GetMapping("/list/page")
    public BaseResponse<IPage<OrderVO>> listOrders(@Valid @ParameterObject OrderQueryDTO queryDTO) {
        IPage<OrderInfo> orderPage = orderInfoService.getOrderPage(queryDTO);
        IPage<OrderVO> orderVOPage = orderPage.convert(orderInfoService::convertToVO);
        return success(orderVOPage);
    }

    @Operation(summary = "获取所有订单列表（进行中）")
    @GetMapping("/list/processing")
    public BaseResponse<List<OrderVO>> getAllProcessingOrders() {
        List<OrderInfo> orders = orderInfoService.getAllOrders();
        List<OrderVO> orderVOList = orders.stream()
                .filter(order -> Objects.equals(OrderStatusEnum.PROCESSING.getValue(), order.getStatus()))
                .map(orderInfoService::convertToVO)
                .toList();
        return success(orderVOList);
    }

    @Operation(summary = "新增 / 保存草稿")
    @PostMapping("/draft/add")
    public BaseResponse<Boolean> addDraft(
            @RequestBody OrderQuoteDraftDTO draft) {
        orderDraftService.saveDraft(draft);
        return success(true);
    }

    @Operation(summary = "获取草稿列表")
    @GetMapping("/draft/list")
    public BaseResponse<List<OrderQuoteDraftDTO>> listDraft() {
        return success(orderDraftService.listDraft());
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/draft/{id}")
    public BaseResponse<Boolean> deleteDraft(
            @Parameter(description = "草稿 ID", required = true)
            @PathVariable String id) {
        orderDraftService.deleteDraft(id);
        return success(true);
    }

}
