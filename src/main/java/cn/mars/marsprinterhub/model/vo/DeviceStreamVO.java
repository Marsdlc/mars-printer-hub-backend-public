package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

/**
 * 统一流信息
 */
@Data
public class DeviceStreamVO {

    /**
     * 流类型：hls / mjpeg
     */
    private String streamType;

    /**
     * 播放地址
     */
    private String url;

    /**
     * 建议帧率（截图流场景）
     */
    private Double fps;

    /**
     * 时延提示：low / medium
     */
    private String latencyHint;
}
