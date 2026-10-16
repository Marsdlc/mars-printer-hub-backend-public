package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import lombok.Data;

import java.util.Date;

/**
 * 用户表
 *
 * @TableName user
 */
@TableName(value = "user")
@Data
public class User {
    public static final String DEFAULT_BAMBU_REGION = BambuRegionEnum.CHINA.getValue();

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 角色 admin user
     */
    private String role;

    /**
     * 拓竹云访问令牌
     */
    private String bambuToken;

    /**
     * 拓竹云用户名
     */
    private String bambuUsername;

    /**
     * 拓竹云用户ID（纯数字）
     */
    private String bambuUserId;

    /**
     * 拓竹云令牌过期时间
     */
    private Date bambuTokenExpireTime;

    /**
     * 拓竹云区域（China / Other）
     */
    private String bambuRegion = DEFAULT_BAMBU_REGION;

    /**
     * VIP 过期时间
     */
    private Date vipExpireTime;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

    /**
     * 是否封禁(0:否,1:是)
     */
    private Integer isBanned;

    /**
     * 封禁原因
     */
    private String banReason;

    /**
     * 封禁时间
     */
    private Date banTime;

    /**
     * 判断是否为有效 VIP 用户
     */
    public boolean isVip() {
        return vipExpireTime != null && vipExpireTime.after(new Date());
    }

    /**
     * 是否删除(0:未删除,1:已删除)
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(updateStrategy= FieldStrategy.NEVER)
    private Date updateTime;
}
