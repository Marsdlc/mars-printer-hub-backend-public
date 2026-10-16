package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 打印机视图对象
 */
@Data
public class PrinterVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

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
     * 设备访问码
     */
    private String accessCode;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 打印机位置
     */
    private String location;

    /**
     * Home Assistant 功耗实体ID
     */
    private String powerEntityId;

    /**
     * Home Assistant 仓温实体ID
     */
    private String chamberTempEntityId;

    /**
     * 自定义摄像头 RTSP 路径
     */
    private String customCameraRtspUrl;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
