package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 耗材汇总信息视图对象
 */
@Data
public class SpoolSummaryVO {

    /**
     * 总耗材数量
     */
    private Long totalCount;

    /**
     * 总花费(元)
     */
    private BigDecimal totalPrice;

    /**
     * 总净重(克)
     */
    private BigDecimal totalNetWeight;

    /**
     * 总已使用重量(克)
     */
    private BigDecimal totalUsedWeight;

    /**
     * 总剩余重量(克)
     */
    private BigDecimal totalRemainingWeight;

    /**
     * 总成本(元)
     */
    private BigDecimal totalCost;

    /**
     * 总电费(元)
     */
    private BigDecimal totalElectricityCost;

    /**
     * 总折损(元)
     */
    private BigDecimal totalDepreciationCost;

    /**
     * 材料类型分布
     * key: 材料类型, value: 数量
     */
    private Map<String, Long> materialTypeDistribution;

    /**
     * 品牌分布
     * key: 品牌, value: 数量
     */
    private Map<String, Long> brandDistribution;

    /**
     * 颜色分布
     * key: 颜色, value: 数量
     */
    private Map<String, Long> colorDistribution;

    /**
     * 外观分布
     * key: 外观描述, value: 数量
     */
    private Map<String, Long> appearanceDistribution;

    /**
     * 使用状态分布
     */
    private UsageStatusDistribution usageStatusDistribution;

    /**
     * 价格区间分布
     */
    private List<PriceRangeDistribution> priceRangeDistribution;

    /**
     * 近30天每日入库统计
     */
    private List<DailyInboundStats> recent30DaysInboundStats;

    /**
     * 使用状态分布内部类
     */
    @Data
    public static class UsageStatusDistribution {
        /**
         * 未使用数量
         */
        private Long unused;

        /**
         * 使用中数量
         */
        private Long inUse;

        /**
         * 已用完数量
         */
        private Long finished;
    }

    /**
     * 价格区间分布内部类
     */
    @Data
    public static class PriceRangeDistribution {
        /**
         * 价格区间描述
         */
        private String priceRange;

        /**
         * 数量
         */
        private Long count;

        /**
         * 最小价格
         */
        private BigDecimal minPrice;

        /**
         * 最大价格
         */
        private BigDecimal maxPrice;
    }

    /**
     * 每日入库统计
     */
    @Data
    public static class DailyInboundStats {
        /**
         * 日期，格式：yyyy-MM-dd
         */
        private String date;

        /**
         * 当天入库数量
         */
        private Long count;

        /**
         * 当天入库金额
         */
        private BigDecimal amount;
    }
}
