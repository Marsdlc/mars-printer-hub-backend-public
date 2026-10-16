package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 耗材使用记录
 * spool_used_records
 */
@TableName(value = "spool_used_records")
@Data
public class SpoolUsedRecord {
    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 耗材 ID
     */
    private Long spoolId;

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
     * 任务 ID
     */
    private Long taskId;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * AMS槽位索引(0-15)
     */
    private Integer amsRef;

    /**
     * 订单 ID
     */
    private Long orderId;

//    /**
//     * 是否删除(0:未删除,1:已删除)
//     */
//    @TableLogic
//    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Date updateTime;
} 
