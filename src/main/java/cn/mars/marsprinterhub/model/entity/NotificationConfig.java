package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知配置实体
 *
 * @author mars
 */
@Data
@TableName("sys_notification_config")
public class NotificationConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 渠道标识
     */
    private String channel;

    /**
     * 渠道名称
     */
    private String name;

    /**
     * 配置内容(JSON字符串)
     */
    private String config;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
