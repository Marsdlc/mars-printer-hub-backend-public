package cn.mars.marsprinterhub.model.dto.spool;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 查询耗材请求DTO
 */
@Data
public class SpoolQueryDTO {

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
     * 耗材 ID
     */
    private Long id;

    /**
     * 耗材名称（模糊查询）
     */
    private String name;

    /**
     * 关键字（支持按耗材 ID 或名称查询）
     */
    private String keyword;

    /**
     * 材料类型
     */
    private List<String> materialType;

    /**
     * 品牌（模糊查询）
     */
    private String brand;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;

    /**
     * 外观/质感
     */
    private List<Integer> appearance;

    /**
     * 状态 0:未使用 1:使用中 2:消耗完
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
