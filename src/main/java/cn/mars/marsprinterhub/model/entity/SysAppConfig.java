package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName(value = "sys_app_config")
@Data
public class SysAppConfig {

    @TableId
    private String configKey;

    private String configValue;
}