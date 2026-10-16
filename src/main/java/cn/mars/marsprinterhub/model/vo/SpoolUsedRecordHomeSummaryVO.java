package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SpoolUsedRecordHomeSummaryVO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 耗材ID
     */
    private Long spoolId;

    /**
     * 耗材信息
     */
    private String spoolName;

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
     * 创建时间
     */
    private Date createTime;
} 
