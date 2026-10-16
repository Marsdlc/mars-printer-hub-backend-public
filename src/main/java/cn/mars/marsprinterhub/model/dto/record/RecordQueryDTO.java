package cn.mars.marsprinterhub.model.dto.record;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询打印记录请求DTO
 */
@Data
public class RecordQueryDTO {

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
     * 标题
     */
    private String title;

    /**
     * 耗材ID
     */
    private Long spoolId;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 任务ID
     */
    private Long orderId;

    /**
     * 是否绑定任务 0-否 1-是
     */
    private Integer isBindOrder;
}
