package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class UserPasskeyCredentialVO {

    private Long id;

    private String credentialId;

    private String nickname;

    private String attachment;

    private String transports;

    private Date lastUsedTime;

    private Date createTime;
}
