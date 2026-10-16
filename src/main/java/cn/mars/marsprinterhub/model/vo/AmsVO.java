package cn.mars.marsprinterhub.model.vo;

import cn.mars.marsprinterhub.bambu.model.DrySetting;
import lombok.Data;

import java.util.List;

/**
 * AMS 视图对象
 *
 * @author mars
 */
@Data
public class AmsVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * AMS编号，从0开始
     */
    private Integer amsNo;

    /**
     * AMS名称
     */
    private String amsName;

    /**
     * 当前活动料盘索引
     */
    private Integer trayNow;

    /**
     * 槽位列表
     */
    private List<AmsSlotsVO> slots;

    /**
     * 湿度
     */
    private String humidity;

    /**
     * Home Assistant 湿度实体ID
     */
    private String humidityEntityId;

    /**
     * 温度
     */
    private String temperature;

    /**
     * Home Assistant 温度实体ID
     */
    private String temperatureEntityId;

    /**
     * 烘干设置
     */
    private DrySetting drySetting;

    /**
     * 烘干剩余时间（分钟）
     */
    private Integer dryTime;
}
