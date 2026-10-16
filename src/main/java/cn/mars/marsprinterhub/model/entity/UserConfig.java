package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户配置表
 */
@Data
@TableName("user_config")
public class UserConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String configJson;

    private Date createTime;

    private Date updateTime;
}
