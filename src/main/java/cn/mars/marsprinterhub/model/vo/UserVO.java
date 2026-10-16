package cn.mars.marsprinterhub.model.vo;

import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图（脱敏）
 */
@Data
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

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
     * 角色
     */
    private String role;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 拓竹云用户名
     */
    private String bambuUsername;

    /**
     * 拓竹云区域
     */
    private BambuRegionEnum bambuRegion;

    /**
     * 拓竹云令牌过期时间
     */
    private Date bambuTokenExpireTime;

    /**
     * VIP 过期时间
     */
    private Date vipExpireTime;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

    /**
     * 是否封禁
     */
    private Boolean isBanned;

    /**
     * 封禁原因
     */
    private String banReason;

    /**
     * 封禁时间
     */
    private Date banTime;

    /**
     * 是否内网
     */
    private Boolean isIntranet;

    /**
     * 是否会员
     */
    private Boolean isVip;
}
