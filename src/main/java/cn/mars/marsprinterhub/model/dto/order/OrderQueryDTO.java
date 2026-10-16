package cn.mars.marsprinterhub.model.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderQueryDTO {

    /**
     * 当前页
     */
    @NotNull
    @Min(1)
    private Long current = 1L;

    /**
     * 每页大小
     */
    @NotNull
    @Min(1)
    @Max(100)
    private Long size = 10L;

    /**
     * 订单名称
     */
    private String orderName;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 客户来源
     */
    private String customerSource;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向：asc(升序) 或 desc(降序)
     */
    private String sortOrder;
}