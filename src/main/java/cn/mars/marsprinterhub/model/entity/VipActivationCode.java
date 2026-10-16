package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * VIP 激活码表
 */
@TableName(value = "vip_activation_code")
@Data
public class VipActivationCode {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 激活码
     */
    private String code;

    /**
     * 激活时长（天）
     */
    private Integer durationDays;

    /**
     * 是否已使用
     */
    private Boolean isUsed;

    /**
     * 使用者用户ID
     */
    private Long usedBy;

    /**
     * 使用时间
     */
    private Date usedTime;

    /**
     * 创建时间
     */
    private Date createTime;
}
