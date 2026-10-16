package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SpoolUsedRecordVO {
    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 耗材 ID
     */
    private Long spoolId;

    /**
     * 耗材信息
     */
    private SpoolVO spoolVO;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务图片
     */
    private String coverUrl;

    /**
     * 任务状态
     */
    private Integer status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 打印进度
     */
    private Integer printProgress;

    /**
     * 任务重量(克)
     */
    private BigDecimal taskWeight;

    /**
     * 实际使用重量(克)
     */
    private BigDecimal actualWeight;

    /**
     * 记录成本(元)
     */
    private BigDecimal cost;

    /**
     * 电费(元)
     */
    private BigDecimal electricityCost;

    /**
     * 折旧费(元)
     */
    private BigDecimal depreciationCost;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 创建时间
     */
    private Date createTime;
} 
