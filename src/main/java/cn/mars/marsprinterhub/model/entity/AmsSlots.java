package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * AMS 槽位绑定表
 *
 * @TableName ams_slots
 */
@TableName(value = "ams_slots")
@Data
public class AmsSlots {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * AMS ID
     */
    private Long amsId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 槽位编号（0~3）
     */
    private Integer slotNo;

    /**
     * 绑定的耗材ID
     */
    private Long spoolId;

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