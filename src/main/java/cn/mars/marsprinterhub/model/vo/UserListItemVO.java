package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户列表项视图（管理员用）
 */
@Data
public class UserListItemVO implements Serializable {

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
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

    /**
     * 是否封禁
     */
    private Boolean isBanned;

    /**
     * 封禁时间
     */
    private Date banTime;

    /**
     * 封禁原因
     */
    private String banReason;
}
