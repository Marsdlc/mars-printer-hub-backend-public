package cn.mars.marsprinterhub.model.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderAssetsUpdateDTO {

    @NotNull(message = "订单 ID 不能为空")
    private Long id;

    /**
     * 订单相关图片 URL 列表
     */
    private List<String> images;

    /**
     * 订单关联模型 URL 列表
     */
    private List<String> models;
}
