package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

/**
 * 打印机风扇状态视图对象
 */
@Data
public class FanStatusVO {

    /**
     * 部件冷却风扇百分比。
     */
    private Integer coolingFanSpeedPercentage;

    /**
     * 辅助风扇百分比。
     */
    private Integer auxFanSpeedPercentage;

    /**
     * 腔体风扇百分比。
     */
    private Integer chamberFanSpeedPercentage;

    /**
     * 第二辅助风扇百分比。
     */
    private Integer secondaryAuxFanSpeedPercentage;
}
