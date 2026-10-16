package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.util.List;

/**
 * Open API 设备信息返回对象
 */
@Data
public class OpenDeviceInfoVO {

    /**
     * 打印机名称
     */
    private String name;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 打印机型号
     */
    private String model;

    /**
     * 当前打印状态
     */
    private String printStatus;

    /**
     * 当前打印进度
     */
    private Integer printProgress;

    /**
     * 当前任务封面
     */
    private String taskImage;

    /**
     * 剩余打印时间
     */
    private Integer remainingTime;

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
     * 当前层数
     */
    private Integer layerNum;

    /**
     * 总层数
     */
    private Integer totalLayerNum;

    /**
     * 耗材类型
     */
    private String trayType;

    /**
     * HMS 错误列表
     */
    private List<HmsErrorVO> hmsErrors;
}
