package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

@Data
public class DeviceSimpleVO {
    /**
     * ID
     */
    private Long id;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 排序
     */
    private Integer sort;
}
