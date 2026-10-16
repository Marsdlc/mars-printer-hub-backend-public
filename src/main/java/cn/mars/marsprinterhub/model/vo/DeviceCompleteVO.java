package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 打印机完整信息视图对象
 * 包含打印机基本信息、状态信息和AMS信息
 *
 * @author mars
 */
@Data
public class DeviceCompleteVO {

    /**
     * 打印机状态信息
     */
    private PrinterStatusVO status;

    /**
     * AMS 信息列表
     */
    private List<AmsVO> amsList;

    /**
     * mqtt 连接状态
     */
    private Integer mqttConnectStatus;

    /**
     * 功率信息
     */
    private HomeAssistantEntityStateVO power;

    /**
     * chamber 灯信息
     */
    private String chamberLight;

    /**
     * chamber2 灯信息
     */
    private String chamberLight2;

    /**
     * work 灯信息
     */
    private String workLight;

    /**
     * 风扇状态
     */
    private FanStatusVO fans;

    /**
     * 热床温度
     */
    private Integer bedTemp;

    /**
     * 仓度
     */
    private Integer chamberTemp;

    /**
     * 活动喷嘴当前温度
     */
    private Integer nozzleTemp;

    /**
     * 活动喷嘴：single/left/right
     */
    private String activeNozzle;

    /**
     * 活动喷嘴目标温度
     */
    private Integer targetNozzleTemp;

    /**
     * 左喷嘴当前温度（双喷嘴设备）
     */
    private Integer leftNozzleTemp;

    /**
     * 左喷嘴目标温度（双喷嘴设备）
     */
    private Integer leftTargetNozzleTemp;

    /**
     * 右喷嘴当前温度（双喷嘴设备）
     */
    private Integer rightNozzleTemp;

    /**
     * 右喷嘴目标温度（双喷嘴设备）
     */
    private Integer rightTargetNozzleTemp;

    /**
     * HMS 错误列表
     */
    private List<HmsErrorVO> hmsErrors;

    /**
     * 当前层数
     */
    private Integer layerNum;

    /**
     * 总层数
     */
    private Integer totalLayerNum;

    /**
     * 最后更新时间
     */
    private Long lastUpdateTime;
}
