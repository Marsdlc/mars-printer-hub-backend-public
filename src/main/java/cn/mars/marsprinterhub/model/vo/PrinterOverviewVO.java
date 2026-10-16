package cn.mars.marsprinterhub.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 打印机概览视图对象（精简数据）
 */
@Data
@Builder
public class PrinterOverviewVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 打印机名称
     */
    private String name;

    /**
     * 设备型号（文本，如P1S、A1等）
     */
    private String model;

    /**
     * 拓竹设备ID
     */
    private String deviceId;

    /**
     * 打印状态（RUNNING/PAUSE/IDLE/FINISH/OFFLINE等）
     */
    private String status;

    /**
     * 打印机位置
     */
    private String location;

    /**
     * 打印进度（0-100）
     */
    private Integer mcPercent;

    /**
     * 剩余打印时间（分钟）
     */
    private Integer remainingTime;

    /**
     * 打印机封面图片 URL
     */
    private String coverUrl;
}
