package cn.mars.marsprinterhub.model.dto.printer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询打印机请求DTO
 */
@Data
public class PrinterQueryDTO {

    /**
     * 当前页
     */
    @NotNull(message = "当前页数不能为空")
    @Min(value = 1, message = "当前页数最小为1")
    private Long current = 1L;

    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Long size = 10L;

    /**
     * 打印机名称（模糊查询）
     */
    private String name;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备型号
     */
    private String model;

    /**
     * IP地址
     */
    private String ipAddress;
}