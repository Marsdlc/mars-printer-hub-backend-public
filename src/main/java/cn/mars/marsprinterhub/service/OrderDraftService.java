package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.order.OrderQuoteDraftDTO;

import java.util.List;

public interface OrderDraftService {

    /**
     * 保存订单草稿
     *
     * @param draft 订单草稿
     */
    void saveDraft(OrderQuoteDraftDTO draft);

    /**
     * 获取所有订单草稿
     *
     * @return 订单草稿列表
     */
    List<OrderQuoteDraftDTO> listDraft();

    /**
     * 删除订单草稿
     *
     * @param id 订单草稿 ID
     */
    void deleteDraft(String id);
}
