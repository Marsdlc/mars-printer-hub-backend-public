package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

/**
 * AMS 槽位视图对象
 *
 * @author mars
 */
@Data
public class AmsSlotsVO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * AMS ID
     */
    private Long amsId;

    /**
     * 槽位信息索引
     */
    private String trayInfoIdx;

    /**
     * 槽位编号（0~3）
     */
    private Integer slotNo;

    /**
     * 耗材类型
     */
    private String trayType;

    /**
     * 耗材颜色
     */
    private String trayColor;

    /**
     * 绑定的耗材ID
     */
    private Long spoolId;

    /**
     * 绑定的耗材信息
     */
    private SpoolVO spool;
}