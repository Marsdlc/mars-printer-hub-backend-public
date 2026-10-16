package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.util.List;

/**
 * AMS状态更新视图对象
 * 用于 SSE 推送 AMS 状态变化
 *
 * @author mars
 */
@Data
public class AmsStatusVO {

    /**
     * 打印机ID
     */
    private Long printerId;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * AMS信息列表
     */
    private List<AmsVO> amsList;

    /**
     * 更新时间戳
     */
    private Long timestamp;
}
