package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HomeVO {

    /**
     * 本周使用时长
     */
    private Double weeklyUsedTime;

    /**
     * 成功打印次数
     */
    private Integer successfulPrints;

    /**
     * 总使用耗材重量
     */
    private BigDecimal totalUsedSpoolsWeight;

    /**
     * 打印机信息
     */
    private List<PrinterHomeSummaryVO> printers;

    /**
     * 最近添加的耗材(最新5个)
     */
    private List<SpoolHomeSummaryVO> recentlyAdded;

    /**
     * 即将用完的耗材(剩余重量<10%)
     */
    private List<SpoolHomeSummaryVO> lowStock;

    /**
     * 最近10条使用记录
     */
    private List<SpoolUsedRecordHomeSummaryVO> last10UsedRecords;
}
