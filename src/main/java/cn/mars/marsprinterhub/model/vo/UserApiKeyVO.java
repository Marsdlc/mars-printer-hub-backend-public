package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class UserApiKeyVO {

    private Long id;

    private String name;

    private String keyPrefix;

    private Integer status;

    private Date lastUsedTime;

    private String lastUsedIp;

    private Date createTime;

    private Date updateTime;
}
