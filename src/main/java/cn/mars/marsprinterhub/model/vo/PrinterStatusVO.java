package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

/**
 * 打印机状态视图对象
 */
@Data
public class PrinterStatusVO {

    /**
     * 打印机名称
     */
    private String name;

    /**
     * 拓竹设备ID（dev_id）
     */
    private String deviceId;

    /**
     * 设备型号（如P1P、P1S、A1等）
     */
    private String model;

    /**
     * 打印状态
     */
    private String printStatus;

    /**
     * 打印进度（0-100）
     */
    private Integer printProgress;

    /**
     * 当前打印任务图片
     */
    private String taskImage;

    /**
     * 剩余打印时间（分钟）
     */
    private Integer remainingTime;
}