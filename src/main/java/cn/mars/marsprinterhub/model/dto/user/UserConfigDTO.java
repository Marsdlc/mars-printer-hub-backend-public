package cn.mars.marsprinterhub.model.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户配置 DTO
 */
@Data
public class UserConfigDTO {

    /**
     * 功率（W）
     */
    private BigDecimal powerWatt;

    /**
     * 电费（元/度）
     */
    private BigDecimal electricityPricePerKwh;

    /**
     * 折旧（元/时）
     */
    private BigDecimal depreciationPerHour;

    /**
     *
     * 人工（元/时）
     */
    private BigDecimal laborCostPerHour;

    /**
     * 分页大小
     */
    @Min(value = 1, message = "分页大小最小为1")
    @Max(value = 100, message = "分页大小最大为100")
    private Long pageSize;

    /**
     * 耗材管理默认状态筛选
     */
    private String spoolDefaultStatus;

    /**
     * 打印记录默认状态筛选
     */
    private String spoolRecordDefaultStatus;

    /**
     * 订单默认状态筛选
     */
    private String orderDefaultStatus;

    /**
     * 桌面端主题
     */
    private String desktopTheme;

    /**
     * 移动端主题
     */
    private String mobileTheme;

    /**
     * 字体
     */
    private String font;

    /**
     * 桌面端布局
     */
    private String desktopLayout;

    /**
     * Web 端背景图 URL
     */
    private String webBackgroundUrl;

    /**
     * 移动端背景图 URL
     */
    private String mobileBackgroundUrl;

    /**
     * 圆角风格：sharp / rounded
     */
    private String cornerStyle;
}
