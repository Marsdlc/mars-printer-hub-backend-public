package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

@Data
public class UserApiKeyCreateVO extends UserApiKeyVO {

    /**
     * 明文 Key，仅创建时返回一次
     */
    private String apiKey;
}
