package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * AMS 模块表
 *
 * @TableName ams
 */
@TableName(value = "ams")
@Data
public class Ams {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 打印机ID
     */
    private Long printerId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * AMS编号，从0开始，254为外挂
     */
    private Integer amsNo;

    /**
     * Home Assistant 温度实体ID
     */
    private String temperatureEntityId;

    /**
     * Home Assistant 湿度实体ID
     */
    private String humidityEntityId;

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
