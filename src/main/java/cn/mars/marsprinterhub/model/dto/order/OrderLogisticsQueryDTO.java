package cn.mars.marsprinterhub.model.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderLogisticsQueryDTO {

    /**
     * 快递单号
     */
    @NotBlank(message = "快递单号不能为空")
    private String number;

    /**
     * 快递公司编码
     */
    private String type;

    /**
     * 收件人手机号后四位
     */
    private String mobile;
}
