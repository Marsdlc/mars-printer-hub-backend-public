package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户 API Key 表
 */
@Data
@TableName("user_api_key")
public class UserApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String keyPrefix;

    private String secretHash;

    /**
     * 0:禁用 1:启用
     */
    private Integer status;

    private Date lastUsedTime;

    private String lastUsedIp;

    private Long createUserId;

    private Date createTime;

    private Date updateTime;
}
