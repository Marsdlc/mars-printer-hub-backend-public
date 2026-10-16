package cn.mars.marsprinterhub.model.dto.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderQuoteDraftDTO {

    private Long id;
    private Long timestamp;

    private String orderName;
    private String customerName;

    /**
     * 打印时长
     */
    private Integer printDuration;

    /**
     * 功率（W）
     */
    private Integer power;

    /**
     * 电价
     */
    private Double electricityPrice;

    /**
     * 人工时薪
     */
    private Double laborHourlyRate;

    /**
     * 耗材列表
     */
    private List<Consumable> consumables;

    /**
     * 最终报价附加项
     */
    private FinalQuote finalQuote;

    /**
     * 总收入
     */
    private Double totalIncome;

    @Data
    public static class Consumable {
        /**
         * 材料 ID
         */
        private Integer material;

        /**
         * 重量
         */
        private Integer weight;

        /**
         * 单价
         */
        private Double price;
    }

    @Data
    public static class FinalQuote {
        private Double logistics;
        private Double design;
        private Double other;
    }
}