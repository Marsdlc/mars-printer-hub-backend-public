package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_passkey_credential")
public class UserPasskeyCredential {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String credentialId;

    private String publicKeyCose;

    private String userHandle;

    private Long signatureCount;

    private String aaguid;

    private String transports;

    private String attachment;

    private String nickname;

    private Date lastUsedTime;

    @TableLogic
    private Integer isDelete;

    private Date createTime;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Date updateTime;
}
